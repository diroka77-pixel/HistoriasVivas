package com.historiasvivas.lab
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine
import com.historiasvivas.engine.ai.local.LocalNarrativeAI
import com.historiasvivas.engine.scenario.RuinsScenario
import com.historiasvivas.engine.session.StorySession
import com.historiasvivas.engine.manuscript.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MainActivity:AppCompatActivity(){
 private lateinit var status:TextView; private lateinit var storyText:TextView; private lateinit var metrics:TextView
 private lateinit var action:EditText; private lateinit var send:Button; private lateinit var engine:InferenceEngine
 private var session:StorySession?=null
 private var manuscript:StoryManuscript?=null
 private val manuscriptPersistence=ManuscriptPersistence()
 private val selectModel=registerForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)lifecycleScope.launch{prepareModel(uri)}}
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContentView(R.layout.activity_main)
  applySafeAreaInsets()
  status=findViewById(R.id.modelStatus);storyText=findViewById(R.id.storyText);metrics=findViewById(R.id.metrics);action=findViewById(R.id.actionText);send=findViewById(R.id.sendAction)
  lifecycleScope.launch(Dispatchers.Default){engine=AiChat.getInferenceEngine(applicationContext)}
  findViewById<Button>(R.id.selectModel).setOnClickListener{selectModel.launch(arrayOf("*/*"))};send.setOnClickListener{playTurn()}
 }
 private fun applySafeAreaInsets(){
  // Keep the XML spacing, then reserve the actual system bars and keyboard.
  // Capture once so repeated inset dispatches do not accumulate padding.
  window.setDecorFitsSystemWindows(false)
  val root=findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
  val left=root.paddingLeft; val top=root.paddingTop
  val right=root.paddingRight; val bottom=root.paddingBottom
  root.setOnApplyWindowInsetsListener { view,insets ->
   val safe=insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
   val keyboard=insets.getInsets(WindowInsets.Type.ime())
   view.setPadding(left+safe.left,top+safe.top,right+safe.right,bottom+maxOf(safe.bottom,keyboard.bottom))
   insets
  }
  root.requestApplyInsets()
 }
 private suspend fun prepareModel(uri:Uri)=withContext(Dispatchers.IO){
  withContext(Dispatchers.Main){status.text="Copiando modelo local…";send.isEnabled=false}
  val file=File(File(filesDir,"models").apply{mkdirs()},"narrator.gguf")
  contentResolver.openInputStream(uri)?.use{input->FileOutputStream(file).use{input.copyTo(it)}}?:return@withContext
  val started=System.currentTimeMillis();withContext(Dispatchers.Main){status.text="Cargando narrador local…"};engine.loadModel(file.path)
  val ai=LocalNarrativeAI(LlamaCppNarrativeRuntime(engine))
  val saveFile=File(filesDir,"nadir-story.json")
  session=if(saveFile.exists()) StorySession.resume(saveFile.readText(),ai) else StorySession(RuinsScenario.create(),ai)
  val manuscriptFile=File(filesDir,"nadir-manuscript.json")
  manuscript=if(manuscriptFile.exists()) manuscriptPersistence.deserialize(manuscriptFile.readText()) else StoryManuscript(session!!.story.id,session!!.story.title)
  withContext(Dispatchers.Main){status.text="Narrador local preparado · sin conexión";metrics.text="Carga: "+(System.currentTimeMillis()-started)+" ms";storyText.text=if((session?.story?.turn?:0L)>0) "Nadir reanudado · turno "+session!!.story.turn+". ¿Qué hace o dice Daniel?" else "Nadir despierta bajo un cielo desconocido. ¿Qué hace o dice Daniel?";send.isEnabled=true}
 }
 private fun playTurn(){val current=session?:return;val userAction=action.text.toString().trim();if(userAction.isEmpty())return
  action.text.clear();send.isEnabled=false;val started=System.currentTimeMillis()
  lifecycleScope.launch(Dispatchers.Default){
   try{
    val turn=current.act(userAction)
    File(filesDir,"nadir-story.json").writeText(current.save())
    manuscript=manuscript!!.append(ManuscriptEntry(turn.story.turn,userAction,turn.prose,System.currentTimeMillis()))
    File(filesDir,"nadir-manuscript.json").writeText(manuscriptPersistence.serialize(manuscript!!))
    File(filesDir,"nadir-manuscript.txt").writeText(manuscript!!.plainText())
    withContext(Dispatchers.Main){storyText.text=turn.prose;metrics.text="Turno "+turn.story.turn+" · mundo + manuscrito guardados · "+(System.currentTimeMillis()-started)+" ms"}
   }
   catch(t:Throwable){withContext(Dispatchers.Main){storyText.text="Error local: "+(t.message?:"desconocido")}}
   finally{withContext(Dispatchers.Main){send.isEnabled=true}}
  }
 }
 override fun onDestroy(){if(::engine.isInitialized)engine.destroy();super.onDestroy()}
}
