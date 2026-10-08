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
   "b" to Perception("listener","b",impressions=mapOf("trust" to 90)),
   "c" to Perception("listener","c",impressions=mapOf("trust" to 90))
  ))
  val first=HeardClaim(
   "source","b","a","There is something beneath the ruins",70,1,
   InformationChannel.DIRECT_TESTIMONY,subject="ruins"
  )
  val bToC=rumor.relay(first,"b","c",2)
  val heardByC=engine.hear(bToC,"c",trustingMind)
  assertTrue(heardByC.confidence<=55)
  val cToD=rumor.relay(heardByC,"c","d",3)
  val heardByD=engine.hear(cToD,"d",trustingMind)
  assertTrue(heardByD.confidence<=40)
  assertTrue(heardByD.confidence<heardByC.confidence)
  assertEquals("ruins",bToC.subject)
  assertEquals("ruins",heardByC.subject)
  assertEquals("ruins",cToD.subject)
  assertEquals("ruins",heardByD.subject)
  assertEquals("ruins",engine.asBelief(heardByD).subject)
  assertNull(bToC.privateTruth)
  assertNull(cToD.privateTruth)
 }

 @Test fun heardClaimKeepsTheTestimonySubjectInBelief(){
  val engine=SocialInformationEngine()
  val testimony=Testimony("subject-1","elena","The ruins are unsafe","ruins",4)
  val heard=engine.hear(testimony,"daniel",CharacterMind())

  assertEquals("ruins",heard.subject)
  assertEquals("ruins",engine.asBelief(heard).subject)
 }
 @Test fun legacyHeardClaimKeepsItsPreviousBeliefKey(){
  val legacy=HeardClaim(
   "legacy","daniel","elena","Something happened",30,4,InformationChannel.RUMOR
  )

  assertNull(legacy.subject)
  assertEquals("elena:legacy",SocialInformationEngine().asBelief(legacy).subject)
 }

 @Test fun honestDirectTestimonySupersedesMatchingRumorWithoutErasingHistory(){
  val engine=SocialInformationEngine()
  val rumor=Testimony(
   "rumor-1","elena","Erik repaired the relationship","erik",4,
   channel=InformationChannel.RUMOR,intent=TestimonyIntent.UNCERTAIN,
   reliability=50,relatedEventId="hidden-repair"
  )
  val direct=Testimony(
   "direct-1","erik","Erik repaired the relationship","erik",6,
   channel=InformationChannel.DIRECT_TESTIMONY,intent=TestimonyIntent.HONEST,
   reliability=80,relatedEventId="hidden-repair"
  )
  val sources=mapOf(rumor.id to rumor,direct.id to direct)
  val afterRumor=engine.integrateBelief(
   CharacterMind(),engine.hear(rumor,"daniel",CharacterMind()),sources,4
  )
  val finalMind=engine.integrateBelief(
   afterRumor,engine.hear(direct,"daniel",afterRumor),sources,6
  )

  val oldBelief=finalMind.beliefs.single{it.sourceEventId==rumor.id}
  val confirmed=finalMind.beliefs.single{it.sourceEventId==direct.id}
  assertEquals(BeliefStatus.DOUBTED,oldBelief.status)
  assertEquals(direct.id,oldBelief.supersededBySourceId)
  assertEquals(6,oldBelief.lastRevisedTurn)
  val revision=finalMind.beliefRevisions.single()
  assertEquals(rumor.id,revision.previous.sourceEventId)
  assertEquals(oldBelief,revision.revised)
  assertEquals(confirmed,revision.confirmed)
  assertEquals("erik",revision.sourceCharacterId)
 }
 @Test fun conflictingDirectAccountContestsRatherThanSilentlyDeletingRumor(){
  val engine=SocialInformationEngine()
  val rumor=Testimony(
   "rumor-conflict","elena","The apology was accepted","erik",4,
   channel=InformationChannel.RUMOR,intent=TestimonyIntent.UNCERTAIN,
   relatedEventId="hidden-repair"
  )
  val direct=Testimony(
   "direct-conflict","erik","The apology was rejected","erik",6,
   channel=InformationChannel.DIRECT_TESTIMONY,intent=TestimonyIntent.HONEST,
   relatedEventId="hidden-repair"
  )
  val sources=mapOf(rumor.id to rumor,direct.id to direct)
  val rumorMind=engine.integrateBelief(
   CharacterMind(),engine.hear(rumor,"daniel",CharacterMind()),sources,4
  )
  val corrected=engine.integrateBelief(
   rumorMind,engine.hear(direct,"daniel",rumorMind),sources,6
  )

  assertEquals(
   BeliefStatus.CONTESTED,
   corrected.beliefs.single{it.sourceEventId==rumor.id}.status
  )
  assertEquals(1,corrected.beliefRevisions.size)
  assertEquals(2,corrected.beliefs.size)
 }
 @Test fun dishonestOrRumoredFollowUpCannotSupersedePriorBelief(){
  val engine=SocialInformationEngine()
  val first=Testimony(
   "rumor-base","elena","A repair happened","erik",4,
   channel=InformationChannel.RUMOR,intent=TestimonyIntent.UNCERTAIN,
   relatedEventId="hidden-repair"
  )
  val lie=Testimony(
   "lie-follow-up","erik","Everything is resolved","erik",6,
   channel=InformationChannel.DIRECT_TESTIMONY,intent=TestimonyIntent.LYING,
   relatedEventId="hidden-repair"
  )
  val sources=mapOf(first.id to first,lie.id to lie)
  val firstMind=engine.integrateBelief(
   CharacterMind(),engine.hear(first,"daniel",CharacterMind()),sources,4
  )
  val afterLie=engine.integrateBelief(
   firstMind,engine.hear(lie,"daniel",firstMind),sources,6
  )

  assertEquals(BeliefStatus.ACTIVE,afterLie.beliefs.single{it.sourceEventId==first.id}.status)
  assertTrue(afterLie.beliefRevisions.isEmpty())
  assertEquals(2,afterLie.beliefs.size)
 }
 @Test fun integratingTheSameSourceTwiceIsIdempotent(){
  val engine=SocialInformationEngine()
  val direct=Testimony(
   "direct-once","erik","A repair happened","erik",6,
   relatedEventId="hidden-repair"
  )
  val heard=engine.hear(direct,"daniel",CharacterMind())
  val once=engine.integrateBelief(CharacterMind(),heard,mapOf(direct.id to direct),6)
  val twice=engine.integrateBelief(once,heard,mapOf(direct.id to direct),7)

  assertEquals(once,twice)
  assertEquals(1,twice.beliefs.size)
 }

}
