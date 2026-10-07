package com.historiasvivas.engine.agency

import com.historiasvivas.engine.character.*
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.reasoning.*
import com.historiasvivas.engine.spatial.*
import com.historiasvivas.engine.discovery.*
import kotlin.test.*

class HypothesisAgencyLocationTest {
 private fun storyAt(location:String)=Story(
  "s","Nadir","p",
  world=WorldState(
   characters=mapOf("erik" to Character("erik","Erik",mind=CharacterMind(personality=PersonalityProfile(mapOf("courage" to 60))))),
   locations=mapOf("settlement" to Location("settlement","Settlement"),"ruins" to Location("ruins","Ruins")),
   spatial=SpatialState(positions=mapOf("erik" to CharacterPosition("erik",location)),routes=listOf(Route("settlement","ruins",3))),
   discovery=DiscoveryState(signals=mapOf("signal" to WorldSignal("signal","ruins",sense=SignalSense.RADIO,surface="A pulse",createdTurn=10,salience=80)))
  ),turn=12
 )
 private fun hypothesis(status:HypothesisStatus=HypothesisStatus.STRONG)=Hypothesis(
  "h","erik","ruins","The ruins contain an active source",80,setOf("e1","e2"),createdTurn=10,status=status
 )

 @Test fun npcAlreadyAtHypothesisLocationInvestigatesInsteadOfTravellingToItself(){
  val plan=HypothesisAgencyEngine().plan(storyAt("ruins"),"erik",hypothesis())!!
  assertEquals(AutonomousActionKind.INVESTIGATE,plan.kind)
 }
 @Test fun rejectedHypothesisProducesNoAutonomousPlan(){
  assertNull(HypothesisAgencyEngine().plan(storyAt("settlement"),"erik",hypothesis(HypothesisStatus.REJECTED)))
 }
 @Test fun npcDoesNotCreateInvestigationPlanWhenNoObservableClueRemains(){
  val base=storyAt("ruins")
  val exhausted=base.copy(world=base.world.copy(discovery=DiscoveryState(
   signals=base.world.discovery.signals.mapValues{(_,s)->s.copy(investigatedBy=setOf("erik"))}
  )))
  assertNull(HypothesisAgencyEngine().plan(exhausted,"erik",hypothesis()))
 }

}
