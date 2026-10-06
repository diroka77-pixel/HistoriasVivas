package com.historiasvivas.lab

import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MainActivity:AppCompatActivity(){
 private lateinit var status:TextView
 private lateinit var story:TextView
 private lateinit var metrics:TextView
 private lateinit var action:EditText
 private lateinit var send:Button
 private lateinit var engine:InferenceEngine
 private var ready=false

 private val selectModel=registerForActivityResult(ActivityResultContracts.OpenDocument()){uri->
  if(uri!=null) lifecycleScope.launch{prepareModel(uri)}
 }

 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState);setContentView(R.layout.activity_main)
  status=findViewById(R.id.modelStatus);story=findViewById(R.id.storyText)
  metrics=findViewById(R.id.metrics);action=findViewById(R.id.actionText);send=findViewById(R.id.sendAction)
  lifecycleScope.launch(Dispatchers.Default){engine=AiChat.getInferenceEngine(applicationContext)}
  findViewById<Button>(R.id.selectModel).setOnClickListener{selectModel.launch(arrayOf("*/*"))}
  send.setOnClickListener{generate()}
 }

 private suspend fun prepareModel(uri:Uri)=withContext(Dispatchers.IO){
  withContext(Dispatchers.Main){status.text="Copiando modelo local…";send.isEnabled=false}
  val dir=File(filesDir,"models").apply{mkdirs()}
  val file=File(dir,"narrator.gguf")
  contentResolver.openInputStream(uri)?.use{input->FileOutputStream(file).use{out->input.copyTo(out)}} ?: return@withContext
  val started=System.currentTimeMillis()
  withContext(Dispatchers.Main){status.text="Cargando Qwen local…"}
  engine.loadModel(file.path)
  val elapsed=System.currentTimeMillis()-started
  withContext(Dispatchers.Main){
   ready=true;status.text="Modelo local preparado · sin conexión"
   metrics.text="Carga: "+elapsed+" ms";send.isEnabled=true
   story.text="Las ruinas de Nadir esperan. Escribe libremente qué hace o dice Daniel."
  }
 }

 private fun generate(){
  if(!ready)return
  val userAction=action.text.toString().trim();if(userAction.isEmpty())return
  action.text.clear();send.isEnabled=false
  val prompt="Continúa una novela interactiva en español. No decidas la siguiente acción del protagonista. Acción de Daniel: "+userAction
  val started=System.currentTimeMillis();val output=StringBuilder()
  lifecycleScope.launch(Dispatchers.Default){
   engine.sendUserPrompt(prompt,700).onCompletion{
    val elapsed=System.currentTimeMillis()-started
    withContext(Dispatchers.Main){metrics.text="Generación: "+elapsed+" ms";send.isEnabled=true}
   }.collect{token->
    output.append(token)
    withContext(Dispatchers.Main){story.text=output.toString()}
   }
  }
 }
 override fun onDestroy(){if(::engine.isInitialized)engine.destroy();super.onDestroy()}
}
