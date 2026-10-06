package com.historiasvivas.engine
import com.historiasvivas.engine.character.*
import com.historiasvivas.engine.model.Character
import kotlin.test.*

class CharacterMindTest {
 @Test fun repeatedBehaviourCanShiftPersonality(){
  val c=Character("daniel","Daniel",mind=CharacterMind(personality=PersonalityProfile(mapOf("compassion" to 60))))
  val changed=CharacterMindReducer().apply(c,MindDelta("daniel",personalityShifts=mapOf("compassion" to -15,"ruthlessness" to 10),memoriesToAdd=setOf("Abandoned an injured companion")))
  assertEquals(45,changed.mind.personality.dimensions["compassion"])
  assertEquals(10,changed.mind.personality.dimensions["ruthlessness"])
  assertTrue("Abandoned an injured companion" in changed.mind.personalMemories)
 }
 @Test fun anotherCharacterCanPerceiveTheChangeDifferently(){
  val elena=Character("elena","Elena")
  val changed=CharacterMindReducer().perceive(elena,PerceptionDelta("elena","daniel",mapOf("dangerous" to 25,"trustworthy" to -20),setOf("He left Erik behind")))
  assertEquals(25,changed.mind.perceptions["daniel"]?.impressions?.get("dangerous"))
  assertEquals(-20,changed.mind.perceptions["daniel"]?.impressions?.get("trustworthy"))
 }
 @Test fun beliefsNeedNotEqualWorldTruth(){
  val c=Character("daniel","Daniel")
  val changed=CharacterMindReducer().apply(c,MindDelta("daniel",beliefsToAdd=listOf(Belief("humanity","We are the last humans",80))))
  assertEquals("We are the last humans",changed.mind.beliefs.single().claim)
 }
}
