package com.historiasvivas.engine.director
import com.historiasvivas.engine.character.*
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.world.*
import com.historiasvivas.engine.discovery.*
import com.historiasvivas.engine.spatial.*
import kotlin.test.*
class CausalWorldEngineTest {
 @Test fun crisisBecomesRealityOnlyWhenCausesConverge(){
  val story=Story("s","Nadir","p",world=WorldState(environment=EnvironmentState(
   resources=mapOf("water" to ResourcePool("water",18,100,20)),
   pressures=mapOf("fauna" to WorldPressure("fauna",70,0,"Fauna near perimeter"))
  )),turn=10)
  val result=CausalWorldEngine().advance(story)
  assertEquals(1,result.publicEvents.size)
  assertTrue(result.consequences.any{it.description.contains("water")})
 }
 @Test fun privateSocialFractureDoesNotBecomePlayerKnowledge(){
  val memories=mapOf(
   "e1" to EmotionalTrace("e1","betrayal",mapOf("anger" to 80),90),
   "e2" to EmotionalTrace("e2","fear",mapOf("fear" to 70),80)
  )
  val erik=Character("erik","Erik",mind=CharacterMind(emotionalMemory=memories))
  val daniel=Character("daniel","Daniel")
  val story=Story("s","Nadir","p",world=WorldState(
   characters=mapOf("erik" to erik,"daniel" to daniel),
   relationships=listOf(Relationship("erik","daniel",trust=-60))
  ),turn=20)
  val result=CausalWorldEngine().advance(story)
  assertTrue(result.publicEvents.isEmpty())
  assertEquals(1,result.hiddenEvents.size)
  assertFalse("daniel" in result.hiddenEvents.first().knownBy)
 }
 @Test fun discoveryOpportunityMaterializesWithoutLeakingHiddenCause(){
  val signals=mapOf(
   "a" to WorldSignal("a","ruins",sense=SignalSense.RADIO,surface="Repeating pulse",hiddenCause="secret beacon",createdTurn=1,salience=80),
   "b" to WorldSignal("b","ruins",sense=SignalSense.TRACE,surface="Machine marks",hiddenCause="covert team",createdTurn=2,salience=75)
  )
  val story=Story("s","Nadir","p",world=WorldState(
   characters=mapOf("erik" to Character("erik","Erik")),discovery=DiscoveryState(signals=signals)
  ),turn=5)
  val result=CausalWorldEngine().advance(story)
  val event=result.publicEvents.single()
  assertTrue("discovery" in event.tags)
  assertFalse(event.summary.contains("secret beacon"))
  assertFalse(event.summary.contains("covert team"))
  assertEquals(1,result.consequences.size)
  val consequence=result.consequences.single()
  assertTrue(consequence.description.contains("a"))
  assertTrue(consequence.description.contains("b"))
  assertFalse(consequence.description.contains("secret beacon"))
  assertFalse(consequence.description.contains("covert team"))
 }
 @Test fun expeditionDangerMaterializesAsPendingRiskNotAutomaticDamage(){
  val env=EnvironmentState(pressures=mapOf("weather" to WorldPressure("weather",80,1,"Severe storm")))
  val base=SpatialState(positions=mapOf("erik" to CharacterPosition("erik","settlement")),routes=listOf(Route("settlement","ruins",3)))
  val travelling=SpatialEngine().startJourney(base,"erik","ruins",4)
  val story=Story("s","Nadir","p",world=WorldState(
   characters=mapOf("erik" to Character("erik","Erik")),environment=env,spatial=travelling
  ),turn=5)
  val result=CausalWorldEngine().advance(story)
  assertTrue(result.publicEvents.single().tags.contains("expedition"))
  assertEquals(1,result.consequences.size)
  assertFalse(result.consequences.single().resolved)
 }

 @Test fun staleDiscoveryProposalIsRejectedAfterSignalsAreResolved(){
  val signals=mapOf(
   "a" to WorldSignal("a","ruins",sense=SignalSense.RADIO,surface="Pulse",createdTurn=1,salience=80,investigatedBy=setOf("erik")),
   "b" to WorldSignal("b","ruins",sense=SignalSense.TRACE,surface="Marks",createdTurn=2,salience=75,investigatedBy=setOf("erik"))
  )
  val story=Story("s","Nadir","p",world=WorldState(characters=mapOf("erik" to Character("erik","Erik")),discovery=DiscoveryState(signals=signals)),turn=5)
  val stale=EmergentEventProposal("d",EmergentEventKind.DISCOVERY_OPPORTUNITY,70,listOf(CausalFactor("signals",45,"old"),CausalFactor("salience",25,"old")),"Old opportunity",false)
  val candidate=CausalEventMaterializer().materialize(story,stale)!!
  assertFalse(CausalEventValidator().validate(story,candidate).accepted)
 }

 @Test fun deadObserversCannotKeepDiscoveryProposalCausal(){
  val signals=mapOf(
   "a" to WorldSignal("a","ruins",sense=SignalSense.RADIO,surface="Pulse",createdTurn=1,salience=80,investigatedBy=setOf("erik")),
   "b" to WorldSignal("b","ruins",sense=SignalSense.TRACE,surface="Marks",createdTurn=2,salience=75,investigatedBy=setOf("erik"))
  )
  val characters=mapOf(
   "erik" to Character("erik","Erik"),
   "ghost" to Character("ghost","Ghost",alive=false)
  )
  val story=Story("s","Nadir","p",world=WorldState(characters=characters,discovery=DiscoveryState(signals=signals)),turn=5)
  val stale=EmergentEventProposal("d",EmergentEventKind.DISCOVERY_OPPORTUNITY,70,listOf(CausalFactor("signals",45,"old"),CausalFactor("salience",25,"old")),"Old opportunity",false)
  val candidate=CausalEventMaterializer().materialize(story,stale)!!
  assertFalse(CausalEventValidator().validate(story,candidate).accepted)
 }

 @Test fun deadObserversAreNotParticipantsInDiscoveryOpportunity(){
  val signals=mapOf(
   "a" to WorldSignal("a","ruins",sense=SignalSense.RADIO,surface="Pulse",createdTurn=1,salience=80,noticedBy=setOf("erik","ghost")),
   "b" to WorldSignal("b","ruins",sense=SignalSense.TRACE,surface="Marks",createdTurn=2,salience=75,noticedBy=setOf("ghost"))
  )
  val story=Story("s","Nadir","p",world=WorldState(
   characters=mapOf("erik" to Character("erik","Erik"),"ghost" to Character("ghost","Ghost",alive=false)),
   discovery=DiscoveryState(signals=signals)
  ),turn=5)
  val event=CausalWorldEngine().advance(story).publicEvents.single()
  assertEquals(setOf("erik"),event.participants)
 }

 @Test fun deadObserversAreNotParticipantsInDiscoveryOpportunity(){
  val signals=mapOf(
   "a" to WorldSignal("a","ruins",sense=SignalSense.RADIO,surface="Pulse",createdTurn=1,salience=80,noticedBy=setOf("erik","ghost")),
   "b" to WorldSignal("b","ruins",sense=SignalSense.TRACE,surface="Marks",createdTurn=2,salience=75,noticedBy=setOf("ghost"))
  )
  val story=Story("s","Nadir","p",world=WorldState(
   characters=mapOf("erik" to Character("erik","Erik"),"ghost" to Character("ghost","Ghost",alive=false)),
   discovery=DiscoveryState(signals=signals)
  ),turn=5)
  val event=CausalWorldEngine().advance(story).publicEvents.single()
  assertEquals(setOf("erik"),event.participants)
 }

}
