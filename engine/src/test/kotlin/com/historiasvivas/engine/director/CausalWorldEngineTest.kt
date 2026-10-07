package com.historiasvivas.engine.director
import com.historiasvivas.engine.character.*
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.world.*
import kotlin.test.*
class CausalWorldEngineTest {
 @Test fun crisisBecomesRealityOnlyWhenCausesConverge(){
  val story=Story("s","Nadir","p",world=WorldState(environment=EnvironmentState(
   resources=mapOf("water" to ResourcePool("water",18,100,20)),
   pressures=mapOf("fauna" to WorldPressure("fauna",70,0,"Fauna near perimeter"))
  )),turn=10)
  val result=CausalWorldEngine().advance(story)
  assertEquals(1,result.publicEvents.size)
  assertTrue(result.consequences.any{it.description.contains("water")})
 }
 @Test fun privateSocialFractureDoesNotBecomePlayerKnowledge(){
  val memories=mapOf(
   "e1" to EmotionalTrace("e1","betrayal",mapOf("anger" to 80),90),
   "e2" to EmotionalTrace("e2","fear",mapOf("fear" to 70),80)
  )
  val erik=Character("erik","Erik",mind=CharacterMind(emotionalMemory=memories))
  val daniel=Character("daniel","Daniel")
  val story=Story("s","Nadir","p",world=WorldState(
   characters=mapOf("erik" to erik,"daniel" to daniel),
   relationships=listOf(Relationship("erik","daniel",trust=-60))
  ),turn=20)
  val result=CausalWorldEngine().advance(story)
  assertTrue(result.publicEvents.isEmpty())
  assertEquals(1,result.hiddenEvents.size)
  assertFalse("daniel" in result.hiddenEvents.first().knownBy)
 }
}