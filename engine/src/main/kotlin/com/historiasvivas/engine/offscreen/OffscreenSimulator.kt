package com.historiasvivas.engine.offscreen
import com.historiasvivas.engine.agency.*
import com.historiasvivas.engine.model.Story

class OffscreenSimulator(private val selector:AgencySelector=AgencySelector(),private val cooldownTurns:Long=2){
 fun plan(story:Story,presentCharacterIds:Set<String>,maxActions:Int=2):List<OffscreenAction>{
  if(maxActions<=0)return emptyList()
  val recentlyActed=story.world.hiddenEvents.filter{story.turn-it.turn<cooldownTurns}.map{it.actorId}.toSet()
  return selector.select(story,maxActions*3)
   .filter{it.characterId !in presentCharacterIds && it.characterId !in recentlyActed}
   .take(maxActions)
   .mapIndexed{index,intent->
    OffscreenAction(intent,HiddenEvent(
     id="hidden-"+(story.turn+1)+"-"+index,
     turn=story.turn+1,
     actorId=intent.characterId,
     summary=intent.motivation
    ))
   }
 }
}
