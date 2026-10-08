package com.historiasvivas.engine.social

import com.historiasvivas.engine.model.Story
import com.historiasvivas.engine.offscreen.HiddenEvent
import com.historiasvivas.engine.spatial.SpatialEngine

data class RelationshipRepairCycle(
 val attempts:List<RelationshipRepairAttempt> = emptyList(),
 val outcomes:List<RelationshipRepairOutcome> = emptyList()
)

class RelationshipRepairPlanner(
 private val spatial:SpatialEngine=SpatialEngine(),
 private val retryCooldownTurns:Long=5
){
 fun plan(story:Story,offenderId:String):RelationshipRepairAttempt?{
  val offender=story.world.characters[offenderId]?:return null
  if(!offender.alive)return null
  val offenderLocation=spatial.locationOf(story.world.spatial,offenderId)?:return null
  val candidates=story.world.characters.values.asSequence()
   .filter{it.alive&&it.id!=offenderId&&spatial.locationOf(story.world.spatial,it.id)==offenderLocation}
   .flatMap{harmed->harmed.mind.beliefRevisions.asSequence().map{harmed to it}}
   .filter{(_,revision)->revision.sourceCharacterId==offenderId&&revision.confirmed!=null}
   .filter{(_,revision)->
    val prior=story.world.socialKnowledge.relationshipRepairs.values.filter{it.beliefRevisionId==revision.id}
    prior.none{it.accepted}&&prior.none{story.turn-it.turn<retryCooldownTurns}
   }
   .sortedWith(compareByDescending<Pair<com.historiasvivas.engine.model.Character,com.historiasvivas.engine.character.BeliefRevision>>{it.second.previous.confidence}.thenBy{it.first.id})
   .toList()
  val (harmed,revision)=candidates.firstOrNull()?:return null

  val p=offender.mind.personality.dimensions
  val honesty=p["honesty"]?:0
  val compassion=p["compassion"]?:0
  val responsibility=p["responsibility"]?:0
  val pride=p["pride"]?:0
  val manipulation=p["manipulation"]?:0
  val relationship=story.world.relationships.firstOrNull{
   it.fromCharacterId==offenderId&&it.toCharacterId==harmed.id
  }
  val affection=relationship?.affection?:0
  val acts=buildSet{
   if(honesty>=0)add(RepairAct.ADMISSION)
   if(compassion>=20||affection>=30)add(RepairAct.APOLOGY)
   if(responsibility>=50||honesty>=60||compassion>=70)add(RepairAct.RESTITUTION)
  }
  if(acts.isEmpty())return null
  val sincerity=(50+honesty/3+compassion/4+responsibility/5-pride/4-manipulation/3).coerceIn(0,100)
  return RelationshipRepairAttempt(
   id="repair-"+offenderId+"-"+harmed.id+"-"+revision.id+"-"+story.turn,
   offenderId=offenderId,
   harmedId=harmed.id,
   beliefRevisionId=revision.id,
   acts=acts,
   sincerity=sincerity,
   turn=story.turn
  )
 }
}

class RelationshipRepairScheduler(
 private val planner:RelationshipRepairPlanner=RelationshipRepairPlanner(),
 private val repair:RelationshipRepairEngine=RelationshipRepairEngine()
){
 fun run(story:Story,presentCharacterIds:Set<String>,maxAttempts:Int=1):Pair<Story,RelationshipRepairCycle>{
  if(maxAttempts<=0)return story to RelationshipRepairCycle()
  var current=story
  val attempts=mutableListOf<RelationshipRepairAttempt>()
  val outcomes=mutableListOf<RelationshipRepairOutcome>()
  current.world.characters.values.asSequence()
   .filter{it.alive&&it.id !in presentCharacterIds}
   .sortedBy{it.id}
   .forEach{offender->
    if(attempts.size>=maxAttempts)return@forEach
    val attempt=planner.plan(current,offender.id)?:return@forEach
    val result=repair.apply(current,attempt)?:return@forEach
    val summary=if(result.outcome.accepted)
     offender.name+" makes a limited repair with "+(current.world.characters[attempt.harmedId]?.name?:attempt.harmedId)
    else
     offender.name+" attempts a repair that is not accepted"
    val hidden=HiddenEvent(
     id="hidden-"+attempt.id,
     turn=story.turn,
     actorId=offender.id,
     summary=summary,
     knownBy=setOf(offender.id,attempt.harmedId)
    )
    current=result.story.copy(world=result.story.world.copy(
     hiddenEvents=(result.story.world.hiddenEvents+hidden).distinctBy{it.id}
    ))
    attempts+=attempt
    outcomes+=result.outcome
   }
  return current to RelationshipRepairCycle(attempts,outcomes)
 }
}
