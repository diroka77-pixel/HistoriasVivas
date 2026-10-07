package com.historiasvivas.engine.social
import com.historiasvivas.engine.character.*
import kotlin.test.*
class SocialInformationEngineTest {
 @Test fun lieBecomesBeliefNotTruth(){
  val lie=Testimony("t1","erik","There is nothing beneath the ruins","ruins",10,intent=TestimonyIntent.LYING,privateTruth="A signal exists beneath the ruins")
  val mind=CharacterMind(perceptions=mapOf("erik" to Perception("daniel","erik",mapOf("trust" to 60))))
  val heard=SocialInformationEngine().hear(lie,"daniel",mind)
  val belief=SocialInformationEngine().asBelief(heard)
  assertEquals("There is nothing beneath the ruins",belief.claim)
  assertTrue(belief.confidence>55)
  assertEquals(TestimonyIntent.LYING,lie.intent)
  assertNotEquals(lie.privateTruth,belief.claim)
 }
 @Test fun rumorStartsLessCredibleThanDirectTestimony(){
  val mind=CharacterMind()
  val direct=Testimony("d","elena","I saw lights in the forest","forest",4)
  val rumor=direct.copy(id="r",channel=InformationChannel.RUMOR)
  val engine=SocialInformationEngine()
  assertTrue(engine.hear(direct,"daniel",mind).confidence>engine.hear(rumor,"daniel",mind).confidence)
 }
 @Test fun multiHopRumorCannotRegainReliabilityFromTrustedRelays(){
  val engine=SocialInformationEngine()
  val rumor=RumorEngine()
  val trustingMind=CharacterMind(perceptions=mapOf(
   "b" to Perception("b",impressions=mapOf("trust" to 90)),
   "c" to Perception("c",impressions=mapOf("trust" to 90))
  ))
  val first=HeardClaim("source","b","a","There is something beneath the ruins",70,1,InformationChannel.DIRECT_TESTIMONY)
  val bToC=rumor.relay(first,"b","c",2)
  val heardByC=engine.hear(bToC,"c",trustingMind)
  assertTrue(heardByC.confidence<=55)
  val cToD=rumor.relay(heardByC,"c","d",3)
  val heardByD=engine.hear(cToD,"d",trustingMind)
  assertTrue(heardByD.confidence<=40)
  assertTrue(heardByD.confidence<heardByC.confidence)
  assertNull(bToC.privateTruth)
  assertNull(cToD.privateTruth)
 }

}
