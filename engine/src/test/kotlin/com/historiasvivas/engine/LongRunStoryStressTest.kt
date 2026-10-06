package com.historiasvivas.engine
import com.historiasvivas.engine.ai.*
import com.historiasvivas.engine.scenario.RuinsScenario
import com.historiasvivas.engine.session.StorySession
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class LongRunStoryStressTest {
 @Test fun oneHundredTurnsPreserveWorldIdentityAndBoundRecentMemory()=runBlocking{
  val ai=object:NarrativeAI{
   override suspend fun continueStory(request:NarrativeRequest)=NarrativeResponse(
    prose="The scene continues.",
    eventSummary="Turn "+(request.story.turn+1)+": "+request.userAction,
    participantIds=setOf("daniel"),
    canonical=(request.story.turn+1)%25L==0L
   )
  }
  val session=StorySession(RuinsScenario.create(),ai)
  repeat(100){index->session.act("Observo el mundo y actúo con calma "+(index+1)+".")}
  val story=session.story
  assertEquals(100,story.turn)
  assertEquals(100,story.world.worldClock)
  assertEquals(setOf("daniel","elena","erik"),story.world.characters.keys)
  assertEquals(2,story.world.secrets.size)
  assertTrue(story.memory.recentEvents.size<=12)
  assertEquals(4,story.memory.canonicalEvents.size)
  assertTrue(story.world.facts.contains("The Northern Ruins predate human arrival"))
 }
 @Test fun saveResumeAtMidpointStillReachesSameStructuralMilestone()=runBlocking{
  val ai=object:NarrativeAI{
   override suspend fun continueStory(request:NarrativeRequest)=NarrativeResponse(
    prose="The world waits and responds.",eventSummary=request.userAction,participantIds=setOf("daniel")
   )
  }
  var session=StorySession(RuinsScenario.create(),ai)
  repeat(50){session.act("Continúo explorando.")}
  session=StorySession.resume(session.save(),ai)
  repeat(50){session.act("Continúo explorando.")}
  assertEquals(100,session.story.turn)
  assertEquals(100,session.story.world.worldClock)
  assertEquals("Elena",session.story.world.characters["elena"]?.name)
  assertFalse(session.story.world.secrets.first{it.id=="council"}.revealed)
 }
}
