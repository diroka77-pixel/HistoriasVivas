package com.historiasvivas.engine.discovery
import com.historiasvivas.engine.character.*
import kotlin.test.*
class InvestigationEngineTest {
 @Test fun sameEvidenceCanProduceDifferentSubjectiveConfidence(){
  val signal=WorldSignal("tracks",null,sense=SignalSense.TRACE,surface="Fresh narrow tracks leave the route",hiddenCause="Erik",createdTurn=3,salience=70)
  val state=DiscoveryState(signals=mapOf("tracks" to signal))
  val cautious=CharacterMind(personality=PersonalityProfile(mapOf("caution" to 80,"trust" to -40)))
  val trusting=CharacterMind(personality=PersonalityProfile(mapOf("caution" to -20,"trust" to 70)))
  val engine=InvestigationEngine()
  val daniel=engine.investigate(state,"tracks","daniel",cautious,4)!!
  val elena=engine.investigate(daniel.state,"tracks","elena",trusting,4)!!
  assertNotEquals(daniel.interpretation.confidence,elena.interpretation.confidence)
  assertEquals("Erik",daniel.state.signals["tracks"]!!.hiddenCause)
  assertFalse(daniel.interpretation.hypothesis.contains("Erik"))
 }
 @Test fun furtherEvidenceRevisesRatherThanErasesPreviousInterpretation(){
  val signal=WorldSignal("radio",null,sense=SignalSense.RADIO,surface="A repeating pulse",hiddenCause="beacon",createdTurn=1,salience=60)
  val engine=InvestigationEngine(); val mind=CharacterMind()
  val first=engine.investigate(DiscoveryState(signals=mapOf("radio" to signal)),"radio","daniel",mind,2)!!
  val second=engine.investigate(first.state,"radio","daniel",mind,3)!!
  assertEquals(first.interpretation.id,second.interpretation.revisedFrom)
  assertTrue(second.interpretation.confidence>first.interpretation.confidence)
  assertTrue(second.state.interpretations.containsKey(first.interpretation.id))
 }
}