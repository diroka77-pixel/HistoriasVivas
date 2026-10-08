package com.historiasvivas.engine
import com.historiasvivas.engine.character.*
import com.historiasvivas.engine.discovery.Evidence
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.offscreen.*
import com.historiasvivas.engine.social.*
import kotlin.test.*

class OffscreenWorldTest {
 @Test fun protagonistDoesNotAutomaticallyKnowOffscreenEvents(){
  val event=HiddenEvent("h1",5,"erik","Erik entered the ruins")
  val gate=KnowledgeGate()
  assertTrue(gate.visible(listOf(event),"daniel").isEmpty())
  assertEquals(1,gate.visible(listOf(event),"erik").size)
 }
 @Test fun existingEvidenceOwnedByDiscovererCanRevealHiddenEvent(){
  val event=HiddenEvent("h1",5,"erik","Erik entered the ruins")
  val evidence=Evidence("evidence-blood-1","blood-trace","Blood near the entrance",70,7,"daniel",relatedEventId=event.id)
  val gate=KnowledgeGate()
  val updated=gate.discover(
   listOf(event),
   Discovery("h1","daniel",DiscoveryChannel.EVIDENCE,"Found Erik's blood near the entrance",sourceId=evidence.id),
   DiscoverySources(evidence=mapOf(evidence.id to evidence))
  )
  assertEquals(1,gate.visible(updated,"daniel").size)
 }
 @Test fun absentCharacterCanActOffscreen(){
  val erik=Character("erik","Erik",goals=listOf("Explore the ruins"))
  val daniel=Character("daniel","Daniel")
  val story=Story("s","Story","Premise",world=WorldState(characters=mapOf("erik" to erik,"daniel" to daniel)))
  val actions=OffscreenSimulator().plan(story,presentCharacterIds=setOf("daniel"))
  assertTrue(actions.any{it.intent.characterId=="erik"})
  assertTrue(actions.all{it.hiddenEvent.knownBy==setOf(it.intent.characterId)})
 }
 @Test fun unsupportedClaimCannotRevealHiddenEvent(){
  val event=HiddenEvent("h1",5,"erik","Erik entered the ruins")
  val unchanged=KnowledgeGate().discover(
   listOf(event),
   Discovery("h1","daniel",DiscoveryChannel.RUMOR,"Someone says Erik entered the ruins")
  )
  assertTrue(KnowledgeGate().visible(unchanged,"daniel").isEmpty())
 }
 @Test fun inventedSourceIdCannotRevealHiddenEvent(){
  val event=HiddenEvent("h1",5,"erik","Erik entered the ruins")
  val discovery=Discovery("h1","daniel",DiscoveryChannel.TOLD,"Elena told Daniel",sourceId="invented-testimony")
  val updated=KnowledgeGate().discover(listOf(event),discovery,DiscoverySources())
  assertTrue(KnowledgeGate().visible(updated,"daniel").isEmpty())
 }
 @Test fun sourceMustMatchTheDeclaredChannel(){
  val event=HiddenEvent("h1",5,"erik","Erik entered the ruins")
  val testimony=Testimony(
   id="t1",speakerId="elena",claim="Erik entered the ruins",subject="erik",turn=6,
   channel=InformationChannel.DIRECT_TESTIMONY,relatedEventId=event.id
  )
  val discovery=Discovery("h1","daniel",DiscoveryChannel.RUMOR,"A rumor reaches Daniel",sourceId=testimony.id)
  val updated=KnowledgeGate().discover(
   listOf(event),discovery,DiscoverySources(testimonies=mapOf(testimony.id to testimony))
  )
  assertTrue(KnowledgeGate().visible(updated,"daniel").isEmpty())
 }
 @Test fun registeredRumorCreatesBeliefButDoesNotRevealTruth(){
  val event=HiddenEvent("h1",5,"erik","Erik entered the ruins")
  val rumor=Testimony(
   id="rumor-1",speakerId="elena",claim="Erik may have entered the ruins",subject="erik",turn=6,
   channel=InformationChannel.RUMOR,intent=TestimonyIntent.UNCERTAIN,relatedEventId=event.id
  )
  val social=SocialInformationEngine()
  val heard=social.hear(rumor,"daniel",CharacterMind())
  val belief=social.asBelief(heard)
  val discovery=Discovery(event.id,"daniel",DiscoveryChannel.RUMOR,"Daniel hears the rumor",sourceId=rumor.id)
  val updated=KnowledgeGate().discover(
   listOf(event),discovery,DiscoverySources(
    testimonies=mapOf(rumor.id to rumor),
    heardClaims=mapOf(rumor.id+":"+heard.listenerId to heard)
   )
  )
  assertEquals(rumor.claim,belief.claim)
  assertTrue(KnowledgeGate().visible(updated,"daniel").isEmpty())
 }
 @Test fun honestReceivedTestimonyCanRevealLinkedTruth(){
  val event=HiddenEvent("h1",5,"erik","Erik entered the ruins")
  val testimony=Testimony(
   id="t-honest",speakerId="elena",claim=event.summary,subject="erik",turn=6,
   channel=InformationChannel.DIRECT_TESTIMONY,intent=TestimonyIntent.HONEST,relatedEventId=event.id
  )
  val heard=SocialInformationEngine().hear(testimony,"daniel",CharacterMind())
  val updated=KnowledgeGate().discover(
   listOf(event),
   Discovery(event.id,"daniel",DiscoveryChannel.TOLD,"Elena tells Daniel what happened",sourceId=testimony.id),
   DiscoverySources(
    testimonies=mapOf(testimony.id to testimony),
    heardClaims=mapOf(testimony.id+":"+heard.listenerId to heard)
   )
  )
  assertEquals(listOf(event.id),KnowledgeGate().visible(updated,"daniel").map{it.id})
 }
 @Test fun liesAndPartialAccountsCreateBeliefsWithoutRevealingTruth(){
  val event=HiddenEvent("h1",5,"erik","Erik entered the ruins")
  val social=SocialInformationEngine()
  listOf(TestimonyIntent.LYING,TestimonyIntent.WITHHOLDING).forEach{intent->
   val testimony=Testimony(
    id="t-"+intent.name.lowercase(),speakerId="elena",claim="Erik stayed near the settlement",subject="erik",turn=6,
    channel=InformationChannel.DIRECT_TESTIMONY,intent=intent,privateTruth=event.summary,relatedEventId=event.id
   )
   val heard=social.hear(testimony,"daniel",CharacterMind())
   val belief=social.asBelief(heard)
   val updated=KnowledgeGate().discover(
    listOf(event),
    Discovery(event.id,"daniel",DiscoveryChannel.TOLD,"Elena gives her account",sourceId=testimony.id),
    DiscoverySources(
     testimonies=mapOf(testimony.id to testimony),
     heardClaims=mapOf(testimony.id+":"+heard.listenerId to heard)
    )
   )
   assertEquals(testimony.claim,belief.claim)
   assertNotEquals(testimony.privateTruth,belief.claim)
   assertTrue(KnowledgeGate().visible(updated,"daniel").isEmpty())
  }
 }
 @Test fun existingButUnheardTestimonyCannotRevealHiddenEvent(){
  val event=HiddenEvent("h1",5,"erik","Erik entered the ruins")
  val testimony=Testimony(
   id="t1",speakerId="elena",claim="Erik entered the ruins",subject="erik",turn=6,
   channel=InformationChannel.DIRECT_TESTIMONY,relatedEventId=event.id
  )
  val discovery=Discovery(event.id,"daniel",DiscoveryChannel.TOLD,"Elena allegedly told Daniel",sourceId=testimony.id)
  val updated=KnowledgeGate().discover(
   listOf(event),discovery,DiscoverySources(testimonies=mapOf(testimony.id to testimony))
  )
  assertTrue(KnowledgeGate().visible(updated,"daniel").isEmpty())
 }
 @Test fun sourceLinkedToAnotherEventCannotRevealTarget(){
  val target=HiddenEvent("h-target",5,"erik","Erik entered the ruins")
  val unrelated=HiddenEvent("h-other",4,"elena","Elena repaired the radio")
  val testimony=Testimony(
   id="t-other",speakerId="elena",claim="The radio works",subject="radio",turn=6,
   channel=InformationChannel.DIRECT_TESTIMONY,relatedEventId=unrelated.id
  )
  val discovery=Discovery(target.id,"daniel",DiscoveryChannel.TOLD,"Elena told Daniel about Erik",sourceId=testimony.id)
  val updated=KnowledgeGate().discover(
   listOf(target,unrelated),discovery,DiscoverySources(testimonies=mapOf(testimony.id to testimony))
  )
  assertTrue(KnowledgeGate().visible(updated,"daniel").isEmpty())
 }
 @Test fun evidenceDiscoveredBySomeoneElseCannotRevealEvent(){
  val event=HiddenEvent("h1",5,"erik","Erik entered the ruins")
  val evidence=Evidence("e1","trace","Fresh tracks",60,6,"elena",relatedEventId=event.id)
  val discovery=Discovery("h1","daniel",DiscoveryChannel.INVESTIGATION,"Daniel infers Erik passed",sourceId=evidence.id)
  val updated=KnowledgeGate().discover(
   listOf(event),discovery,DiscoverySources(evidence=mapOf(evidence.id to evidence))
  )
  assertTrue(KnowledgeGate().visible(updated,"daniel").isEmpty())
 }
 @Test fun witnessedEventCanBeKnownWithoutIndirectSource(){
  val event=HiddenEvent(
   "h1",5,"erik","Erik entered the ruins",knownBy=setOf("erik","daniel")
  )
  val gate=KnowledgeGate()
  val updated=gate.discover(
   listOf(event),
   Discovery("h1","daniel",DiscoveryChannel.WITNESSED,"Daniel sees Erik cross the entrance")
  )
  assertEquals(1,gate.visible(updated,"daniel").size)
 }
 @Test fun witnessedEventRejectsFabricatedSource(){
  val event=HiddenEvent("h1",5,"erik","Erik entered the ruins")
  val discovery=Discovery("h1","daniel",DiscoveryChannel.WITNESSED,"Daniel sees Erik",sourceId="unneeded")
  val updated=KnowledgeGate().discover(listOf(event),discovery)
  assertTrue(KnowledgeGate().visible(updated,"daniel").isEmpty())
 }
}
