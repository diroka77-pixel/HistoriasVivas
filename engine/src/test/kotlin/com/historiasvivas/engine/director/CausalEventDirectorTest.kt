package com.historiasvivas.engine.director
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.world.*
import kotlin.test.*
class CausalEventDirectorTest {
 @Test fun oneWeakConditionDoesNotManufactureDrama(){
  val story=Story("s","Nadir","p",world=WorldState(environment=EnvironmentState(resources=mapOf("water" to ResourcePool("water",29,100,20)))))
  assertNull(CausalEventDirector().propose(story))
 }
 @Test fun convergingSocialCausesCanCreateEmergentConflict(){
  val traces=mapOf("a" to com.historiasvivas.engine.character.EmotionalTrace("a","betrayal",mapOf("anger" to 80),90),"b" to com.historiasvivas.engine.character.EmotionalTrace("b","fear",mapOf("fear" to 70),80))
  val erik=Character("erik","Erik",mind=com.historiasvivas.engine.character.CharacterMind(emotionalMemory=traces))
  val story=Story("s","Nadir","p",world=WorldState(characters=mapOf("erik" to erik),relationships=listOf(Relationship("erik","daniel",trust=-50))))
  val proposal=CausalEventDirector().propose(story)
  assertEquals(EmergentEventKind.SOCIAL_FRACTURE,proposal?.kind)
  assertTrue((proposal?.factors?.size?:0)>=2)
 }
 @Test fun cooldownPreventsConstantEscalation(){
  val traces=mapOf("a" to com.historiasvivas.engine.character.EmotionalTrace("a","betrayal",salience=90),"b" to com.historiasvivas.engine.character.EmotionalTrace("b","fear",salience=80))
  val erik=Character("erik","Erik",mind=com.historiasvivas.engine.character.CharacterMind(emotionalMemory=traces))
  val story=Story("s","Nadir","p",world=WorldState(characters=mapOf("erik" to erik),relationships=listOf(Relationship("erik","daniel",trust=-50))),turn=20)
  assertNull(CausalEventDirector().propose(story,lastEmergentTurn=19))
 }
}