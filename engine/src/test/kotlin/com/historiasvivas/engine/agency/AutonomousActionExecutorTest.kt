package com.historiasvivas.engine.agency
import com.historiasvivas.engine.character.*
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.reasoning.*
import com.historiasvivas.engine.spatial.*
import com.historiasvivas.engine.discovery.*
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
 @Test fun autonomousInvestigationCreatesKnowledgeOnlyForInvestigatingNpc(){
  val erik=Character("erik","Erik",mind=CharacterMind(personality=PersonalityProfile(mapOf("caution" to 30))))
  val signal=WorldSignal("pulse","ruins",sense=SignalSense.RADIO,surface="A weak repeating pulse",hiddenCause="buried beacon",createdTurn=5,salience=70)
  val spatial=SpatialState(positions=mapOf("erik" to CharacterPosition("erik","ruins")))
  val story=Story("s","Nadir","p",world=WorldState(characters=mapOf("erik" to erik),spatial=spatial,discovery=DiscoveryState(signals=mapOf(signal.id to signal))),turn=12)
  val plan=AutonomousActionPlan("plan","erik","h",AutonomousActionKind.INVESTIGATE,60,motivation="private",turn=12)
  val (after,result)=AutonomousActionExecutor().execute(story,plan)
  assertEquals(ActionExecutionStatus.STARTED,result.status)
  assertEquals(SignalStatus.INVESTIGATED,after.world.discovery.signals.getValue("pulse").statusFor("erik"))
  assertEquals(SignalStatus.UNNOTICED,after.world.discovery.signals.getValue("pulse").statusFor("daniel"))
  assertTrue(after.world.discovery.evidence.values.any{it.discoveredBy=="erik"})
  assertTrue(after.world.discovery.interpretations.values.any{it.characterId=="erik"})
  assertTrue(after.world.discovery.evidence.values.none{it.discoveredBy=="daniel"})
 }

 @Test fun autonomousInvestigationChoosesMostSalientObservableSignal(){
  val erik=Character("erik","Erik")
  val low=WorldSignal("low","ruins",sense=SignalSense.SIGHT,surface="A faint mark",createdTurn=5,salience=30)
  val high=WorldSignal("high","ruins",sense=SignalSense.RADIO,surface="A strong repeating pulse",createdTurn=5,salience=90)
  val story=Story(
   "s","Nadir","p",
   world=WorldState(
    characters=mapOf("erik" to erik),
    spatial=SpatialState(positions=mapOf("erik" to CharacterPosition("erik","ruins"))),
    discovery=DiscoveryState(signals=mapOf(low.id to low,high.id to high))
   ),
   turn=12
  )
  val plan=AutonomousActionPlan("plan","erik","h",AutonomousActionKind.INVESTIGATE,60,motivation="private",turn=12)
  val (after,result)=AutonomousActionExecutor().execute(story,plan)
  assertEquals(ActionExecutionStatus.STARTED,result.status)
  assertEquals(SignalStatus.INVESTIGATED,after.world.discovery.signals.getValue("high").statusFor("erik"))
  assertEquals(SignalStatus.UNNOTICED,after.world.discovery.signals.getValue("low").statusFor("erik"))
  assertTrue(after.world.discovery.evidence.values.any{it.signalId=="high"&&it.discoveredBy=="erik"})
  assertTrue(after.world.discovery.evidence.values.none{it.signalId=="low"&&it.discoveredBy=="erik"})
 }

 @Test fun cautiousWithdrawalStartsPhysicalJourneyAwayFromThreat(){
  val erik=Character("erik","Erik")
  val story=Story(
   "s","Nadir","p",
   world=WorldState(
    characters=mapOf("erik" to erik),
    locations=mapOf("ruins" to Location("ruins","Ruins"),"settlement" to Location("settlement","Settlement"),"forest" to Location("forest","Forest")),
    spatial=SpatialState(
     positions=mapOf("erik" to CharacterPosition("erik","ruins")),
     routes=listOf(Route("ruins","settlement",2),Route("ruins","forest",3))
    )
   ),turn=20
  )
  val plan=AutonomousActionPlan("withdraw","erik","h",AutonomousActionKind.WITHDRAW,80,targetLocationId="ruins",motivation="Danger",turn=20)
  val (after,result)=AutonomousActionExecutor().execute(story,plan)
  assertEquals(ActionExecutionStatus.STARTED,result.status)
  val journey=after.world.spatial.journeys.getValue("erik")
  assertEquals("ruins",journey.from)
  assertNotEquals("ruins",journey.to)
  assertTrue(journey.to in setOf("forest","settlement"))
 }

 @Test fun warningTransfersClaimAsListenerBeliefWithoutSharingPrivateHypothesisState(){
  val erik=Character("erik","Erik",mind=CharacterMind(personality=PersonalityProfile(mapOf("honesty" to 80,"loyalty" to 80))))
  val elena=Character("elena","Elena")
  val h=Hypothesis("h","erik","ruins","The ruins may be dangerous",80,setOf("e1","e2"),createdTurn=10,status=HypothesisStatus.STRONG)
  val story=Story("s","Nadir","p",world=WorldState(
   characters=mapOf("erik" to erik,"elena" to elena),
   relationships=listOf(Relationship("erik","elena",trust=80)),
   spatial=SpatialState(positions=mapOf("erik" to CharacterPosition("erik","settlement"),"elena" to CharacterPosition("elena","settlement"))),
   reasoning=ReasoningState(hypotheses=mapOf(h.id to h))
  ),turn=12)
  val plan=AutonomousActionPlan("warn","erik","h",AutonomousActionKind.WARN,80,targetCharacterId="elena",motivation="Protect Elena",turn=12)
  val (after,result)=AutonomousActionExecutor().execute(story,plan)
  assertEquals(ActionExecutionStatus.STARTED,result.status)
  assertTrue(after.world.characters.getValue("elena").mind.beliefs.any{it.claim==h.claim})
  assertEquals(setOf("h"),after.world.reasoning.hypotheses.keys)
  assertTrue(after.world.socialKnowledge.testimonies.isNotEmpty())
  assertTrue(after.world.socialKnowledge.heardClaims.values.any{it.listenerId=="elena"})
 }
 @Test fun warningCannotTransferKnowledgeToAbsentListener(){
  val erik=Character("erik","Erik")
  val elena=Character("elena","Elena")
  val h=Hypothesis("h","erik","ruins","The ruins may be dangerous",80,setOf("e1","e2"),createdTurn=10,status=HypothesisStatus.STRONG)
  val story=Story("s","Nadir","p",world=WorldState(
   characters=mapOf("erik" to erik,"elena" to elena),
   spatial=SpatialState(positions=mapOf("erik" to CharacterPosition("erik","ruins"),"elena" to CharacterPosition("elena","settlement"))),
   reasoning=ReasoningState(hypotheses=mapOf(h.id to h))
  ),turn=12)
  val plan=AutonomousActionPlan("warn","erik","h",AutonomousActionKind.WARN,80,targetCharacterId="elena",motivation="Warn",turn=12)
  val (after,result)=AutonomousActionExecutor().execute(story,plan)
  assertEquals(ActionExecutionStatus.BLOCKED,result.status)
  assertTrue(after.world.characters.getValue("elena").mind.beliefs.isEmpty())
  assertTrue(after.world.socialKnowledge.heardClaims.isEmpty())
 }

 @Test fun hidingEvidenceReducesObservableTraceWithoutErasingPriorKnowledgeOrCause(){
  val erik=Character("erik","Erik")
  val signal=WorldSignal("trace","ruins",sense=SignalSense.TRACE,surface="Fresh tracks lead below",hiddenCause="erik opened the sealed hatch",createdTurn=5,salience=90)
  val evidence=Evidence("e","trace","Fresh tracks lead below",80,6,"elena")
  val story=Story("s","Nadir","p",world=WorldState(
   characters=mapOf("erik" to erik),
   spatial=SpatialState(positions=mapOf("erik" to CharacterPosition("erik","ruins"))),
   discovery=DiscoveryState(signals=mapOf("trace" to signal),evidence=mapOf("e" to evidence))
  ),turn=12)
  val plan=AutonomousActionPlan("hide","erik","h",AutonomousActionKind.HIDE_EVIDENCE,80,motivation="Conceal",turn=12)
  val (after,result)=AutonomousActionExecutor().execute(story,plan)
  assertEquals(ActionExecutionStatus.STARTED,result.status)
  val hidden=after.world.discovery.signals.getValue("trace")
  assertTrue(hidden.salience<signal.salience)
  assertFalse(hidden.persistent)
  assertEquals(signal.hiddenCause,hidden.hiddenCause)
  assertEquals(evidence,after.world.discovery.evidence.getValue("e"))
 }

 @Test fun protectStartsJourneyTowardReachableCharacter(){
  val erik=Character("erik","Erik")
  val elena=Character("elena","Elena")
  val story=Story("s","Nadir","p",world=WorldState(
   characters=mapOf("erik" to erik,"elena" to elena),
   spatial=SpatialState(
    positions=mapOf("erik" to CharacterPosition("erik","ruins"),"elena" to CharacterPosition("elena","settlement")),
    routes=listOf(Route("ruins","settlement",3))
   )
  ),turn=15)
  val plan=AutonomousActionPlan("protect","erik","h",AutonomousActionKind.PROTECT,80,targetCharacterId="elena",motivation="Protect Elena",turn=15)
  val (after,result)=AutonomousActionExecutor().execute(story,plan)
  assertEquals(ActionExecutionStatus.STARTED,result.status)
  assertEquals("settlement",after.world.spatial.journeys.getValue("erik").to)
 }
 @Test fun protectDoesNotInventActionWhenAlreadyTogether(){
  val erik=Character("erik","Erik")
  val elena=Character("elena","Elena")
  val story=Story("s","Nadir","p",world=WorldState(
   characters=mapOf("erik" to erik,"elena" to elena),
   spatial=SpatialState(positions=mapOf("erik" to CharacterPosition("erik","settlement"),"elena" to CharacterPosition("elena","settlement")))
  ),turn=15)
  val plan=AutonomousActionPlan("protect","erik","h",AutonomousActionKind.PROTECT,80,targetCharacterId="elena",motivation="Protect Elena",turn=15)
  val (_,result)=AutonomousActionExecutor().execute(story,plan)
  assertEquals(ActionExecutionStatus.DEFERRED,result.status)
 }

}
