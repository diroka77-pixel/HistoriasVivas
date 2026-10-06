package com.historiasvivas.lab
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity:AppCompatActivity(){
 private lateinit var status:TextView
 private lateinit var send:Button
 private var modelUri:Uri?=null
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContentView(R.layout.activity_main)
  status=findViewById(R.id.modelStatus);send=findViewById(R.id.sendAction)
  findViewById<Button>(R.id.selectModel).setOnClickListener{ startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="application/octet-stream"},100) }
  send.setOnClickListener{ Toast.makeText(this,"El puente nativo de inferencia será la siguiente pieza.",Toast.LENGTH_SHORT).show() }
 }
 @Deprecated("Deprecated in Android")
 override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data)
  if(requestCode==100&&resultCode==Activity.RESULT_OK){modelUri=data?.data; modelUri?.let{contentResolver.takePersistableUriPermission(it,Intent.FLAG_GRANT_READ_URI_PERMISSION);status.text="Modelo local seleccionado";send.isEnabled=true}}
 }
}
