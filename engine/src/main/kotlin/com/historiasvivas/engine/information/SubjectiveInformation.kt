package com.historiasvivas.engine.information
import kotlinx.serialization.Serializable
import com.historiasvivas.engine.character.Belief

@Serializable enum class InformationKind { TESTIMONY, RUMOR, LIE, EVIDENCE, INFERENCE }
@Serializable data class InformationClaim(
 val id:String,
 val subject:String,
 val claim:String,
 val kind:InformationKind,
 val sourceCharacterId:String?=null,
 val relatedEventId:String?=null,
 val reliability:Int=50,
 val intentionallyFalse:Boolean=false
)
data class ReceivedInformation(
 val receiverId:String,
 val information:InformationClaim,
 val sourceTrust:Int=0
)

class BeliefResolver {
 fun beliefFrom(input:ReceivedInformation):Belief {
  val info=input.information
  val channelBase=when(info.kind){
   InformationKind.EVIDENCE->80
   InformationKind.TESTIMONY->60
   InformationKind.INFERENCE->55
   InformationKind.RUMOR->35
   InformationKind.LIE->25
  }
  val trustAdjustment=(input.sourceTrust/5).coerceIn(-20,20)
  val reliabilityAdjustment=((info.reliability-50)/2).coerceIn(-25,25)
  val confidence=(channelBase+trustAdjustment+reliabilityAdjustment).coerceIn(1,99)
  return Belief(info.subject,info.claim,confidence,info.relatedEventId)
 }
}
class ContradictionDetector {
 fun contradictions(beliefs:List<Belief>,incoming:Belief):List<Belief> =
  beliefs.filter{it.subject==incoming.subject && it.claim!=incoming.claim && it.confidence>=50}
}
