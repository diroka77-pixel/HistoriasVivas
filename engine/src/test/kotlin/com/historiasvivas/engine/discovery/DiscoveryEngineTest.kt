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
  assertEquals(SignalStatus.UNNOTICED,ignored.signals["radio"]!!.statusFor("elena"))
 }
}