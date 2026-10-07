package com.historiasvivas.engine.spatial
import kotlinx.serialization.Serializable

@Serializable data class CharacterPosition(val characterId:String,val locationId:String,val sinceTurn:Long=0)
@Serializable data class Route(val from:String,val to:String,val travelUnits:Int=1,val bidirectional:Boolean=true,val blocked:Boolean=false)
@Serializable data class SpatialState(
 val positions:Map<String,CharacterPosition> = emptyMap(),
 val routes:List<Route> = emptyList()
)

class SpatialEngine {
 fun locationOf(state:SpatialState,characterId:String):String?=state.positions[characterId]?.locationId
 fun presentAt(state:SpatialState,locationId:String):Set<String>=
  state.positions.values.filter{it.locationId==locationId}.map{it.characterId}.toSet()
 fun travelUnits(state:SpatialState,from:String,to:String):Int?{
  if(from==to)return 0
  val direct=state.routes.firstOrNull{!it.blocked&&((it.from==from&&it.to==to)||(it.bidirectional&&it.from==to&&it.to==from))}
  return direct?.travelUnits
 }
 fun move(state:SpatialState,characterId:String,to:String,turn:Long):SpatialState{
  val current=locationOf(state,characterId)?:return state
  if(travelUnits(state,current,to)==null)return state
  return state.copy(positions=state.positions+(characterId to CharacterPosition(characterId,to,turn)))
 }
}