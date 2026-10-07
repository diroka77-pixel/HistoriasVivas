package com.historiasvivas.engine.reasoning
import kotlinx.serialization.Serializable
import com.historiasvivas.engine.character.Belief
import com.historiasvivas.engine.discovery.Evidence

@Serializable enum class HypothesisStatus { TENTATIVE, PLAUSIBLE, STRONG, WEAKENED, REJECTED }
@Serializable data class Hypothesis(
 val id:String,val characterId:String,val subject:String,val claim:String,val confidence:Int,
 val supportingEvidenceIds:Set<String> = emptySet(),val supportingBeliefSources:Set<String> = emptySet(),
 val contradictingEvidenceIds:Set<String> = emptySet(),val createdTurn:Long,val revisedTurn:Long=createdTurn,
 val status:HypothesisStatus=HypothesisStatus.TENTATIVE
)
@Serializable data class ReasoningState(val hypotheses:Map<String,Hypothesis> = emptyMap())

class HypothesisEngine {
 fun formulate(characterId:String,subject:String,claim:String,evidence:List<Evidence>,beliefs:List<Belief>,turn:Long):Hypothesis?{
  val known=evidence.filter{it.discoveredBy==characterId}.distinctBy{it.id}
  if(known.size<2)return null
  val evidenceScore=known.sumOf{it.strength}.div(known.size).coerceIn(0,100)
  val beliefScore=beliefs.filter{it.confidence>=40}.map{it.confidence}.average().takeUnless{it.isNaN()}?.toInt()?:0
  val confidence=(evidenceScore*2/3+beliefScore/3).coerceIn(10,90)
  return Hypothesis(
   id="hypothesis-"+characterId+"-"+subject+"-"+turn,characterId=characterId,subject=subject,claim=claim,confidence=confidence,
   supportingEvidenceIds=known.map{it.id}.toSet(),supportingBeliefSources=beliefs.mapNotNull{it.sourceEventId}.toSet(),
   createdTurn=turn,status=status(confidence)
  )
 }
 fun update(h:Hypothesis,supporting:List<Evidence>,contradicting:List<Evidence>,turn:Long):Hypothesis{
  val knownSupport=supporting.filter{it.discoveredBy==h.characterId && it.id !in h.supportingEvidenceIds && it.id !in h.contradictingEvidenceIds}.distinctBy{it.id}
  val knownOpposition=contradicting.filter{it.discoveredBy==h.characterId && it.id !in h.supportingEvidenceIds && it.id !in h.contradictingEvidenceIds}.distinctBy{it.id}
  val support=knownSupport.sumOf{it.strength}/5
  val oppose=knownOpposition.sumOf{it.strength}/4
  val confidence=(h.confidence+support-oppose).coerceIn(0,95)
  val base=status(confidence)
  val finalStatus=if(knownOpposition.isNotEmpty()&&confidence>=20&&confidence<45)HypothesisStatus.WEAKENED else base
  return h.copy(
   confidence=confidence,supportingEvidenceIds=h.supportingEvidenceIds+knownSupport.map{it.id},
   contradictingEvidenceIds=h.contradictingEvidenceIds+knownOpposition.map{it.id},revisedTurn=turn,status=finalStatus
  )
 }
 private fun status(c:Int)=when{c>=75->HypothesisStatus.STRONG;c>=50->HypothesisStatus.PLAUSIBLE;c>=20->HypothesisStatus.TENTATIVE;else->HypothesisStatus.REJECTED}
}