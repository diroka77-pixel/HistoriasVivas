package com.historiasvivas.engine
import com.historiasvivas.engine.character.Belief
import com.historiasvivas.engine.information.*
import kotlin.test.*

class SubjectiveInformationTest {
 @Test fun evidenceUsuallyCreatesStrongerBeliefThanRumor(){
  val resolver=BeliefResolver()
  val evidence=resolver.beliefFrom(ReceivedInformation("daniel",InformationClaim("e","ruins","Erik entered the ruins",InformationKind.EVIDENCE,reliability=80)))
  val rumor=resolver.beliefFrom(ReceivedInformation("daniel",InformationClaim("r","ruins","Erik fled the colony",InformationKind.RUMOR,reliability=50)))
  assertTrue(evidence.confidence>rumor.confidence)
 }
 @Test fun trustedLiarCanStillCreateAConvincingFalseBelief(){
  val resolver=BeliefResolver()
  val lie=InformationClaim("l","erik","Erik betrayed us",InformationKind.LIE,sourceCharacterId="elena",reliability=90,intentionallyFalse=true)
  val belief=resolver.beliefFrom(ReceivedInformation("daniel",lie,sourceTrust=90))
  assertTrue(belief.confidence>=50)
  assertEquals("Erik betrayed us",belief.claim)
 }
 @Test fun contradictoryVersionsRemainSeparate(){
  val old=Belief("ruins","The ruins are abandoned",75)
  val incoming=Belief("ruins","Someone lives inside the ruins",70)
  val contradictions=ContradictionDetector().contradictions(listOf(old),incoming)
  assertEquals(listOf(old),contradictions)
 }
}
