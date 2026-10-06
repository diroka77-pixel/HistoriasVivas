package com.historiasvivas.engine.session
import com.historiasvivas.engine.ai.NarrativeAI
import com.historiasvivas.engine.model.Story
import com.historiasvivas.engine.persistence.StoryPersistence
import com.historiasvivas.engine.turn.*

data class SessionTurn(val prose:String,val story:Story)

class StorySession(
 initialStory:Story,
 ai:NarrativeAI,
 private val persistence:StoryPersistence=StoryPersistence()
){
 private val engine=TurnEngine(ai)
 var story:Story=initialStory
  private set

 suspend fun act(freeText:String):SessionTurn{
  val result=engine.play(story,freeText)
  story=result.story
  return SessionTurn(result.prose,story)
 }
 fun save():String=persistence.serialize(story)
 companion object{
  fun resume(data:String,ai:NarrativeAI,persistence:StoryPersistence=StoryPersistence())=
   StorySession(persistence.deserialize(data),ai,persistence)
 }
}
