package com.historiasvivas.engine.offscreen
import com.historiasvivas.engine.agency.*
import com.historiasvivas.engine.model.Story

class OffscreenSimulator(private val selector:AgencySelector=AgencySelector()){
 fun plan(story:Story,presentCharacterIds:Set<String>,maxActions:Int=2):List<OffscreenAction>{
  if(maxActions<=0)return emptyList()
  return selector.select(story,maxActions*3)
   .filter{it.characterId !in presentCharacterIds}
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
