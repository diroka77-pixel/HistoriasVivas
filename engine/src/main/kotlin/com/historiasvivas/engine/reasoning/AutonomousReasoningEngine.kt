package com.historiasvivas.engine.reasoning
import com.historiasvivas.engine.model.Story

data class AutonomousInference(val characterId:String,val hypothesis:Hypothesis)

class AutonomousReasoningEngine(private val hypotheses:HypothesisEngine=HypothesisEngine()){
 fun infer(story:Story,characterId:String,turn:Long=story.turn):List<AutonomousInference>{
  val character=story.world.characters[characterId]?:return emptyList()
  if(!character.alive)return emptyList()
  val evidence=story.world.discovery.evidence.values.filter{it.discoveredBy==characterId}.distinctBy{it.id}
  if(evidence.size<2)return emptyList()

  val contradictions=story.world.discovery.interpretations.values
   .filter{it.characterId==characterId&&it.relation==com.historiasvivas.engine.discovery.InterpretationRelation.CONTRADICTS&&it.relatedHypothesisId!=null}
   .groupBy{it.relatedHypothesisId!!}
   .mapValues{(_,items)->
    val ids=items.flatMap{it.evidenceIds}.toSet()
    evidence.filter{it.id in ids}
   }

  fun contextFor(signalId:String):String{
   val signal=story.world.discovery.signals[signalId]?:return "legacy"
   signal.locationId?.let{return "location:"+it}
   if(signal.routeFrom!=null&&signal.routeTo!=null){
    val ends=listOf(signal.routeFrom,signal.routeTo).sorted()
    return "route:"+ends.joinToString("<->")
   }
   return "signal:"+signalId
  }
  val interpretedGroups=story.world.discovery.interpretations.values
   .filter{it.characterId==characterId&&it.relation==com.historiasvivas.engine.discovery.InterpretationRelation.SUPPORTS}
   .groupBy{it.hypothesis to contextFor(it.signalId)}
   .mapNotNull{(key,interpretations)->
    val evidenceIds=interpretations.flatMap{it.evidenceIds}.toSet()
    val clues=evidence.filter{it.id in evidenceIds}
    if(clues.size>=2) Triple(key.first,key.second,clues) else null
   }
   .sortedWith(compareBy<Triple<String,String,List<com.historiasvivas.engine.discovery.Evidence>>>{it.first}.thenBy{it.second})

  return interpretedGroups.take(2).mapNotNull{(claim,context,clues)->
   val subject=when{
    context.startsWith("location:")->context.removePrefix("location:")
    context=="legacy"->clues.map{it.signalId}.distinct().sorted().joinToString("+")
    context.startsWith("signal:")->clues.map{it.signalId}.distinct().sorted().joinToString("+")
    else->context
   }
   val existing=story.world.reasoning.hypotheses.values
    .filter{it.characterId==characterId&&it.claim==claim&&it.subject==subject}
    .maxByOrNull{it.revisedTurn}
   val hypothesis=if(existing==null){
    hypotheses.formulate(characterId,subject,claim,clues,character.mind.beliefs,turn)
   }else{
    hypotheses.update(existing,clues,contradictions[existing.id].orEmpty(),turn)
   }
   hypothesis?.let{AutonomousInference(characterId,it)}
  }
 }
}
