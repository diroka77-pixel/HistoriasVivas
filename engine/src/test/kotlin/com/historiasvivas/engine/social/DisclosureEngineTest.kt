package com.historiasvivas.engine.social
import com.historiasvivas.engine.character.*
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.reasoning.*
import kotlin.test.*
class DisclosureEngineTest {
 private fun story(speaker:Character,relationship:Relationship)=Story("s","Nadir","p",world=WorldState(characters=mapOf(speaker.id to speaker,"daniel" to Character("daniel","Daniel")),relationships=listOf(relationship)),turn=20)
 private val h=Hypothesis("h","erik","ruins","A transmitter is active beneath the ruins",82,setOf("e1","e2"),createdTurn=18,status=HypothesisStatus.STRONG)
 @Test fun trustedHonestNpcSharesStrongHypothesis(){
  val erik=Character("erik","Erik",mind=CharacterMind(personality=PersonalityProfile(mapOf("honesty" to 70,"loyalty" to 60))))
  val s=story(erik,Relationship("erik","daniel",trust=70))
  val d=DisclosureEngine().decide(s,"erik","daniel",h)!!
  assertEquals(DisclosureKind.TELL,d.kind)
  assertEquals(TestimonyIntent.HONEST,DisclosureEngine().toTestimony(d,h)!!.intent)
 }
 @Test fun frightenedNpcCanLieWithoutChangingPrivateHypothesis(){
  val erik=Character("erik","Erik",mind=CharacterMind(personality=PersonalityProfile(mapOf("honesty" to -30,"caution" to 60))))
  val s=story(erik,Relationship("erik","daniel",trust=-20,fear=85))
  val d=DisclosureEngine().decide(s,"erik","daniel",h)!!
  assertEquals(DisclosureKind.LIE,d.kind)
  val testimony=DisclosureEngine().toTestimony(d,h)!!
  assertEquals(TestimonyIntent.LYING,testimony.intent)
  assertEquals(h.claim,testimony.privateTruth)
  assertNotEquals(h.claim,testimony.claim)
 }
}