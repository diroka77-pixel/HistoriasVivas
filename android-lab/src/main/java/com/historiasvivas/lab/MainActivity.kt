package com.historiasvivas.lab

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine
import com.arm.aichat.InferenceEngine.State
import com.arm.aichat.isModelLoaded
import com.historiasvivas.engine.ai.local.LocalNarrativeAI
import com.historiasvivas.engine.ai.local.LocalNarrativePromptBuilder
import com.historiasvivas.engine.manuscript.*
import com.historiasvivas.engine.scenario.RuinsScenario
import com.historiasvivas.engine.session.StorySession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var status: TextView
    private lateinit var storyText: TextView
    private lateinit var metrics: TextView
    private lateinit var storyScroll: ScrollView
    private lateinit var action: EditText
    private lateinit var send: Button
    private lateinit var selectModelButton: Button
    private lateinit var engine: InferenceEngine
    private var session: StorySession? = null
    private var manuscript: StoryManuscript? = null
    private val manuscriptPersistence = ManuscriptPersistence()

    private val selectModel =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) lifecycleScope.launch { prepareModel(uri) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        applySafeAreaInsets()

        status = findViewById(R.id.modelStatus)
        storyText = findViewById(R.id.storyText)
        metrics = findViewById(R.id.metrics)
        storyScroll = findViewById(R.id.storyScroll)
        action = findViewById(R.id.actionText)
        send = findViewById(R.id.sendAction)
        selectModelButton = findViewById(R.id.selectModel)

        selectModelButton.setOnClickListener {
            selectModel.launch(arrayOf("application/octet-stream", "*/*"))
        }
        send.setOnClickListener { playTurn() }

        lifecycleScope.launch {
            try {
                engine = withContext(Dispatchers.Default) {
                    AiChat.getInferenceEngine(applicationContext)
                }
                val savedModel = File(filesDir, "models/narrator.gguf")
                if (savedModel.exists() && savedModel.length() > 0L) {
                    activateModel(savedModel)
                }
            } catch (t: Throwable) {
                showLoadError(t)
            }
        }
    }

    private fun applySafeAreaInsets() {
        window.setDecorFitsSystemWindows(false)
        val root = findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
        val left = root.paddingLeft
        val top = root.paddingTop
        val right = root.paddingRight
        val bottom = root.paddingBottom
        root.setOnApplyWindowInsetsListener { view, insets ->
            val safe = insets.getInsets(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
            )
            val keyboard = insets.getInsets(WindowInsets.Type.ime())
            view.setPadding(
                left + safe.left,
                top + safe.top,
                right + safe.right,
                bottom + maxOf(safe.bottom, keyboard.bottom)
            )
            insets
        }
        root.requestApplyInsets()
    }

    private fun selectedFileSize(uri: Uri): Long =
        contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else -1L
        } ?: -1L

    private fun gib(bytes: Long) =
        String.format(Locale.getDefault(), "%.1f", bytes / 1073741824.0)

    private suspend fun waitForEngineReference() {
        while (!::engine.isInitialized) delay(50)
    }

    private suspend fun waitForEngineReady() {
        waitForEngineReference()
        val state = engine.state.first {
            it is State.Initialized || it.isModelLoaded || it is State.Error
        }
        if (state is State.Error) throw state.exception
    }

    private suspend fun unloadCurrentModel() = withContext(Dispatchers.Default) {
        waitForEngineReady()
        if (engine.state.value.isModelLoaded) {
            engine.cleanUp()
            val state = engine.state.first {
                it is State.Initialized || it is State.Error
            }
            if (state is State.Error) throw state.exception
        }
    }

    private suspend fun prepareModel(uri: Uri) = withContext(Dispatchers.IO) {
        val modelDir = File(filesDir, "models").apply { mkdirs() }
        val file = File(modelDir, "narrator.gguf")
        val partial = File(modelDir, "narrator.gguf.part")
        var phase = "copiar"

        withContext(Dispatchers.Main) {
            status.text = "Preparando la copia del modelo…"
            send.isEnabled = false
            selectModelButton.isEnabled = false
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        try {
            val size = selectedFileSize(uri)
            val reserve = 512L * 1024L * 1024L
            val needed = if (size > 0) size + reserve else 4L * 1024L * 1024L * 1024L
            val available = modelDir.usableSpace
            if (available < needed) {
                throw IOException(
                    "Espacio insuficiente. Hay " + gib(available) +
                        " GB libres y hacen falta al menos " + gib(needed) + " GB."
                )
            }

            if (partial.exists()) partial.delete()
            val input = contentResolver.openInputStream(uri)
                ?: throw IOException("Android no ha permitido abrir el archivo seleccionado.")
            input.use { source ->
                FileOutputStream(partial).use { target ->
                    val buffer = ByteArray(1024 * 1024)
                    var copied = 0L
                    var lastShown = -1
                    while (true) {
                        val count = source.read(buffer)
                        if (count < 0) break
                        target.write(buffer, 0, count)
                        copied += count
                        if (size > 0) {
                            val percent = ((copied * 100L) / size).toInt().coerceIn(0, 100)
                            if (percent >= lastShown + 2) {
                                lastShown = percent
                                withContext(Dispatchers.Main) {
                                    status.text = "Copiando modelo local… $percent %"
                                }
                            }
                        }
                    }
                    target.fd.sync()
                }
            }

            if (size > 0 && partial.length() != size) {
                throw IOException(
                    "La copia quedó incompleta (" + gib(partial.length()) +
                        " de " + gib(size) + " GB)."
                )
            }

            phase = "cargar"
            unloadCurrentModel()
            try {
                Files.move(
                    partial.toPath(),
                    file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                )
            } catch (_: Throwable) {
                Files.move(
                    partial.toPath(),
                    file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
            activateModel(file)
        } catch (t: Throwable) {
            partial.delete()
            withContext(Dispatchers.Main) {
                if (phase == "copiar") {
                    status.text = "No se pudo copiar el modelo"
                    storyText.text = (t.message ?: "error desconocido") +
                        "\n\nComprueba el espacio libre y vuelve a seleccionar el archivo GGUF."
                    metrics.text = "La aplicación sigue abierta; puedes intentarlo otra vez."
                } else {
                    showLoadError(t)
                }
            }
        } finally {
            withContext(Dispatchers.Main) {
                selectModelButton.isEnabled = true
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    private suspend fun activateModel(file: File) = withContext(Dispatchers.Default) {
        withContext(Dispatchers.Main) {
            status.text = "Cargando narrador guardado… Puede tardar varios minutos."
            send.isEnabled = false
            selectModelButton.isEnabled = false
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        try {
            waitForEngineReady()
            if (!engine.state.value.isModelLoaded) {
                engine.loadModel(file.path)
                engine.setSystemPrompt(LocalNarrativePromptBuilder().systemPrompt())
            }

            val ai = LocalNarrativeAI(LlamaCppNarrativeRuntime(engine))
            val saveFile = File(filesDir, "nadir-story.json")
            session = if (saveFile.exists()) {
                StorySession.resume(saveFile.readText(), ai)
            } else {
                StorySession(RuinsScenario.create(), ai)
            }

            val manuscriptFile = File(filesDir, "nadir-manuscript.json")
            manuscript = if (manuscriptFile.exists()) {
                manuscriptPersistence.deserialize(manuscriptFile.readText())
            } else {
                StoryManuscript(session!!.story.id, session!!.story.title)
            }

            withContext(Dispatchers.Main) {
                status.text = "Narrador local preparado · modelo guardado"
                selectModelButton.text = "Cambiar modelo GGUF"
                metrics.text = if (manuscript!!.entries.isEmpty()) {
                    "Modelo cargado correctamente"
                } else {
                    "Historia recuperada · turno " + session!!.story.turn
                }
                storyText.text = if (manuscript!!.entries.isEmpty()) {
                    "Nadir despierta bajo un cielo desconocido. ¿Qué hace o dice Daniel?"
                } else {
                    manuscript!!.plainText()
                }
                send.isEnabled = true
                scrollToBottom()
            }
        } finally {
            withContext(Dispatchers.Main) {
                selectModelButton.isEnabled = true
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    private fun showLoadError(t: Throwable) {
        status.text = "No se pudo cargar el narrador"
        storyText.text = (t.message?.takeIf { it.isNotBlank() } ?: "error desconocido") +
            "\n\nEl modelo guardado no se ha borrado. Puedes intentarlo otra vez o cambiarlo."
        metrics.text = "La aplicación sigue abierta."
        selectModelButton.isEnabled = true
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun scrollToBottom() {
        storyScroll.post { storyScroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun playTurn() {
        val current = session ?: return
        val userAction = action.text.toString().trim()
        if (userAction.isEmpty()) return

        action.text.clear()
        send.isEnabled = false
        val started = System.currentTimeMillis()

        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val turn = current.act(userAction)
                File(filesDir, "nadir-story.json").writeText(current.save())
                manuscript = manuscript!!.append(
                    ManuscriptEntry(
                        turn.story.turn,
                        userAction,
                        turn.prose,
                        System.currentTimeMillis()
                    )
                )
                File(filesDir, "nadir-manuscript.json").writeText(
                    manuscriptPersistence.serialize(manuscript!!)
                )
                File(filesDir, "nadir-manuscript.txt").writeText(manuscript!!.plainText())

                withContext(Dispatchers.Main) {
                    storyText.text = manuscript!!.plainText()
                    metrics.text = "Turno " + turn.story.turn +
                        " · historia guardada · " +
                        (System.currentTimeMillis() - started) + " ms"
                    scrollToBottom()
                }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    metrics.text = "Error local: " + (t.message ?: "desconocido")
                }
            } finally {
                withContext(Dispatchers.Main) { send.isEnabled = true }
            }
        }
    }
}
