package com.historiasvivas.engine.discovery
import com.historiasvivas.engine.character.*
import kotlinx.serialization.Serializable

@Serializable data class Evidence(
 val id:String,val signalId:String,val observation:String,val strength:Int=50,val discoveredTurn:Long,val discoveredBy:String
)
@Serializable data class Interpretation(
 val id:String,val characterId:String,val signalId:String,val hypothesis:String,val confidence:Int,val evidenceIds:Set<String> = emptySet(),val revisedFrom:String?=null
)

class InterpretationEngine {
 fun initial(characterId:String,signal:WorldSignal,mind:CharacterMind,evidence:Evidence):Interpretation{
  val caution=mind.personality.dimensions["caution"]?:0
  val trust=mind.personality.dimensions["trust"]?:0
  val confidence=(35+evidence.strength/3-caution/8+trust/12).coerceIn(10,85)
  val hypothesis=when(signal.sense){
   SignalSense.TRACE->"Someone or something passed through here recently"
   SignalSense.RADIO->"The signal may have an intentional source"
   SignalSense.SOUND->"The sound suggests nearby activity"
   SignalSense.SMELL->"The smell may indicate a recent presence"
   SignalSense.ENVIRONMENT->"The environment is changing for an unknown reason"
   SignalSense.SIGHT->"The visible anomaly may be significant"
  }
  return Interpretation("interpretation-"+characterId+"-"+signal.id,characterId,signal.id,hypothesis,confidence,setOf(evidence.id))
 }
 fun revise(previous:Interpretation,newEvidence:Evidence,newHypothesis:String?=null):Interpretation{
  val confidence=(previous.confidence+newEvidence.strength/5).coerceIn(5,95)
  return previous.copy(
   id=previous.id+"-r"+(previous.evidenceIds.size+1),
   hypothesis=newHypothesis?:previous.hypothesis,confidence=confidence,
   evidenceIds=previous.evidenceIds+newEvidence.id,revisedFrom=previous.id
  )
 }
}