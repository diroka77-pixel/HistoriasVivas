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
 @Test fun exhaustedLocalCluesDoNotEraseTheNpcDecision(){
  val base=storyAt("ruins")
  val exhausted=base.copy(world=base.world.copy(discovery=DiscoveryState(
   signals=base.world.discovery.signals.mapValues{(_,s)->s.copy(investigatedBy=setOf("erik"))}
  )))
  val plan=HypothesisAgencyEngine().plan(exhausted,"erik",hypothesis())
  assertNotNull(plan)
  assertEquals(AutonomousActionKind.INVESTIGATE,plan.kind)
 }

 @Test fun compassionateNpcProtectsMostValuedCharacterAndNamesTarget(){
  val base=storyAt("settlement")
  val erik=base.world.characters.getValue("erik").copy(
   mind=CharacterMind(personality=PersonalityProfile(mapOf("compassion" to 90,"courage" to 40)))
  )
  val elena=Character("elena","Elena")
  val daniel=Character("daniel","Daniel")
  val story=base.copy(world=base.world.copy(
   characters=mapOf("erik" to erik,"elena" to elena,"daniel" to daniel),
   relationships=listOf(
    Relationship("erik","elena",trust=70,affection=80),
    Relationship("erik","daniel",trust=20,affection=25)
   ),
   spatial=base.world.spatial.copy(positions=mapOf(
    "erik" to CharacterPosition("erik","settlement"),
    "elena" to CharacterPosition("elena","ruins"),
    "daniel" to CharacterPosition("daniel","settlement")
   ))
  ))
  val plan=HypothesisAgencyEngine().plan(story,"erik",hypothesis())!!
  assertEquals(AutonomousActionKind.PROTECT,plan.kind)
  assertEquals("elena",plan.targetCharacterId)
 }
 @Test fun warningNamesPresentListenerInsteadOfLeavingExecutorToGuess(){
  val base=storyAt("settlement")
  val erik=base.world.characters.getValue("erik").copy(
   mind=CharacterMind(personality=PersonalityProfile(mapOf("compassion" to 70)))
  )
  val elena=Character("elena","Elena")
  val story=base.copy(world=base.world.copy(
   characters=mapOf("erik" to erik,"elena" to elena),
   relationships=listOf(Relationship("erik","elena",trust=50,affection=30)),
   spatial=base.world.spatial.copy(positions=mapOf(
    "erik" to CharacterPosition("erik","settlement"),
    "elena" to CharacterPosition("elena","settlement")
   ))
  ))
  val plan=HypothesisAgencyEngine().plan(story,"erik",hypothesis())!!
  assertEquals(AutonomousActionKind.WARN,plan.kind)
  assertEquals("elena",plan.targetCharacterId)
 }

 @Test fun secrecyTargetsSupportingSignalEvenAfterActorInvestigatedIt(){
  val erik=Character("erik","Erik",mind=CharacterMind(personality=PersonalityProfile(mapOf("secrecy" to 90))))
  val signal=WorldSignal("trace","ruins",sense=SignalSense.TRACE,surface="Machine marks",createdTurn=1,salience=80,investigatedBy=setOf("erik"))
  val evidence=Evidence("e","trace","Machine marks",70,2,"erik")
  val h=Hypothesis("hide-h","erik","ruins","Someone used the ruins",80,supportingEvidenceIds=setOf("e"),createdTurn=2,status=HypothesisStatus.STRONG)
  val story=Story("s","Nadir","p",world=WorldState(
   characters=mapOf("erik" to erik),
   spatial=SpatialState(positions=mapOf("erik" to CharacterPosition("erik","ruins"))),
   discovery=DiscoveryState(signals=mapOf("trace" to signal),evidence=mapOf("e" to evidence))
  ),turn=3)
  val plan=HypothesisAgencyEngine().plan(story,"erik",h)!!
  assertEquals(AutonomousActionKind.HIDE_EVIDENCE,plan.kind)
  assertEquals("trace",plan.targetSignalId)
 }

}
