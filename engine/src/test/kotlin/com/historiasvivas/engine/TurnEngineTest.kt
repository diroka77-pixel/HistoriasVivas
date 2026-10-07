package com.historiasvivas.engine
import com.historiasvivas.engine.ai.*
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.turn.TurnEngine
import com.historiasvivas.engine.character.*
import com.historiasvivas.engine.discovery.*
import com.historiasvivas.engine.spatial.*
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class TurnEngineTest {
 @Test fun actionCreatesPersistentEventAndDelayedConsequence()= runBlocking {
  val ai=object:NarrativeAI{
   override suspend fun continueStory(request:NarrativeRequest)=NarrativeResponse(
    prose="Erik observa en silencio mientras te alejas entre los árboles.",
    eventSummary="El protagonista abandona a Erik herido.",
    participantIds=setOf("erik"),canonical=true,
    proposedFacts=setOf("Erik fue abandonado herido"),
    proposedConsequences=listOf(Consequence("erik-return","turn-1","Erik recordará el abandono",10))
   )
  }
  val start=Story("s1","Bosque","Dos viajeros perdidos.",world=WorldState(characters=mapOf("erik" to Character("erik","Erik"))))
  val result=TurnEngine(ai).play(start,"Dejo a Erik y continúo solo.")
  assertEquals(1,result.story.turn)
  assertTrue("Erik fue abandonado herido" in result.story.world.facts)
  assertTrue(result.story.memory.canonicalEvents.any{it.id=="turn-1"})
  assertTrue(result.story.world.pendingConsequences.any{it.id=="erik-return"&&it.earliestTurn==10L})
 }
 @Test fun mentionedRemoteNpcIsNotTreatedAsPhysicallyPresent()= runBlocking {
  val ai=object:NarrativeAI{
   override suspend fun continueStory(request:NarrativeRequest)=NarrativeResponse(
    prose="Daniel piensa en Erik, que sigue lejos entre las ruinas.",
    eventSummary="Daniel recuerda a Erik.",
    participantIds=setOf("erik")
   )
  }
  val daniel=Character("daniel","Daniel")
  val erik=Character("erik","Erik",mind=CharacterMind(personality=PersonalityProfile(mapOf("courage" to 60))))
  val evidence=mapOf(
   "e1" to Evidence("e1","ruins-a","Repeated pulse",80,1,"erik"),
   "e2" to Evidence("e2","ruins-b","Fresh machine marks",80,1,"erik")
  )
  val interpretation=Interpretation("i","erik","ruins-a","The ruins may contain an active source",80,setOf("e1","e2"))
  val spatial=SpatialState(
   positions=mapOf("daniel" to CharacterPosition("daniel","settlement"),"erik" to CharacterPosition("erik","ruins")),
   routes=listOf(Route("ruins","ruins-a+ruins-b",3))
  )
  val start=Story("s2","Nadir","p",world=WorldState(
   characters=mapOf("daniel" to daniel,"erik" to erik),
   locations=mapOf("settlement" to Location("settlement","Settlement"),"ruins" to Location("ruins","Ruins"),"ruins-a+ruins-b" to Location("ruins-a+ruins-b","Deep ruins")),
   discovery=DiscoveryState(evidence=evidence,interpretations=mapOf("i" to interpretation)),
   spatial=spatial
  ),turn=12,protagonistId="daniel")
  val result=TurnEngine(ai).play(start,"Pienso en Erik.")
  assertTrue(result.story.world.autonomousAgency.plans.values.any{it.characterId=="erik"})
 }

 @Test fun latentEmergentEventStillStartsDirectorCooldown()= runBlocking {
  val ai=object:NarrativeAI{
   override suspend fun continueStory(request:NarrativeRequest)=NarrativeResponse(
    prose="Daniel descansa un momento.",eventSummary="Daniel toma aire."
   )
  }
  val signals=mapOf(
   "a" to WorldSignal("a","ruins",sense=SignalSense.RADIO,surface="Repeating pulse",createdTurn=1,salience=80),
   "b" to WorldSignal("b","ruins",sense=SignalSense.TRACE,surface="Fresh marks",createdTurn=2,salience=75)
  )
  val start=Story("s3","Nadir","p",world=WorldState(
   characters=mapOf("daniel" to Character("daniel","Daniel")),
   discovery=DiscoveryState(signals=signals)
  ),turn=5,protagonistId="daniel",director=DirectorState(tension=60))
  val result=TurnEngine(ai).play(start,"Me siento y descanso.")
  assertNotNull(result.story.director.lastEmergentTurn)
  assertTrue(result.story.director.latentEmergentEventIds.isNotEmpty())
 }

}
