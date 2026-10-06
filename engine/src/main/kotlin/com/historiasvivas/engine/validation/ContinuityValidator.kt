package com.historiasvivas.engine.validation
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.turn.WorldDelta

data class ValidationIssue(val code:String,val message:String)
data class ValidationResult(val accepted:Boolean,val issues:List<ValidationIssue>)

class ContinuityValidator {
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
  return ValidationResult(issues.isEmpty(),issues)
 }
}
