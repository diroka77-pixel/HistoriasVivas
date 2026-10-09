package com.historiasvivas.engine.social

import com.historiasvivas.engine.character.*
import com.historiasvivas.engine.discovery.DiscoveryState
import com.historiasvivas.engine.discovery.Evidence
import com.historiasvivas.engine.model.Character
import com.historiasvivas.engine.model.Story
import com.historiasvivas.engine.model.WorldState
import com.historiasvivas.engine.offscreen.HiddenEvent
import kotlin.test.*

class RumorBeliefDecayEngineTest {
 private val eventId="hidden-repair-1"
 private val rumor=Testimony(
  "rumor-1","nora","A private repair happened","erik",5,
  channel=InformationChannel.RUMOR,intent=TestimonyIntent.UNCERTAIN,
  reliability=60,relatedEventId=eventId
 )
 private val rumorBelief=Belief(
  "nora:rumor-1",rumor.claim,60,rumor.id
 )
 private fun story(
  turn:Long=9,
  discovered:Boolean=false,
  evidence:Evidence?=null,
  direct:Boolean=false
 ):Story{
  val directTestimony=Testimony(
   "direct-1","erik","I made a limited repair","elena",8,
   channel=InformationChannel.DIRECT_TESTIMONY,intent=TestimonyIntent.HONEST,
   relatedEventId=eventId
  )
  val testimonies=mapOf(rumor.id to rumor)+
   if(direct)mapOf(directTestimony.id to directTestimony) else emptyMap()
  val claims=if(direct)mapOf(
   directTestimony.id+":elena" to HeardClaim(
    directTestimony.id,"elena","erik",directTestimony.claim,80,8,
    InformationChannel.DIRECT_TESTIMONY,eventId
   )
  ) else emptyMap()
  return Story(
   "s","Nadir","p",turn=turn,
   world=WorldState(
    characters=mapOf(
     "elena" to Character(
      "elena","Elena",mind=CharacterMind(beliefs=listOf(rumorBelief))
     )
    ),
    hiddenEvents=listOf(HiddenEvent(
     eventId,4,"erik","A private repair occurred",
     knownBy=setOf("erik"),discoveredBy=if(discovered)setOf("elena") else emptySet()
    )),
    discovery=DiscoveryState(evidence=listOfNotNull(evidence).associateBy{it.id}),
    socialKnowledge=SocialKnowledgeState(
     testimonies=testimonies,
     heardClaims=claims
    )
   )
  )
 }

 @Test fun staleUnverifiedRumorBecomesDoubtedButNotRejected(){
  val (updated,cycle)=RumorBeliefDecayEngine().apply(story())
  val belief=updated.world.characters.getValue("elena").mind.beliefs.single()
  val revision=updated.world.characters.getValue("elena").mind.beliefRevisions.single()

  assertEquals(listOf(RumorBeliefDecay("elena",rumor.id)),cycle.decayed)
  assertEquals(BeliefStatus.DOUBTED,belief.status)
  assertEquals(40,belief.confidence)
  assertEquals(9,belief.lastRevisedTurn)
  assertNull(belief.supersededBySourceId)
  assertEquals(rumorBelief,revision.previous)
  assertEquals(belief,revision.revised)
  assertNull(revision.confirmed)
  assertNull(revision.sourceCharacterId)
 }
 @Test fun rumorDoesNotDecayBeforeTheWaitingPeriod(){
  val original=story(turn=8)
  val (same,cycle)=RumorBeliefDecayEngine().apply(original)

  assertSame(original,same)
  assertTrue(cycle.decayed.isEmpty())
  assertEquals(BeliefStatus.ACTIVE,same.world.characters.getValue("elena").mind.beliefs.single().status)
 }
 @Test fun decayIsAppliedOnlyOnce(){
  val engine=RumorBeliefDecayEngine()
  val (once,first)=engine.apply(story())
  val (same,second)=engine.apply(once)

  assertEquals(1,first.decayed.size)
  assertTrue(second.decayed.isEmpty())
  assertEquals(once,same)
  assertEquals(1,same.world.characters.getValue("elena").mind.beliefRevisions.size)
 }
 @Test fun testimonyFromAnUninformedSpeakerDoesNotGroundTheRumor(){
  val original=story(direct=true)
  val knowledge=original.world.socialKnowledge
  val direct=knowledge.testimonies.getValue("direct-1").copy(speakerId="nora")
  val heard=knowledge.heardClaims.getValue("direct-1:elena").copy(speakerId="nora")
  val ungrounded=original.copy(world=original.world.copy(
   socialKnowledge=knowledge.copy(
    testimonies=knowledge.testimonies+("direct-1" to direct),
    heardClaims=knowledge.heardClaims+("direct-1:elena" to heard)
   )
  ))

  val (updated,cycle)=RumorBeliefDecayEngine().apply(ungrounded)

  assertEquals(1,cycle.decayed.size)
  assertEquals(
   BeliefStatus.DOUBTED,
   updated.world.characters.getValue("elena").mind.beliefs.single().status
  )
 }
 @Test fun groundedRumorIsNotDowngradedByTime(){
  val ownedEvidence=Evidence(
   "e1","signal-1","Observed conduct",70,8,"elena",eventId
  )
  val variants=listOf(
   story(discovered=true),
   story(evidence=ownedEvidence),
   story(direct=true)
  )
  variants.forEach{original->
   val (same,cycle)=RumorBeliefDecayEngine().apply(original)
   assertSame(original,same)
   assertTrue(cycle.decayed.isEmpty())
  }
 }
}
