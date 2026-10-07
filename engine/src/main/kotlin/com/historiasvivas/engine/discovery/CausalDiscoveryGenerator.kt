package com.historiasvivas.engine.discovery
import com.historiasvivas.engine.model.Story
import com.historiasvivas.engine.spatial.Journey

class CausalDiscoveryGenerator {
 fun generate(story:Story):List<WorldSignal>{
  val signals=mutableListOf<WorldSignal>()
  story.world.spatial.journeys.values.forEach{j->signals+=fromJourney(story,j)}
  story.world.environment.pressures.values.filter{it.intensity>=45}.forEach{p->
   signals+=WorldSignal(
    id="pressure-"+p.id,locationId=null,sense=SignalSense.ENVIRONMENT,
    surface=surfaceForPressure(p.id,p.description),hiddenCause="pressure:"+p.id+":"+p.intensity,
    createdTurn=story.turn,salience=p.intensity.coerceIn(45,95),persistent=true
   )
  }
  return signals.distinctBy{it.id}
 }
 fun fromCompletedJourney(story:Story,j:Journey):WorldSignal=fromJourney(story,j.copy(elapsedUnits=j.totalUnits))
 private fun fromJourney(story:Story,j:Journey):WorldSignal=WorldSignal(
  id="trace-"+j.characterId+"-"+j.startedTurn,locationId=null,routeFrom=j.from,routeTo=j.to,
  sense=SignalSense.TRACE,surface=surfaceForJourney(j),
  hiddenCause="journey:"+j.characterId+":"+j.from+":"+j.to+":elapsed="+j.elapsedUnits,
  createdTurn=story.turn,salience=(55+j.elapsedUnits*5).coerceAtMost(80),persistent=true
 )
 private fun surfaceForJourney(j:Journey):String=when{
  j.elapsedUnits<=0->"Fresh signs show that someone has just entered the route"
  j.remainingUnits<=1->"A continuous trail of recent passage runs along most of the route"
  else->"Signs of recent passage extend farther along the route"
 }
 private fun surfaceForPressure(id:String,description:String):String=when{
  id.contains("fauna",true)->"Unusual tracks and disturbed ground suggest increased animal activity"
  description.isNotBlank()->"The surroundings show a subtle but persistent change: "+description
  else->"Something in the environment has changed enough to leave observable signs"
 }
}