package com.historiasvivas.engine.spatial
import com.historiasvivas.engine.model.*

data class AbandonedPathEffect(val event:StoryEvent,val consequence:Consequence)

class JourneyCausality {
 fun fromDiversion(story:Story,journey:Journey,diversion:JourneyDiversion):AbandonedPathEffect{
  val id="diversion-"+journey.characterId+"-"+diversion.turn
  val event=StoryEvent(id,diversion.turn,journey.characterId+" abandoned the route to "+diversion.abandonedDestination+" and diverted toward "+diversion.newDestination,participants=setOf(journey.characterId),tags=setOf("journey","diversion"))
  val consequence=Consequence(id+"-abandoned",id,"Events at "+diversion.abandonedDestination+" may continue without "+journey.characterId,diversion.turn+1)
  return AbandonedPathEffect(event,consequence)
 }
}