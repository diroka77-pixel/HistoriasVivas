package com.historiasvivas.engine.reasoning
import com.historiasvivas.engine.model.Story

data class AutonomousInference(val characterId:String,val hypothesis:Hypothesis)

class AutonomousReasoningEngine(private val hypotheses:HypothesisEngine=HypothesisEngine()){
 fun infer(story:Story,characterId:String,turn:Long=story.turn):List<AutonomousInference>{
  val character=story.world.characters[characterId]?:return emptyList()
  val evidence=story.world.discovery.evidence.values.filter{it.discoveredBy==characterId}
  if(evidence.size<2)return emptyList()
  val grouped=evidence.groupBy{it.signalId.substringBefore('-')}
  return grouped.values.filter{it.size>=2}.take(2).mapNotNull{clues->
   val subject=clues.first().signalId.substringBefore('-')
   hypotheses.formulate(characterId,subject,"The known clues may share a common cause",clues,character.mind.beliefs,turn)
    ?.let{AutonomousInference(characterId,it)}
  }
 }
}