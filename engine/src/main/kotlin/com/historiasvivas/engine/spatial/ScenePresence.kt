package com.historiasvivas.engine.spatial

import com.historiasvivas.engine.model.Story

class ScenePresence(
 private val spatial:SpatialEngine=SpatialEngine()
){
 fun withProtagonist(story:Story):Set<String>{
  val location=spatial.locationOf(story.world.spatial,story.protagonistId)
   ?:return setOf(story.protagonistId).filter{it in story.world.characters}.toSet()
  return spatial.presentAt(story.world.spatial,location)
   .filter{it in story.world.characters}
   .toSet()
 }
}
