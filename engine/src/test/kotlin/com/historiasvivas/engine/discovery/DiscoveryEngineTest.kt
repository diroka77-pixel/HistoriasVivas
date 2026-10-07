package com.historiasvivas.engine.discovery
import kotlin.test.*
class DiscoveryEngineTest {
 @Test fun travellerSeesSurfaceButHiddenCauseRemainsSeparate(){
  val signal=WorldSignal("tracks","forest",routeFrom="settlement",routeTo="ruins",sense=SignalSense.TRACE,surface="Broken branches and fresh tracks leave the path",hiddenCause="Erik passed here before dawn",createdTurn=4,salience=80)
  val state=DiscoveryState(mapOf(signal.id to signal)); val engine=DiscoveryEngine()
  val visible=engine.visibleOnJourney(state,"daniel","settlement","ruins")
  assertEquals("Broken branches and fresh tracks leave the path",visible.single().surface)
  assertEquals(SignalStatus.UNNOTICED,visible.single().statusFor("daniel"))
  val noticed=engine.notice(state,"tracks","daniel")
  assertEquals(SignalStatus.NOTICED,noticed.signals["tracks"]!!.statusFor("daniel"))
 }
 @Test fun ignoredSignalPersistsAndCanStillExistForSomeoneElse(){
  val signal=WorldSignal("radio","settlement",sense=SignalSense.RADIO,surface="A weak repeating pulse",hiddenCause="Old beacon",createdTurn=8)
  val engine=DiscoveryEngine(); val ignored=engine.ignore(DiscoveryState(mapOf("radio" to signal)),"radio","daniel")
  assertEquals(SignalStatus.IGNORED,ignored.signals["radio"]!!.statusFor("daniel"))
  assertTrue(engine.visibleAt(ignored,"daniel","settlement").isEmpty())
  assertEquals(SignalStatus.UNNOTICED,ignored.signals["radio"]!!.statusFor("elena"))
  assertEquals(listOf("radio"),engine.visibleAt(ignored,"elena","settlement").map{it.id})
 }
 @Test fun ignoredJourneySignalDoesNotImmediatelyReappearForSameTraveller(){
  val signal=WorldSignal("tracks","forest",routeFrom="settlement",routeTo="ruins",sense=SignalSense.TRACE,surface="Fresh tracks",createdTurn=4)
  val engine=DiscoveryEngine()
  val ignored=engine.ignore(DiscoveryState(mapOf(signal.id to signal)),signal.id,"daniel")
  assertTrue(engine.visibleOnJourney(ignored,"daniel","settlement","ruins").isEmpty())
  assertEquals(listOf("tracks"),engine.visibleOnJourney(ignored,"elena","settlement","ruins").map{it.id})
 }

 @Test fun ignoredPersistentSignalCanReturnOnlyAfterRealRevision(){
  val signal=WorldSignal("pulse","settlement",sense=SignalSense.RADIO,surface="A weak repeating pulse",createdTurn=8)
  val engine=DiscoveryEngine()
  val ignored=engine.ignore(DiscoveryState(mapOf(signal.id to signal)),signal.id,"daniel")
  assertTrue(engine.visibleAt(ignored,"daniel","settlement").isEmpty())
  val unchanged=engine.reactivate(ignored,signal.id,8)
  assertTrue(engine.visibleAt(unchanged,"daniel","settlement").isEmpty())
  val changed=engine.reactivate(ignored,signal.id,12,newSurface="The pulse is stronger and now carries a pattern",newSalience=85)
  assertEquals(1,changed.signals[signal.id]!!.revision)
  assertEquals(12,changed.signals[signal.id]!!.lastChangedTurn)
  assertEquals("The pulse is stronger and now carries a pattern",engine.visibleAt(changed,"daniel","settlement").single().surface)
 }

 @Test fun nonPersistentIgnoredSignalCannotBeReactivated(){
  val signal=WorldSignal("echo","ruins",sense=SignalSense.SOUND,surface="A single metallic echo",createdTurn=3,persistent=false)
  val engine=DiscoveryEngine()
  val ignored=engine.ignore(DiscoveryState(mapOf(signal.id to signal)),signal.id,"daniel")
  val attempted=engine.reactivate(ignored,signal.id,9,newSurface="Another echo")
  assertEquals(SignalStatus.IGNORED,attempted.signals[signal.id]!!.statusFor("daniel"))
  assertTrue(engine.visibleAt(attempted,"daniel","ruins").isEmpty())
 }

 @Test fun materiallyRevisedSignalCanBeInvestigatedAgainWithoutErasingKnowledge(){
  val original=WorldSignal("trace","ruins",sense=SignalSense.TRACE,surface="Faint tracks",createdTurn=1,salience=40,investigatedBy=setOf("erik"))
  val evidence=Evidence("e1","trace","Faint tracks",40,2,"erik")
  val state=DiscoveryState(signals=mapOf("trace" to original),evidence=mapOf("e1" to evidence))
  val revised=DiscoveryEngine().reactivate(state,"trace",5,newSurface="Fresh tracks cross the old trail",newSalience=80)
  val signal=revised.signals.getValue("trace")
  assertEquals(1,signal.revision)
  assertFalse("erik" in signal.investigatedBy)
  assertTrue(DiscoveryEngine().visibleAt(revised,"erik","ruins").any{it.id=="trace"})
  assertEquals(evidence,revised.evidence.getValue("e1"))
 }

}
