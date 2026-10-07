package com.historiasvivas.engine.discovery
import com.historiasvivas.engine.character.CharacterMind

data class InvestigationResult(val state:DiscoveryState,val evidence:Evidence,val interpretation:Interpretation)

class InvestigationEngine(private val interpreter:InterpretationEngine=InterpretationEngine()){
 fun investigate(state:DiscoveryState,signalId:String,characterId:String,mind:CharacterMind,turn:Long):InvestigationResult?{
  val signal=state.signals[signalId]?:return null
  val baseEvidenceId="evidence-"+signalId+"-"+characterId+"-"+turn
  var evidenceId=baseEvidenceId
  var suffix=2
  while(evidenceId in state.evidence){
   evidenceId=baseEvidenceId+"-"+suffix
   suffix++
  }
  val evidence=Evidence(
   id=evidenceId,signalId=signalId,
   observation=signal.surface,strength=(signal.salience-10).coerceIn(20,80),discoveredTurn=turn,discoveredBy=characterId
  )
  val previous=state.interpretations.values.filter{it.characterId==characterId&&it.signalId==signalId}.maxByOrNull{it.evidenceIds.size}
  val interpretation=if(previous==null)interpreter.initial(characterId,signal,mind,evidence) else interpreter.revise(previous,evidence)
  val updatedSignal=signal.copy(noticedBy=signal.noticedBy+characterId,investigatedBy=signal.investigatedBy+characterId)
  val next=state.copy(
   signals=state.signals+(signalId to updatedSignal),
   evidence=state.evidence+(evidence.id to evidence),
   interpretations=state.interpretations+(interpretation.id to interpretation)
  )
  return InvestigationResult(next,evidence,interpretation)
 }
}