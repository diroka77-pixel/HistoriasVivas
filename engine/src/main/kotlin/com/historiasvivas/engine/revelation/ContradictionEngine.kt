package com.historiasvivas.engine.revelation
import kotlinx.serialization.Serializable
import com.historiasvivas.engine.character.*
import com.historiasvivas.engine.discovery.Evidence

@Serializable enum class ContradictionOutcome { DOUBT, CONFLICT, REVISE_BELIEF, REVELATION }
@Serializable data class Contradiction(
 val id:String,val characterId:String,val belief:Belief,val evidenceId:String,
 val contradictionStrength:Int,val outcome:ContradictionOutcome,val turn:Long,val sourceCharacterId:String?=null
)

class ContradictionEngine {
 fun evaluate(characterId:String,belief:Belief,evidence:Evidence,contradictionStrength:Int,turn:Long,sourceCharacterId:String?=null):Contradiction{
  val pressure=(evidence.strength+contradictionStrength-belief.confidence/2).coerceIn(0,150)
  val outcome=when{
   pressure>=100->ContradictionOutcome.REVELATION
   pressure>=75->ContradictionOutcome.REVISE_BELIEF
   pressure>=50->ContradictionOutcome.CONFLICT
   else->ContradictionOutcome.DOUBT
  }
  return Contradiction("contradiction-"+characterId+"-"+evidence.id,characterId,belief,evidence.id,contradictionStrength.coerceIn(0,100),outcome,turn,sourceCharacterId)
 }
 fun beliefAfter(contradiction:Contradiction):Belief{
  val loss=when(contradiction.outcome){
   ContradictionOutcome.DOUBT->10; ContradictionOutcome.CONFLICT->25; ContradictionOutcome.REVISE_BELIEF->45; ContradictionOutcome.REVELATION->70
  }
  val status=when(contradiction.outcome){
   ContradictionOutcome.DOUBT->BeliefStatus.DOUBTED
   ContradictionOutcome.CONFLICT->BeliefStatus.CONTESTED
   ContradictionOutcome.REVISE_BELIEF,ContradictionOutcome.REVELATION->BeliefStatus.REJECTED
  }
  return contradiction.belief.copy(
   confidence=(contradiction.belief.confidence-loss).coerceAtLeast(0),
   status=status,
   lastRevisedTurn=contradiction.turn,
   supersededBySourceId=if(contradiction.outcome==ContradictionOutcome.REVELATION)contradiction.evidenceId else null
  )
 }
 fun revisionAfter(contradiction:Contradiction,confirmedClaim:String?=null):BeliefRevision{
  val revised=beliefAfter(contradiction)
  val confirmed=confirmedClaim?.takeIf{
   contradiction.outcome==ContradictionOutcome.REVELATION && it.isNotBlank()
  }?.let{
   Belief(
    subject=contradiction.belief.subject,
    claim=it,
    confidence=(70+contradiction.contradictionStrength/4).coerceAtMost(95),
    sourceEventId=contradiction.evidenceId,
    status=BeliefStatus.ACTIVE,
    lastRevisedTurn=contradiction.turn
   )
  }
  return BeliefRevision(
   id=contradiction.id+"-revision",
   previous=contradiction.belief,
   revised=revised,
   confirmed=confirmed,
   evidenceId=contradiction.evidenceId,
   sourceCharacterId=contradiction.sourceCharacterId,
   turn=contradiction.turn
  )
 }
}
