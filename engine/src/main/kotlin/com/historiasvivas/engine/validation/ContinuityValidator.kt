package com.historiasvivas.engine.validation
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.turn.WorldDelta

data class ValidationIssue(val code:String,val message:String)
data class ValidationResult(val accepted:Boolean,val issues:List<ValidationIssue>)

class ContinuityValidator(private val maxPersonalityShiftPerTurn:Int=15) {
 fun validate(story:Story,delta:WorldDelta):ValidationResult {
  val issues=mutableListOf<ValidationIssue>()
  val known=story.world.characters.keys
  val unknownParticipants=delta.event.participants-known
  if(unknownParticipants.isNotEmpty())
   issues+=ValidationIssue("UNKNOWN_CHARACTER","Unknown participants: "+unknownParticipants.joinToString())
  if(delta.consequencesToResolve.any{id->story.world.pendingConsequences.none{it.id==id}})
   issues+=ValidationIssue("UNKNOWN_CONSEQUENCE","Cannot resolve a consequence that does not exist")
  if(delta.consequencesToSchedule.any{it.earliestTurn<=story.turn})
   issues+=ValidationIssue("INVALID_DELAY","A delayed consequence must target a future turn")
  val duplicateIds=delta.consequencesToSchedule.groupBy{it.id}.filterValues{it.size>1}.keys
  if(duplicateIds.isNotEmpty())
   issues+=ValidationIssue("DUPLICATE_CONSEQUENCE","Duplicate consequence ids: "+duplicateIds.joinToString())
  if(delta.mindDeltas.any{it.characterId !in known})
   issues+=ValidationIssue("UNKNOWN_MIND","Cannot alter the mind of an unknown character")
  if(delta.perceptionDeltas.any{it.observerId !in known || it.subjectId !in known})
   issues+=ValidationIssue("UNKNOWN_PERCEPTION","Perceptions require known observer and subject")
  if(delta.mindDeltas.any{d->d.personalityShifts.values.any{kotlin.math.abs(it)>maxPersonalityShiftPerTurn}})
   issues+=ValidationIssue("PERSONALITY_JUMP","Personality changes must accumulate gradually")
  if(delta.perceptionDeltas.any{d->d.impressionShifts.values.any{kotlin.math.abs(it)>25}})
   issues+=ValidationIssue("PERCEPTION_JUMP","A single event cannot rewrite an entire relationship")

  val hidden=delta.hiddenEventsToAdd
  val hiddenUnknown=hidden.flatMap{
   setOf(it.actorId)+it.knownBy+it.discoveredBy
  }.toSet()-known
  if(hiddenUnknown.isNotEmpty())
   issues+=ValidationIssue("UNKNOWN_HIDDEN_CHARACTER","Hidden events reference unknown characters: "+hiddenUnknown.sorted().joinToString())
  if(hidden.any{it.turn!=story.turn+1})
   issues+=ValidationIssue("INVALID_HIDDEN_TURN","Hidden events must occur on the advancing turn")
  if(hidden.any{it.actorId !in it.knownBy})
   issues+=ValidationIssue("HIDDEN_ACTOR_UNAWARE","A hidden event actor must know their own event")
  val duplicateHidden=hidden.groupBy{it.id}.filterValues{it.size>1}.keys
  if(duplicateHidden.isNotEmpty())
   issues+=ValidationIssue("DUPLICATE_HIDDEN_EVENT","Duplicate hidden event ids: "+duplicateHidden.sorted().joinToString())
  val existingHidden=story.world.hiddenEvents.map{it.id}.toSet()
  val collidingHidden=hidden.map{it.id}.filter{it in existingHidden}.toSet()
  if(collidingHidden.isNotEmpty())
   issues+=ValidationIssue("EXISTING_HIDDEN_EVENT","Hidden event ids already exist: "+collidingHidden.sorted().joinToString())
  if(hidden.any{it.id.isBlank()||it.summary.isBlank()})
   issues+=ValidationIssue("INVALID_HIDDEN_CONTENT","Hidden events require a non-blank id and summary")

  return ValidationResult(issues.isEmpty(),issues)
 }
}
