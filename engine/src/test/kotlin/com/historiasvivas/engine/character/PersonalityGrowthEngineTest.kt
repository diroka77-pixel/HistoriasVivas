package com.historiasvivas.engine.character
import com.historiasvivas.engine.agency.*
import com.historiasvivas.engine.model.*
import kotlin.test.*
class PersonalityGrowthEngineTest {
 @Test fun repeatedHighSalienceFearBuildsCautionGradually(){
  val traces=(1..4).associate{"e$it" to EmotionalTrace("e$it","The forest nearly killed Erik",mapOf("fear" to 70),90)}
  val erik=Character("erik","Erik",mind=CharacterMind(personality=PersonalityProfile(mapOf("caution" to 0)),emotionalMemory=traces))
  val evolved=PersonalityGrowthEngine().evolve(erik)
  assertTrue((evolved.mind.personality.dimensions["caution"]?:0) in 1..3)
 }
 @Test fun personalityChangesAutonomousResponse(){
  val perception=Perception("erik","daniel",mapOf("dangerous" to 60,"trustworthy" to -50))
  val cautious=Character("erik","Erik",mind=CharacterMind(personality=PersonalityProfile(mapOf("caution" to 80,"courage" to -20)),perceptions=mapOf("daniel" to perception)))
  val bold=cautious.copy(id="bold",mind=cautious.mind.copy(personality=PersonalityProfile(mapOf("caution" to 0,"courage" to 80))))
  val intents=CharacterAgency().intents(Story("s","t","p",world=WorldState(characters=mapOf(cautious.id to cautious,bold.id to bold))))
  val cautiousWithdraw=intents.first{it.characterId=="erik"&&it.kind==AgencyKind.WITHDRAW}.priority
  val boldWithdraw=intents.first{it.characterId=="bold"&&it.kind==AgencyKind.WITHDRAW}.priority
  assertTrue(cautiousWithdraw>boldWithdraw)
 }
}
