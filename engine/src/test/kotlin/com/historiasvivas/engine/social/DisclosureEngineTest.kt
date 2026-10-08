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
 @Test fun rememberedDeceptionMakesSpeakerGuardFutureInformation(){
  val personality=PersonalityProfile(mapOf("honesty" to 60,"loyalty" to 50))
  val clean=Character("erik","Erik",mind=CharacterMind(personality=personality))
  val relation=Relationship("erik","daniel",trust=45)
  val openDecision=DisclosureEngine().decide(story(clean,relation),"erik","daniel",h)!!
  assertEquals(DisclosureKind.TELL,openDecision.kind)

  val revision=BeliefRevision(
   id="r1",
   previous=Belief("daniel:t1","The ruins are empty",70,"t1"),
   revised=Belief("daniel:t1","The ruins are empty",0,"t1",status=BeliefStatus.REJECTED),
   confirmed=Belief("daniel:t1","A transmitter is active",90,"e1"),
   evidenceId="e1",
   sourceCharacterId="daniel",
   turn=19
  )
  val guarded=clean.copy(mind=clean.mind.copy(beliefRevisions=listOf(revision)))
  val guardedDecision=DisclosureEngine().decide(story(guarded,relation),"erik","daniel",h)!!
  assertEquals(DisclosureKind.DELAY,guardedDecision.kind)
  assertTrue(guardedDecision.motivation.contains("rememberedFalseAccounts=1"))
  assertNull(DisclosureEngine().toTestimony(guardedDecision,h))
 }
 @Test fun disclosureGuardDecaysWhenNoNewDeceptionOccurs(){
  val revision=BeliefRevision(
   id="decay-r",
   previous=Belief("daniel:t1","The ruins are empty",70,"t1"),
   revised=Belief("daniel:t1","The ruins are empty",0,"t1",status=BeliefStatus.REJECTED),
   confirmed=Belief("daniel:t1","A transmitter is active",90,"e1"),
   evidenceId="e1",
   sourceCharacterId="daniel",
   turn=20
  )
  val erik=Character(
   "erik","Erik",
   mind=CharacterMind(
    personality=PersonalityProfile(mapOf("honesty" to 60,"loyalty" to 50)),
    beliefRevisions=listOf(revision)
   )
  )
  val relation=Relationship("erik","daniel",trust=45)
  val guarded=story(erik,relation).copy(turn=20)
  val recovered=story(erik,relation).copy(turn=35)
  assertEquals(DisclosureKind.DELAY,DisclosureEngine().decide(guarded,"erik","daniel",h)!!.kind)
  assertEquals(DisclosureKind.TELL,DisclosureEngine().decide(recovered,"erik","daniel",h)!!.kind)
 }
 @Test fun acceptedRepairSoftensButDoesNotEraseDisclosureGuard(){
  val revision=BeliefRevision(
   id="repairable-r",
   previous=Belief("daniel:t1","The ruins are empty",70,"t1"),
   revised=Belief("daniel:t1","The ruins are empty",0,"t1",status=BeliefStatus.REJECTED),
   confirmed=Belief("daniel:t1","A transmitter is active",90,"e1"),
   evidenceId="e1",
   sourceCharacterId="daniel",
   turn=20
  )
  val erik=Character(
   "erik","Erik",
   mind=CharacterMind(
    personality=PersonalityProfile(mapOf("honesty" to 60,"loyalty" to 50)),
    beliefRevisions=listOf(revision)
   )
  )
  val damaged=story(erik,Relationship("erik","daniel",trust=45)).copy(turn=20)
  val before=DisclosureEngine().decide(damaged,"erik","daniel",h)!!
  assertEquals(DisclosureKind.DELAY,before.kind)

  val repaired=RelationshipRepairEngine().apply(
   damaged,
   RelationshipRepairAttempt(
    id="repair",offenderId="daniel",harmedId="erik",beliefRevisionId=revision.id,
    acts=setOf(RepairAct.ADMISSION,RepairAct.APOLOGY,RepairAct.RESTITUTION),
    sincerity=90,turn=20
   )
  )!!
  assertTrue(repaired.outcome.accepted)
  val after=DisclosureEngine().decide(repaired.story,"erik","daniel",h)!!
  assertEquals(DisclosureKind.PARTIAL,after.kind)
  assertTrue(after.motivation.contains("repairRelief="))
  assertNotEquals(DisclosureKind.TELL,after.kind)
 }
}