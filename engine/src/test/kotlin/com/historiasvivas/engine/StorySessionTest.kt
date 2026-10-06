package com.historiasvivas.engine
import com.historiasvivas.engine.ai.*
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.session.*
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class StorySessionTest {
 private val ai=object:NarrativeAI{
  override suspend fun continueStory(request:NarrativeRequest)=NarrativeResponse(
   prose="El mundo responde sin decidir por ti.",
   eventSummary=request.userAction,
   participantIds=setOf("daniel"),
   canonical=request.userAction.contains("abandono",true),
   proposedConsequences=if(request.userAction.contains("abandono",true))
    listOf(Consequence("erik-return","turn-"+(request.story.turn+1),"Erik may return because Daniel abandoned him",request.story.turn+20))
   else emptyList()
  )
 }
 @Test fun saveAndResumePreservesCausalState()=runBlocking{
  val seed=StorySeed("story-1","Las ruinas","Humanity survives on an unknown planet.","daniel","Daniel")
  val session=StorySession(StoryFactory().create(seed),ai)
  session.act("Abandono a Erik y regreso al asentamiento.")
  val saved=session.save()
  val resumed=StorySession.resume(saved,ai)
  assertEquals(1,resumed.story.turn)
  assertTrue(resumed.story.memory.canonicalEvents.any{it.summary.contains("Erik")})
  assertTrue(resumed.story.world.pendingConsequences.any{it.id=="erik-return"})
  val next=resumed.act("No digo a nadie lo que ocurrió.")
  assertEquals(2,next.story.turn)
  assertTrue(next.story.world.pendingConsequences.any{it.id=="erik-return"})
 }
}
