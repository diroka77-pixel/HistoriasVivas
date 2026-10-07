package com.historiasvivas.engine.social
import kotlinx.serialization.Serializable
import com.historiasvivas.engine.character.*

@Serializable enum class TestimonyIntent { HONEST, WITHHOLDING, MISLEADING, LYING, UNCERTAIN }
@Serializable enum class InformationChannel { DIRECT_TESTIMONY, RUMOR, SECOND_HAND, MESSAGE }
@Serializable data class Testimony(
 val id:String,val speakerId:String,val claim:String,val subject:String,val turn:Long,
 val sourceEvidenceIds:Set<String> = emptySet(),val channel:InformationChannel=InformationChannel.DIRECT_TESTIMONY,
 val intent:TestimonyIntent=TestimonyIntent.HONEST,val privateTruth:String?=null,val reliability:Int?=null
)
@Serializable data class HeardClaim(
 val testimonyId:String,val listenerId:String,val speakerId:String,val claim:String,val confidence:Int,val turn:Long,val channel:InformationChannel
)

class SocialInformationEngine {
 fun hear(testimony:Testimony,listenerId:String,listenerMind:CharacterMind):HeardClaim{
  val perception=listenerMind.perceptions[testimony.speakerId]
  val perceivedTrust=perception?.impressions?.get("trust")?:0
  val baseline=when(testimony.channel){
   InformationChannel.DIRECT_TESTIMONY->55; InformationChannel.MESSAGE->50; InformationChannel.SECOND_HAND->38; InformationChannel.RUMOR->25
  }
  val sourceReliability=testimony.reliability?:baseline
  val confidence=(sourceReliability+perceivedTrust/3).coerceIn(5,90)
  return HeardClaim(testimony.id,listenerId,testimony.speakerId,testimony.claim,confidence,testimony.turn,testimony.channel)
 }
 fun asBelief(heard:HeardClaim)=Belief(
  subject=heard.speakerId+":"+heard.testimonyId,claim=heard.claim,confidence=heard.confidence,sourceEventId=heard.testimonyId
 )
}