package com.historiasvivas.engine.social

import com.historiasvivas.engine.character.*
import com.historiasvivas.engine.model.*
import kotlin.test.*

class RelationshipRepairEngineTest {
 private fun revision()=BeliefRevision(
  id="revision-1",
  previous=Belief("erik:t1","Erik never entered the ruins",70,"t1"),
  revised=Belief("erik:t1","Erik never entered the ruins",0,"t1",status=BeliefStatus.REJECTED),
  confirmed=Belief("erik:t1","Erik entered the ruins",90,"e1"),
  evidenceId="e1",
  sourceCharacterId="erik",
  turn=10
 )
 private fun story():Story{
  val elena=Character(
   "elena","Elena",
   mind=CharacterMind(
    personality=PersonalityProfile(mapOf("compassion" to 90,"patience" to 60)),
    beliefRevisions=listOf(revision())
   )
  )
  return Story(
   "s","Nadir","p",turn=12,
   world=WorldState(
    characters=mapOf("elena" to elena,"erik" to Character("erik","Erik")),
    relationships=listOf(Relationship("elena","erik",trust=-40,affection=20,fear=15))
   )
  )
 }
 private fun attempt(id:String="repair-1",acts:Set<RepairAct> = RepairAct.entries.toSet())=RelationshipRepairAttempt(
  id=id,offenderId="erik",harmedId="elena",beliefRevisionId="revision-1",
  acts=acts,sincerity=90,turn=12
 )

 @Test fun sincereAdmissionApologyAndRestitutionOnlyPartlyRepairTrust(){
  val result=RelationshipRepairEngine().apply(story(),attempt())!!
  assertTrue(result.outcome.accepted)
  assertTrue(result.outcome.trustChange in 3..25)
  assertTrue(result.outcome.remainingDamage>=20)
  val relationship=result.story.world.relationships.single{it.fromCharacterId=="elena"&&it.toCharacterId=="erik"}
  assertTrue(relationship.trust>-40)
  assertTrue(relationship.trust<0)
  assertEquals(1,result.story.world.characters.getValue("elena").mind.beliefRevisions.size)
  assertTrue(result.story.world.characters.getValue("elena").mind.personalMemories.any{it.contains("incomplete")})
  assertEquals(result.outcome,result.story.world.socialKnowledge.relationshipRepairs.getValue("repair-1"))
 }
 @Test fun apologyWithoutAdmissionIsRejectedAndCanReduceTrust(){
  val result=RelationshipRepairEngine().apply(story(),attempt(acts=setOf(RepairAct.APOLOGY)))!!
  assertFalse(result.outcome.accepted)
  assertEquals(-5,result.outcome.trustChange)
  val relationship=result.story.world.relationships.single{it.fromCharacterId=="elena"&&it.toCharacterId=="erik"}
  assertEquals(-45,relationship.trust)
 }
 @Test fun sameAttemptIsIdempotentAndNewIdsCannotFarmUnlimitedTrust(){
  val engine=RelationshipRepairEngine()
  val first=engine.apply(story(),attempt("repair-1"))!!
  val duplicate=engine.apply(first.story,attempt("repair-1"))!!
  assertEquals(first.story,duplicate.story)

  val second=engine.apply(duplicate.story,attempt("repair-2"))!!
  val third=engine.apply(second.story,attempt("repair-3"))!!
  val total=third.story.world.socialKnowledge.relationshipRepairs.values
   .filter{it.accepted&&it.beliefRevisionId=="revision-1"}
   .sumOf{it.trustChange.coerceAtLeast(0)}
  assertTrue(total<=25)
 }
 @Test fun repairCannotTargetAnUnrelatedBeliefRevision(){
  val invalid=attempt().copy(beliefRevisionId="missing")
  assertNull(RelationshipRepairEngine().apply(story(),invalid))
 }
}
