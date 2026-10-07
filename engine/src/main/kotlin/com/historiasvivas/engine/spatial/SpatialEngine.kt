package com.historiasvivas.engine.spatial
import kotlinx.serialization.Serializable

@Serializable data class CharacterPosition(val characterId:String,val locationId:String,val sinceTurn:Long=0)
@Serializable data class Route(val from:String,val to:String,val travelUnits:Int=1,val bidirectional:Boolean=true,val blocked:Boolean=false)
@Serializable data class Journey(val characterId:String,val from:String,val to:String,val totalUnits:Int,val elapsedUnits:Int=0,val startedTurn:Long=0){
 val remainingUnits:Int get()=(totalUnits-elapsedUnits).coerceAtLeast(0)
 val completed:Boolean get()=elapsedUnits>=totalUnits
}
@Serializable data class SpatialState(
 val positions:Map<String,CharacterPosition> = emptyMap(),
 val routes:List<Route> = emptyList(),
 val journeys:Map<String,Journey> = emptyMap()
)

class SpatialEngine {
 fun locationOf(state:SpatialState,characterId:String):String?=if(characterId in state.journeys)null else state.positions[characterId]?.locationId
 fun presentAt(state:SpatialState,locationId:String):Set<String>=
  state.positions.values.filter{it.locationId==locationId&&it.characterId !in state.journeys}.map{it.characterId}.toSet()
 fun travelUnits(state:SpatialState,from:String,to:String):Int?{
  if(from==to)return 0
  return state.routes.firstOrNull{!it.blocked&&((it.from==from&&it.to==to)||(it.bidirectional&&it.from==to&&it.to==from))}?.travelUnits
 }
 fun startJourney(state:SpatialState,characterId:String,to:String,turn:Long):SpatialState{
  if(characterId in state.journeys)return state
  val from=state.positions[characterId]?.locationId?:return state
  val units=travelUnits(state,from,to)?:return state
  if(units<=0)return state
  return state.copy(journeys=state.journeys+(characterId to Journey(characterId,from,to,units,0,turn)))
 }
 fun advanceJourneys(state:SpatialState,units:Int=1,turn:Long):SpatialState{
  if(units<=0)return state
  var positions=state.positions
  val remaining=mutableMapOf<String,Journey>()
  state.journeys.forEach{(id,j)->
   val advanced=j.copy(elapsedUnits=(j.elapsedUnits+units).coerceAtMost(j.totalUnits))
   if(advanced.completed)positions=positions+(id to CharacterPosition(id,j.to,turn)) else remaining[id]=advanced
  }
  return state.copy(positions=positions,journeys=remaining)
 }
}