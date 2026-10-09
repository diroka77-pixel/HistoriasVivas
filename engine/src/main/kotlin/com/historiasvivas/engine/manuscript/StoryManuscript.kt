package com.historiasvivas.engine.manuscript
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class ManuscriptEntry(
 val turn:Long,
 val userAction:String,
 val prose:String,
 val createdAtEpochMs:Long
)
@Serializable
data class StoryManuscript(
 val storyId:String,
 val title:String,
 val entries:List<ManuscriptEntry> = emptyList()
){
 fun append(entry:ManuscriptEntry)=copy(entries=entries+entry)
 fun plainText():String=buildString{
  appendLine(title);appendLine()
  entries.forEach{e->
   appendLine("— Turno "+e.turn+" —")
   appendLine("Tú: "+e.userAction.trim());appendLine()
   appendLine(e.prose.trim());appendLine()
  }
 }
}
class ManuscriptPersistence {
 private val json=Json{prettyPrint=true;ignoreUnknownKeys=true}
 fun serialize(value:StoryManuscript)=json.encodeToString(value)
 fun deserialize(data:String)=json.decodeFromString<StoryManuscript>(data)
}
