package com.historiasvivas.engine.reasoning
import com.historiasvivas.engine.model.Story

data class AutonomousInference(val characterId:String,val hypothesis:Hypothesis)

class AutonomousReasoningEngine(private val hypotheses:HypothesisEngine=HypothesisEngine()){
 fun infer(story:Story,characterId:String,turn:Long=story.turn):List<AutonomousInference>{
  val character=story.world.characters[characterId]?:return emptyList()
  if(!character.alive)return emptyList()
  val evidence=story.world.discovery.evidence.values.filter{it.discoveredBy==characterId}.distinctBy{it.id}
  if(evidence.size<2)return emptyList()

  val interpretedGroups=story.world.discovery.interpretations.values
   .filter{it.characterId==characterId}
   .groupBy{it.hypothesis}
   .mapNotNull{(claim,interpretations)->
    val evidenceIds=interpretations.flatMap{it.evidenceIds}.toSet()
    val clues=evidence.filter{it.id in evidenceIds}
    if(clues.size>=2) claim to clues else null
   }
   .sortedBy{it.first}

  return interpretedGroups.take(2).mapNotNull{(claim,clues)->
   val subject=clues.map{it.signalId}.distinct().sorted().joinToString("+")
   hypotheses.formulate(characterId,subject,claim,clues,character.mind.beliefs,turn)
    ?.let{AutonomousInference(characterId,it)}
  }
 }
}
