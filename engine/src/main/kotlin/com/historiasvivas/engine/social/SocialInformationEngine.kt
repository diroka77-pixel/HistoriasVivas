package com.historiasvivas.engine.social
import kotlinx.serialization.Serializable
import com.historiasvivas.engine.character.*

@Serializable enum class TestimonyIntent { HONEST, WITHHOLDING, MISLEADING, LYING, UNCERTAIN }
@Serializable enum class InformationChannel { DIRECT_TESTIMONY, RUMOR, SECOND_HAND, MESSAGE }
@Serializable data class Testimony(
 val id:String,val speakerId:String,val claim:String,val subject:String,val turn:Long,
 val sourceEvidenceIds:Set<String> = emptySet(),val channel:InformationChannel=InformationChannel.DIRECT_TESTIMONY,
 val intent:TestimonyIntent=TestimonyIntent.HONEST,val privateTruth:String?=null,val reliability:Int?=null,
 val relatedEventId:String?=null,
 val subject:String?=null
)
@Serializable data class HeardClaim(
 val testimonyId:String,val listenerId:String,val speakerId:String,val claim:String,val confidence:Int,val turn:Long,val channel:InformationChannel,
 val relatedEventId:String?=null
)

class SocialInformationEngine {
 fun hear(testimony:Testimony,listenerId:String,listenerMind:CharacterMind):HeardClaim{
  val perception=listenerMind.perceptions[testimony.speakerId]
  val perceivedTrust=perception?.impressions?.get("trust")?:0
  val baseline=when(testimony.channel){
   InformationChannel.DIRECT_TESTIMONY->55; InformationChannel.MESSAGE->50; InformationChannel.SECOND_HAND->38; InformationChannel.RUMOR->25
  }
  val sourceReliability=testimony.reliability?:baseline
  val sociallyAdjusted=(sourceReliability+perceivedTrust/3).coerceIn(5,90)
  val confidence=if(testimony.channel==InformationChannel.RUMOR) sociallyAdjusted.coerceAtMost(sourceReliability) else sociallyAdjusted
  return HeardClaim(
   testimonyId=testimony.id,listenerId=listenerId,speakerId=testimony.speakerId,
   claim=testimony.claim,confidence=confidence,turn=testimony.turn,
   channel=testimony.channel,relatedEventId=testimony.relatedEventId,
   subject=testimony.subject
  )
 }
 fun asBelief(heard:HeardClaim)=Belief(
  subject=heard.subject?:heard.speakerId+":"+heard.testimonyId,
  claim=heard.claim,confidence=heard.confidence,sourceEventId=heard.testimonyId
 )
 fun integrateBelief(
  mind:CharacterMind,
  heard:HeardClaim,
  testimonies:Map<String,Testimony>,
  turn:Long=heard.turn
 ):CharacterMind{
  if(mind.beliefs.any{it.sourceEventId==heard.testimonyId})return mind
  val belief=asBelief(heard)
  val source=testimonies[heard.testimonyId]
  val eventId=heard.relatedEventId
  val isGroundedDirect=source!=null&&
   source.channel!=InformationChannel.RUMOR&&
   source.intent==TestimonyIntent.HONEST&&
   eventId!=null
  if(!isGroundedDirect)return mind.copy(beliefs=mind.beliefs+belief)

  val superseded=mind.beliefs.filter{prior->
   prior.status!=BeliefStatus.REJECTED&&
    prior.sourceEventId?.let{testimonies[it]}?.let{
     it.channel==InformationChannel.RUMOR&&it.relatedEventId==eventId
    }==true
  }
  if(superseded.isEmpty())return mind.copy(beliefs=mind.beliefs+belief)
  val supersededIds=superseded.mapNotNull{it.sourceEventId}.toSet()
  val revisedBeliefs=mind.beliefs.map{prior->
   if(prior.sourceEventId !in supersededIds)prior else prior.copy(
    status=if(prior.claim==belief.claim)BeliefStatus.DOUBTED else BeliefStatus.CONTESTED,
    lastRevisedTurn=turn,
    supersededBySourceId=heard.testimonyId
   )
  }
  val existingRevisionIds=mind.beliefRevisions.map{it.id}.toSet()
  val revisions=superseded.map{prior->
   val revised=revisedBeliefs.first{it.sourceEventId==prior.sourceEventId}
   BeliefRevision(
    id="social-revision-"+prior.sourceEventId+"-"+heard.testimonyId+"-"+turn,
    previous=prior,
    revised=revised,
    confirmed=belief,
    evidenceId=heard.testimonyId,
    sourceCharacterId=heard.speakerId,
    turn=turn
   )
  }.filter{it.id !in existingRevisionIds}
  return mind.copy(
   beliefs=revisedBeliefs+belief,
   beliefRevisions=mind.beliefRevisions+revisions
  )
 }
}
