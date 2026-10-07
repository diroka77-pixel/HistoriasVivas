package com.historiasvivas.engine.agency
import com.historiasvivas.engine.character.*
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.reasoning.*
import com.historiasvivas.engine.spatial.*
import kotlin.test.*
class AutonomousActionExecutorTest {
 @Test fun npcActsOnPrivateHypothesisButMustTravelThroughWorld(){
  val erik=Character("erik","Erik",mind=CharacterMind(personality=PersonalityProfile(mapOf("courage" to 60))))
  val spatial=SpatialState(positions=mapOf("erik" to CharacterPosition("erik","settlement")),routes=listOf(Route("settlement","ruins",3)))
  val story=Story("s","Nadir","p",world=WorldState(characters=mapOf("erik" to erik),locations=mapOf("settlement" to Location("settlement","Settlement"),"ruins" to Location("ruins","Ruins")),spatial=spatial),turn=12)
  val h=Hypothesis("h","erik","ruins","The ruins contain an active source",80,setOf("e1","e2"),createdTurn=10,status=HypothesisStatus.STRONG)
  val plan=HypothesisAgencyEngine().plan(story,"erik",h)!!
  assertEquals(AutonomousActionKind.TRAVEL,plan.kind)
  val (started,result)=AutonomousActionExecutor().execute(story,plan)
  assertEquals(ActionExecutionStatus.STARTED,result.status)
  assertNull(SpatialEngine().locationOf(started.world.spatial,"erik"))
  assertEquals(3,started.world.spatial.journeys["erik"]!!.remainingUnits)
  val oneTurn=SpatialEngine().advanceJourneys(started.world.spatial,1,13)
  assertNull(SpatialEngine().locationOf(oneTurn,"erik"))
  assertEquals(2,oneTurn.journeys["erik"]!!.remainingUnits)
 }
}