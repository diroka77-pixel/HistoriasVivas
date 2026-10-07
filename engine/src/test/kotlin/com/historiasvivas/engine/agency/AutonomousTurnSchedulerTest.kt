package com.historiasvivas.engine.agency
import com.historiasvivas.engine.character.*
import com.historiasvivas.engine.discovery.*
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.spatial.*
import kotlin.test.*

class AutonomousTurnSchedulerTest {
 private fun story(turn:Long=12):Story{
  val erik=Character("erik","Erik",mind=CharacterMind(personality=PersonalityProfile(mapOf("courage" to 60))))
  val e1=Evidence("e1","ruins-a","Repeated pulse",80,9,"erik")
  val e2=Evidence("e2","ruins-b","Fresh machine marks",80,10,"erik")
  val interpretation=Interpretation("i","erik","ruins-a","The ruins may contain an active source",80,setOf("e1","e2"))
  val discovery=DiscoveryState(evidence=mapOf("e1" to e1,"e2" to e2),interpretations=mapOf("i" to interpretation))
  val spatial=SpatialState(positions=mapOf("erik" to CharacterPosition("erik","settlement")),routes=listOf(Route("settlement","ruins-a+ruins-b",3)))
  return Story("s","Nadir","p",world=WorldState(
   characters=mapOf("erik" to erik),locations=mapOf("settlement" to Location("settlement","Settlement"),"ruins-a+ruins-b" to Location("ruins-a+ruins-b","Ruins")),
   discovery=discovery,spatial=spatial
  ),turn=turn,protagonistId="erik")
 }

 @Test fun presentNpcDoesNotRunPrivateAutonomyCycle(){
  val s=story()
  val (updated,cycle)=AutonomousTurnScheduler().run(s,setOf("erik"))
  assertTrue(cycle.plans.isEmpty())
  assertEquals(s.world.spatial,updated.world.spatial)
 }

 @Test fun eligibleNpcCanStartPhysicalActionAndItStaysHidden(){
  val s=story()
  val (updated,cycle)=AutonomousTurnScheduler().run(s,emptySet())
  assertEquals(1,cycle.plans.size)
  assertEquals(ActionExecutionStatus.STARTED,cycle.results.single().status)
  assertTrue("erik" in updated.world.spatial.journeys)
  assertTrue(updated.world.hiddenEvents.any{it.actorId=="erik"})
  assertTrue(updated.world.autonomousAgency.plans.isNotEmpty())
  assertTrue(updated.world.reasoning.hypotheses.isNotEmpty())
 }

 @Test fun recentPlanPreventsImmediateRepeatedAutonomy(){
  val s=story()
  val (once,_)=AutonomousTurnScheduler().run(s,emptySet())
  val next=once.copy(turn=once.turn+1)
  val (_,cycle)=AutonomousTurnScheduler().run(next,emptySet())
  assertTrue(cycle.plans.isEmpty())
 }
}
