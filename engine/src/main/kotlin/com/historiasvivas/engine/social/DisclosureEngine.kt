package com.historiasvivas.engine.social
import kotlinx.serialization.Serializable
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.reasoning.Hypothesis

@Serializable enum class DisclosureKind { TELL, PARTIAL, DELAY, DEFLECT, LIE }
@Serializable data class DisclosureDecision(
 val id:String,val speakerId:String,val listenerId:String,val hypothesisId:String,val kind:DisclosureKind,
 val turn:Long,val motivation:String,val revealedClaim:String?=null
)

class DisclosureEngine {
 fun decide(story:Story,speakerId:String,listenerId:String,hypothesis:Hypothesis):DisclosureDecision?{
  val speaker=story.world.characters[speakerId]?:return null
  if(!speaker.alive||hypothesis.characterId!=speakerId)return null
  val relation=story.world.relationships.firstOrNull{it.fromCharacterId==speakerId&&it.toCharacterId==listenerId}
  val falseAccounts=speaker.mind.beliefRevisions.filter{
   it.sourceCharacterId==listenerId&&it.confirmed!=null&&it.turn<=story.turn
  }
  val rememberedFalseAccounts=falseAccounts.size
  val rawDeceptionPenalty=falseAccounts.sumOf{
   val age=(story.turn-it.turn).coerceAtLeast(0)
   (35-age*3).coerceAtLeast(0).toInt()
  }.coerceAtMost(70)
  val repairRelief=story.world.socialKnowledge.relationshipRepairs.values
   .filter{it.accepted&&it.harmedId==speakerId&&it.offenderId==listenerId}
   .sumOf{it.trustChange}.coerceAtMost(rawDeceptionPenalty)
  val deceptionPenalty=(rawDeceptionPenalty-repairRelief).coerceAtLeast(0)
  val rawTrust=relation?.trust?:speaker.mind.perceptions[listenerId]?.impressions?.get("trust")?:0
  val trust=(rawTrust-deceptionPenalty).coerceIn(-100,100)
  val fear=relation?.fear?:0
  val p=speaker.mind.personality.dimensions
  val honesty=p["honesty"]?:0; val caution=p["caution"]?:0; val loyalty=p["loyalty"]?:0
  val disclosure=trust+loyalty/3+honesty/3-caution/3-fear/2-deceptionPenalty
  val kind=when{
   fear>=65&&honesty<20->DisclosureKind.LIE
   disclosure>=55->DisclosureKind.TELL
   disclosure>=20->DisclosureKind.PARTIAL
   disclosure>=-10->DisclosureKind.DELAY
   honesty<=-45&&trust<=-25->DisclosureKind.LIE
   else->DisclosureKind.DEFLECT
  }
  val claim=when(kind){
   DisclosureKind.TELL->hypothesis.claim
   DisclosureKind.PARTIAL->"I found indications related to "+hypothesis.subject+", but I am not certain enough to tell you everything yet."
   DisclosureKind.LIE->"I found nothing reliable about "+hypothesis.subject+"."
   else->null
  }
  return DisclosureDecision("disclosure-"+speakerId+"-"+listenerId+"-"+hypothesis.id+"-"+story.turn,speakerId,listenerId,hypothesis.id,kind,story.turn,
   "trust=$trust fear=$fear honesty=$honesty caution=$caution loyalty=$loyalty rememberedFalseAccounts=$rememberedFalseAccounts deceptionPenalty=$deceptionPenalty repairRelief=$repairRelief",claim)
 }
 fun toTestimony(decision:DisclosureDecision,hypothesis:Hypothesis):Testimony?{
  val claim=decision.revealedClaim?:return null
  val intent=when(decision.kind){
   DisclosureKind.TELL->TestimonyIntent.HONEST
   DisclosureKind.PARTIAL->TestimonyIntent.WITHHOLDING
   DisclosureKind.LIE->TestimonyIntent.LYING
   DisclosureKind.DEFLECT->TestimonyIntent.MISLEADING
   DisclosureKind.DELAY->return null
  }
  return Testimony(
   id="testimony-"+decision.id,speakerId=decision.speakerId,claim=claim,subject=hypothesis.subject,turn=decision.turn,
   sourceEvidenceIds=hypothesis.supportingEvidenceIds,channel=InformationChannel.DIRECT_TESTIMONY,
   intent=intent,privateTruth=if(intent==TestimonyIntent.HONEST)null else hypothesis.claim,
   relatedEventId=hypothesis.relatedEventId
  )
 }
}
