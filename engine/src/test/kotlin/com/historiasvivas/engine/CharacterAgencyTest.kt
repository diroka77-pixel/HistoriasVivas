package com.historiasvivas.engine
import com.historiasvivas.engine.agency.*
import com.historiasvivas.engine.character.*
import com.historiasvivas.engine.model.*
import kotlin.test.*

class CharacterAgencyTest {
 @Test fun characterCanConfrontWithoutPlayerOrderingIt(){
  val elena=Character("elena","Elena",goals=listOf("Keep the colony safe"),mind=CharacterMind(
   perceptions=mapOf("daniel" to Perception("elena","daniel",mapOf("trustworthy" to -60,"dangerous" to 40)))
  ))
  val daniel=Character("daniel","Daniel")
  val story=Story("s","Story","Premise",world=WorldState(characters=mapOf("elena" to elena,"daniel" to daniel)))
  val intents=CharacterAgency().intents(story)
  assertTrue(intents.any{it.characterId=="elena"&&it.kind==AgencyKind.CONFRONT&&it.targetCharacterId=="daniel"})
  assertTrue(intents.any{it.characterId=="elena"&&it.kind==AgencyKind.WITHDRAW})
 }
 @Test fun equalPriorityIntentionsHaveStableCharacterOrder(){
  val zara=Character("zara","Zara",goals=listOf("Explore"))
  val alba=Character("alba","Alba",goals=listOf("Explore"))
  val story=Story(
   "s","Story","Premise",
   world=WorldState(characters=linkedMapOf("zara" to zara,"alba" to alba))
  )

  assertEquals(
   listOf("alba","zara"),
   CharacterAgency().intents(story).map{it.characterId}
  )
 }
 @Test fun deadCharactersDoNotGenerateIntentions(){
  val dead=Character("erik","Erik",goals=listOf("Find Daniel"),alive=false)
  val story=Story("s","Story","Premise",world=WorldState(characters=mapOf("erik" to dead)))
  assertTrue(CharacterAgency().intents(story).isEmpty())
 }
 @Test fun strongMysteryBeliefCanTriggerInvestigation(){
  val erik=Character("erik","Erik",mind=CharacterMind(beliefs=listOf(Belief("ruins","The ruins hide an unknown origin",85))))
  val story=Story("s","Story","Premise",world=WorldState(characters=mapOf("erik" to erik)))
  assertTrue(CharacterAgency().intents(story).any{it.kind==AgencyKind.INVESTIGATE})
 }
 @Test fun rememberedDeceptionProducesPersonalitySpecificResponses(){
  fun character(id:String,personality:Map<String,Int>)=Character(id,id, mind=CharacterMind(
   personality=PersonalityProfile(personality),
   beliefRevisions=listOf(BeliefRevision(
    id="revision-"+id,
    previous=Belief("signal","There is no signal",70,"lie"),
    revised=Belief("signal","There is no signal",0,"lie",status=BeliefStatus.REJECTED),
    confirmed=Belief("signal","The signal exists",90,"evidence"),
    evidenceId="evidence",
    sourceCharacterId="liar",
    turn=10
   ))
  ))
  fun response(character:Character)=CharacterAgency().intents(
   Story("s","Story","Premise",world=WorldState(characters=mapOf(character.id to character,"liar" to Character("liar","Liar"))),turn=10)
  ).first{it.targetCharacterId=="liar"}

  assertEquals(AgencyKind.CONFRONT,response(character("brave",mapOf("courage" to 70,"patience" to 10))).kind)
  assertEquals(AgencyKind.WITHDRAW,response(character("cautious",mapOf("caution" to 80,"courage" to 10))).kind)
  assertEquals(AgencyKind.INVESTIGATE,response(character("secretive",mapOf("secrecy" to 85,"courage" to 70))).kind)
 }
 @Test fun rejectedMysteryBeliefNoLongerDrivesInvestigation(){
  val rejected=Belief("ruins","The ruins hide an unknown origin",85,status=BeliefStatus.REJECTED)
  val erik=Character("erik","Erik",mind=CharacterMind(beliefs=listOf(rejected)))
  val story=Story("s","Story","Premise",world=WorldState(characters=mapOf("erik" to erik)))
  assertTrue(CharacterAgency().intents(story).none{it.characterId=="erik"&&it.kind==AgencyKind.INVESTIGATE})
 }
 @Test fun oldDeceptionDoesNotRepeatButNewRevisionCanReactivateResponse(){
  fun revision(id:String,turn:Long)=BeliefRevision(
   id=id,
   previous=Belief("signal","There is no signal",70,"lie"),
   revised=Belief("signal","There is no signal",0,"lie",status=BeliefStatus.REJECTED),
   confirmed=Belief("signal","The signal exists",90,"evidence-"+id),
   evidenceId="evidence-"+id,
   sourceCharacterId="liar",
   turn=turn
  )
  val base=Character(
   "elena","Elena",
   mind=CharacterMind(
    personality=PersonalityProfile(mapOf("courage" to 70)),
    beliefRevisions=listOf(revision("old",10))
   )
  )
  fun responses(character:Character,turn:Long)=CharacterAgency().intents(
   Story("s","Story","Premise",world=WorldState(characters=mapOf("elena" to character,"liar" to Character("liar","Liar"))),turn=turn)
  ).filter{it.targetCharacterId=="liar"}

  assertTrue(responses(base,10).any{it.kind==AgencyKind.CONFRONT})
  assertTrue(responses(base,11).isEmpty())
  val deceivedAgain=base.copy(mind=base.mind.copy(beliefRevisions=base.mind.beliefRevisions+revision("new",11)))
  assertTrue(responses(deceivedAgain,11).any{it.kind==AgencyKind.CONFRONT})
 }
}
