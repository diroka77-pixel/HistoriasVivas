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
}
