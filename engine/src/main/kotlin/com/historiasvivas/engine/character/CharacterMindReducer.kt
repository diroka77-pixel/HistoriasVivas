package com.historiasvivas.engine.character
import com.historiasvivas.engine.model.Character

class CharacterMindReducer {
 fun apply(character:Character,delta:MindDelta):Character {
  require(character.id==delta.characterId)
  val existing=character.mind.beliefs.associateBy{it.subject+"|"+it.claim}.toMutableMap()
  delta.beliefsToAdd.forEach{existing[it.subject+"|"+it.claim]=it}
  return character.copy(mind=character.mind.copy(
   personality=character.mind.personality.shifted(delta.personalityShifts),
   beliefs=existing.values.toList(),
   personalMemories=character.mind.personalMemories+delta.memoriesToAdd
  ))
 }
 fun perceive(observer:Character,delta:PerceptionDelta):Character {
  require(observer.id==delta.observerId)
  val old=observer.mind.perceptions[delta.subjectId]?:Perception(observer.id,delta.subjectId)
  val keys=old.impressions.keys+delta.impressionShifts.keys
  val impressions=keys.associateWith{
   ((old.impressions[it]?:0)+(delta.impressionShifts[it]?:0)).coerceIn(-100,100)
  }
  val updated=old.copy(impressions=impressions,notes=old.notes+delta.notesToAdd)
  return observer.copy(mind=observer.mind.copy(perceptions=observer.mind.perceptions+(delta.subjectId to updated)))
 }
}
