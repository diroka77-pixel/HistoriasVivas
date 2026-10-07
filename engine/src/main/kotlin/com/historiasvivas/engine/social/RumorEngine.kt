package com.historiasvivas.engine.social
import kotlinx.serialization.Serializable

@Serializable data class SocialKnowledgeState(
 val testimonies:Map<String,Testimony> = emptyMap(),
 val heardClaims:Map<String,HeardClaim> = emptyMap(),
 val disclosureHistory:Map<String,DisclosureDecision> = emptyMap()
)

class RumorEngine {
 fun relay(original:HeardClaim,newSpeakerId:String,newListenerId:String,turn:Long,summary:String=original.claim):Testimony{
  val degraded=(original.confidence-15).coerceAtLeast(5)
  return Testimony(
   id="rumor-"+original.testimonyId+"-"+newSpeakerId+"-"+turn,
   speakerId=newSpeakerId,claim=summary,subject=original.speakerId,turn=turn,
   sourceEvidenceIds=setOf(original.testimonyId),channel=InformationChannel.RUMOR,
   intent=TestimonyIntent.UNCERTAIN,privateTruth="relay-confidence:"+degraded
  )
 }
}