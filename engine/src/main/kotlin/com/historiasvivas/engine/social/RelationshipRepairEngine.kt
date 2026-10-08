package com.historiasvivas.engine.social

import kotlinx.serialization.Serializable
import com.historiasvivas.engine.character.CharacterMindReducer
import com.historiasvivas.engine.character.PerceptionDelta
import com.historiasvivas.engine.model.Relationship
import com.historiasvivas.engine.model.Story

@Serializable enum class RepairAct { ADMISSION, APOLOGY, RESTITUTION }

@Serializable data class RelationshipRepairAttempt(
 val id:String,
 val offenderId:String,
 val harmedId:String,
 val beliefRevisionId:String,
 val acts:Set<RepairAct>,
 val sincerity:Int,
 val turn:Long
)

@Serializable data class RelationshipRepairOutcome(
 val attemptId:String,
 val offenderId:String,
 val harmedId:String,
 val beliefRevisionId:String,
 val accepted:Boolean,
 val trustChange:Int,
 val affectionChange:Int,
 val fearChange:Int,
 val remainingDamage:Int,
 val turn:Long,
 val note:String
)

data class RelationshipRepairResult(val story:Story,val outcome:RelationshipRepairOutcome)

class RelationshipRepairEngine(
 private val minds:CharacterMindReducer=CharacterMindReducer()
){
 fun apply(story:Story,attempt:RelationshipRepairAttempt):RelationshipRepairResult?{
  story.world.socialKnowledge.relationshipRepairs[attempt.id]?.let{
   return RelationshipRepairResult(story,it)
  }
  val harmed=story.world.characters[attempt.harmedId]?:return null
  if(story.world.characters[attempt.offenderId]?.alive!=true||!harmed.alive)return null
  val revision=harmed.mind.beliefRevisions.firstOrNull{
   it.id==attempt.beliefRevisionId&&it.sourceCharacterId==attempt.offenderId&&it.confirmed!=null
  }?:return null
  if(attempt.turn<revision.turn)return null

  val sincerity=attempt.sincerity.coerceIn(0,100)
  val admission=RepairAct.ADMISSION in attempt.acts
  val apology=RepairAct.APOLOGY in attempt.acts
  val restitution=RepairAct.RESTITUTION in attempt.acts
  val personality=harmed.mind.personality.dimensions
  val compassion=personality["compassion"]?:0
  val patience=personality["patience"]?:0
  val caution=personality["caution"]?:0
  val harmSeverity=((revision.previous.confidence+(revision.confirmed?.confidence?:0))/4).coerceIn(10,50)
  val repairEffort=(if(admission)15 else 0)+(if(apology)10 else 0)+(if(restitution)25 else 0)
  val score=repairEffort+sincerity/5+compassion/6+patience/8-caution/8-harmSeverity
  val accepted=admission&&sincerity>=40&&score>=20
  val trustChange=when{
   accepted->(score/4).coerceIn(3,25)
   apology&&!admission->-5
   else->0
  }
  val affectionChange=when{
   accepted&&restitution->8
   accepted&&apology->3
   else->0
  }
  val fearChange=if(accepted&&restitution)-5 else 0
  val remainingDamage=(100-(if(accepted)repairEffort+sincerity/4 else 0)).coerceIn(20,100)
  val note=if(accepted)
   "Accepted a limited repair attempt from "+attempt.offenderId+"; trust remains incomplete"
  else
   "Did not accept the repair attempt from "+attempt.offenderId

  val oldRelationship=story.world.relationships.firstOrNull{
   it.fromCharacterId==attempt.harmedId&&it.toCharacterId==attempt.offenderId
  }?:Relationship(attempt.harmedId,attempt.offenderId)
  val updatedRelationship=oldRelationship.copy(
   trust=(oldRelationship.trust+trustChange).coerceIn(-100,100),
   affection=(oldRelationship.affection+affectionChange).coerceIn(-100,100),
   fear=(oldRelationship.fear+fearChange).coerceIn(0,100),
   notes=oldRelationship.notes+note
  )
  val relationships=story.world.relationships.filterNot{
   it.fromCharacterId==attempt.harmedId&&it.toCharacterId==attempt.offenderId
  }+updatedRelationship
  val updatedHarmed=minds.perceive(
   harmed,
   PerceptionDelta(
    attempt.harmedId,
    attempt.offenderId,
    impressionShifts=mapOf("trust" to trustChange),
    notesToAdd=setOf(note)
   )
  ).copy(mind=harmed.mind.copy(
   perceptions=minds.perceive(
    harmed,
    PerceptionDelta(
     attempt.harmedId,
     attempt.offenderId,
     impressionShifts=mapOf("trust" to trustChange),
     notesToAdd=setOf(note)
    )
   ).mind.perceptions,
   personalMemories=harmed.mind.personalMemories+note
  ))
  val outcome=RelationshipRepairOutcome(
   attempt.id,attempt.offenderId,attempt.harmedId,attempt.beliefRevisionId,accepted,
   trustChange,affectionChange,fearChange,remainingDamage,attempt.turn,note
  )
  val social=story.world.socialKnowledge.copy(
   relationshipRepairs=story.world.socialKnowledge.relationshipRepairs+(attempt.id to outcome)
  )
  val updated=story.copy(world=story.world.copy(
   characters=story.world.characters+(attempt.harmedId to updatedHarmed),
   relationships=relationships,
   socialKnowledge=social
  ))
  return RelationshipRepairResult(updated,outcome)
 }
}
