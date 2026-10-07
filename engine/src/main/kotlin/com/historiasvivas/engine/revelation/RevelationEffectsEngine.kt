package com.historiasvivas.engine.revelation
import kotlinx.serialization.Serializable
import com.historiasvivas.engine.character.*

@Serializable data class RevelationState(val contradictions:Map<String,Contradiction> = emptyMap())
data class RevelationEffects(
 val updatedBelief:Belief,
 val perceptionDelta:PerceptionDelta?=null,
 val emotionalTrace:EmotionalTrace?=null
)

class RevelationEffectsEngine(private val contradictions:ContradictionEngine=ContradictionEngine()){
 fun effects(c:Contradiction):RevelationEffects{
  val belief=contradictions.beliefAfter(c)
  val trustLoss=when(c.outcome){
   ContradictionOutcome.DOUBT->-3; ContradictionOutcome.CONFLICT->-10; ContradictionOutcome.REVISE_BELIEF->-20; ContradictionOutcome.REVELATION->-35
  }
  val perception=c.sourceCharacterId?.let{PerceptionDelta(c.characterId,it,mapOf("trust" to trustLoss),setOf("Contradicted by evidence "+c.evidenceId))}
  val emotion=EmotionalTrace(
   eventId=c.id,interpretation="New evidence conflicts with a previous belief",
   emotions=mapOf("doubt" to (20-trustLoss).coerceAtMost(70)),
   salience=when(c.outcome){ContradictionOutcome.REVELATION->90;ContradictionOutcome.REVISE_BELIEF->70;ContradictionOutcome.CONFLICT->55;ContradictionOutcome.DOUBT->35},
   unresolved=c.outcome!=ContradictionOutcome.REVELATION
  )
  return RevelationEffects(belief,perception,emotion)
 }
}