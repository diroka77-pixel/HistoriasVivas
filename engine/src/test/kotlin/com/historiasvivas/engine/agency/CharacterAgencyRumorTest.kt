package com.historiasvivas.engine.agency

import com.historiasvivas.engine.character.CharacterMind
import com.historiasvivas.engine.character.PersonalityProfile
import com.historiasvivas.engine.discovery.DiscoveryState
import com.historiasvivas.engine.discovery.Evidence
import com.historiasvivas.engine.model.Character
import com.historiasvivas.engine.model.Story
import com.historiasvivas.engine.model.WorldState
import com.historiasvivas.engine.offscreen.HiddenEvent
import com.historiasvivas.engine.offscreen.OffscreenSimulator
import com.historiasvivas.engine.social.*
import kotlin.test.*

class CharacterAgencyRumorTest {
 private val eventId="hidden-repair-1"
 private val rumor=Testimony(
  "rumor-1","elena","Someone repaired a damaged relationship","erik",10,
  channel=InformationChannel.RUMOR,intent=TestimonyIntent.UNCERTAIN,
  reliability=60,relatedEventId=eventId
 )
 private fun story(
  confidence:Int=60,
  turn:Long=11,
  heardTurn:Long=10,
  additionalTestimonies:Map<String,Testimony> = emptyMap(),
  additionalClaims:Map<String,HeardClaim> = emptyMap(),
  evidence:Map<String,Evidence> = emptyMap(),
  discovered:Boolean=false
 )=Story(
  "s","Nadir","p",turn=turn,protagonistId="daniel",
  world=WorldState(
   characters=mapOf(
    "elena" to Character(
     "elena","Elena",
     mind=CharacterMind(personality=PersonalityProfile(mapOf("caution" to 40)))
    ),
    "daniel" to Character("daniel","Daniel")
   ),
   hiddenEvents=listOf(HiddenEvent(
    eventId,9,"erik","A private repair occurred",
    knownBy=setOf("erik"),discoveredBy=if(discovered)setOf("elena") else emptySet()
   )),
   discovery=DiscoveryState(evidence=evidence),
   socialKnowledge=SocialKnowledgeState(
    testimonies=mapOf(rumor.id to rumor)+additionalTestimonies,
    heardClaims=mapOf(
     rumor.id+":elena" to HeardClaim(
      rumor.id,"elena","nora",rumor.claim,confidence,heardTurn,
      InformationChannel.RUMOR,eventId
     )
    )+additionalClaims
   )
  )
 )

 @Test fun credibleRecentRumorCreatesOnlyAnInvestigativeIntent(){
  val intents=CharacterAgency().intents(story())

  val intent=intents.single()
  assertEquals("elena",intent.characterId)
  assertEquals(AgencyKind.INVESTIGATE,intent.kind)
  assertNull(intent.targetCharacterId)
  assertTrue(intent.priority>=65)
  assertFalse(intent.motivation.contains("repair",ignoreCase=true))
  assertFalse(intent.motivation.contains(rumor.claim,ignoreCase=true))
 }
 @Test fun weakOrStaleRumorsDoNotDriveAction(){
  assertTrue(CharacterAgency().intents(story(confidence=39)).isEmpty())
  assertTrue(CharacterAgency().intents(story(turn=15,heardTurn=10)).isEmpty())
 }
 @Test fun directHonestTestimonySuppressesRedundantRumorInvestigation(){
  val direct=Testimony(
   "direct-1","erik","I made a limited repair","elena",11,
   channel=InformationChannel.DIRECT_TESTIMONY,intent=TestimonyIntent.HONEST,
   relatedEventId=eventId
  )
  val heard=HeardClaim(
   direct.id,"elena","erik",direct.claim,80,11,
   InformationChannel.DIRECT_TESTIMONY,eventId
  )
  val grounded=story(
   additionalTestimonies=mapOf(direct.id to direct),
   additionalClaims=mapOf(direct.id+":elena" to heard)
  )

  assertTrue(CharacterAgency().intents(grounded).isEmpty())
 }
 @Test fun uninformedHonestSpeakerDoesNotSuppressInvestigation(){
  val direct=Testimony(
   "direct-uninformed","nora","A private repair happened","elena",11,
   channel=InformationChannel.DIRECT_TESTIMONY,intent=TestimonyIntent.HONEST,
   relatedEventId=eventId
  )
  val heard=HeardClaim(
   direct.id,"elena","nora",direct.claim,80,11,
   InformationChannel.DIRECT_TESTIMONY,eventId
  )
  val ungrounded=story(
   additionalTestimonies=mapOf(direct.id to direct),
   additionalClaims=mapOf(direct.id+":elena" to heard)
  )

  assertTrue(CharacterAgency().intents(ungrounded).any{
   it.characterId=="elena"&&it.kind==AgencyKind.INVESTIGATE
  })
 }
 @Test fun ownedEvidenceOrKnownEventSuppressesRumorInvestigation(){
  val evidence=Evidence(
   "e1","signal-1","Observed follow-through",70,11,"elena",eventId
  )
  assertTrue(CharacterAgency().intents(story(evidence=mapOf(evidence.id to evidence))).isEmpty())
  assertTrue(CharacterAgency().intents(story(discovered=true)).isEmpty())
 }
 @Test fun offscreenSimulationCanTurnRumorCuriosityIntoPrivateAction(){
  val actions=OffscreenSimulator().plan(story(),presentCharacterIds=setOf("daniel"),maxActions=1)

  val action=actions.single()
  assertEquals("elena",action.intent.characterId)
  assertEquals(AgencyKind.INVESTIGATE,action.intent.kind)
  assertEquals("elena",action.hiddenEvent.actorId)
  assertTrue(action.hiddenEvent.summary.contains("independent confirmation"))
  assertEquals(setOf("elena"),action.hiddenEvent.knownBy)
 }
}
