package com.historiasvivas.engine.character
import com.historiasvivas.engine.model.Character

class CharacterMindReducer {
 fun apply(character:Character,delta:MindDelta):Character{
  require(character.id==delta.characterId)
  val beliefs=character.mind.beliefs.associateBy{it.subject+"|"+it.claim}.toMutableMap()
  delta.beliefsToAdd.forEach{beliefs[it.subject+"|"+it.claim]=it}
  val emotional=character.mind.emotionalMemory.toMutableMap()
  delta.emotionalTracesToAdd.forEach{trace->emotional[trace.eventId]=trace}
  val revisions=(character.mind.beliefRevisions+delta.beliefRevisionsToAdd).distinctBy{it.id}
  return character.copy(mind=character.mind.copy(
   personality=character.mind.personality.shifted(delta.personalityShifts),beliefs=beliefs.values.toList(),
   personalMemories=character.mind.personalMemories+delta.memoriesToAdd,emotionalMemory=emotional,
   beliefRevisions=revisions
  ))
 }
 fun revise(character:Character,revision:EmotionalMemoryRevision):Character{
  require(character.id==revision.characterId)
  val old=character.mind.emotionalMemory[revision.eventId]?:return character
  val keys=old.emotions.keys+revision.emotionShifts.keys
  val emotions=keys.associateWith{((old.emotions[it]?:0)+(revision.emotionShifts[it]?:0)).coerceIn(-100,100)}
  val updated=old.copy(
   interpretation=revision.newInterpretation?:old.interpretation,emotions=emotions,
   salience=(old.salience+revision.salienceShift).coerceIn(0,100),
   unresolved=revision.resolved?.not()?:old.unresolved,revision=old.revision+1
  )
  return character.copy(mind=character.mind.copy(emotionalMemory=character.mind.emotionalMemory+(revision.eventId to updated)))
 }
 fun perceive(observer:Character,delta:PerceptionDelta):Character{
  require(observer.id==delta.observerId)
  val old=observer.mind.perceptions[delta.subjectId]?:Perception(observer.id,delta.subjectId)
  val keys=old.impressions.keys+delta.impressionShifts.keys
  val impressions=keys.associateWith{((old.impressions[it]?:0)+(delta.impressionShifts[it]?:0)).coerceIn(-100,100)}
  return observer.copy(mind=observer.mind.copy(perceptions=observer.mind.perceptions+(delta.subjectId to old.copy(impressions=impressions,notes=old.notes+delta.notesToAdd))))
 }
}
