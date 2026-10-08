package com.historiasvivas.engine.social

import com.historiasvivas.engine.character.Belief
import com.historiasvivas.engine.character.BeliefRevision
import com.historiasvivas.engine.character.BeliefStatus
import com.historiasvivas.engine.character.CharacterMind
import com.historiasvivas.engine.discovery.Evidence
import com.historiasvivas.engine.discovery.Interpretation

class EvidenceBeliefReconciliationEngine {
 fun integrate(
  mind:CharacterMind,
  eventId:String,
  evidence:Evidence,
  interpretation:Interpretation,
  testimonies:Map<String,Testimony>,
  turn:Long
 ):CharacterMind{
  if(mind.beliefs.any{it.sourceEventId==evidence.id})return mind
  val grounded=Belief(
   subject="event:"+eventId,
   claim=interpretation.hypothesis,
   confidence=interpretation.confidence,
   sourceEventId=evidence.id
  )
  val superseded=mind.beliefs.filter{prior->
   prior.status!=BeliefStatus.REJECTED&&
    prior.sourceEventId?.let{testimonies[it]}?.let{
     it.channel==InformationChannel.RUMOR&&it.relatedEventId==eventId
    }==true
  }
  if(superseded.isEmpty())return mind.copy(beliefs=mind.beliefs+grounded)
  val supersededIds=superseded.mapNotNull{it.sourceEventId}.toSet()
  val revised=mind.beliefs.map{prior->
   if(prior.sourceEventId !in supersededIds)prior else prior.copy(
    status=if(prior.claim==grounded.claim)BeliefStatus.DOUBTED else BeliefStatus.CONTESTED,
    lastRevisedTurn=turn,
    supersededBySourceId=evidence.id
   )
  }
  val existing=mind.beliefRevisions.map{it.id}.toSet()
  val revisions=superseded.map{prior->
   BeliefRevision(
    id="evidence-revision-"+prior.sourceEventId+"-"+evidence.id+"-"+turn,
    previous=prior,
    revised=revised.first{it.sourceEventId==prior.sourceEventId},
    confirmed=grounded,
    evidenceId=evidence.id,
    sourceCharacterId=null,
    turn=turn
   )
  }.filter{it.id !in existing}
  return mind.copy(
   beliefs=revised+grounded,
   beliefRevisions=mind.beliefRevisions+revisions
  )
 }
}
