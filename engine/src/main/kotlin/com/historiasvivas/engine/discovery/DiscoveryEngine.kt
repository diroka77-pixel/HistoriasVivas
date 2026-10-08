package com.historiasvivas.engine.discovery
import kotlinx.serialization.Serializable

@Serializable enum class SignalSense { SIGHT, SOUND, TRACE, RADIO, SMELL, ENVIRONMENT }
@Serializable enum class SignalStatus { UNNOTICED, NOTICED, INVESTIGATED, IGNORED, EXPIRED }
@Serializable data class WorldSignal(
 val id:String,val locationId:String?,val routeFrom:String?=null,val routeTo:String?=null,
 val sense:SignalSense,val surface:String,val hiddenCause:String?=null,
 val createdTurn:Long,val salience:Int=50,val persistent:Boolean=true,
 val noticedBy:Set<String> = emptySet(),val investigatedBy:Set<String> = emptySet(),val ignoredBy:Set<String> = emptySet(),
 val revision:Int=0,val lastChangedTurn:Long=createdTurn,val relatedEventId:String?=null
){
 fun statusFor(characterId:String)=when{
  characterId in investigatedBy->SignalStatus.INVESTIGATED
  characterId in ignoredBy->SignalStatus.IGNORED
  characterId in noticedBy->SignalStatus.NOTICED
  else->SignalStatus.UNNOTICED
 }
}

@Serializable data class DiscoveryState(val signals:Map<String,WorldSignal> = emptyMap(),val evidence:Map<String,Evidence> = emptyMap(),val interpretations:Map<String,Interpretation> = emptyMap())

class DiscoveryEngine {
 fun visibleAt(state:DiscoveryState,characterId:String,locationId:String?): List<WorldSignal> =
  state.signals.values.filter{
   val local=locationId!=null&&it.locationId==locationId
   val global=it.locationId==null&&it.routeFrom==null&&it.routeTo==null
   (local||global)&&it.salience>0&&
    it.statusFor(characterId) !in setOf(SignalStatus.INVESTIGATED,SignalStatus.IGNORED)
  }.sortedWith(compareByDescending<WorldSignal>{it.salience}.thenBy{it.id})
 fun visibleOnJourney(state:DiscoveryState,characterId:String,from:String,to:String): List<WorldSignal> =
  state.signals.values.filter{s->
   val onRoute=(s.routeFrom==from&&s.routeTo==to)||(s.routeFrom==to&&s.routeTo==from)
   val global=s.locationId==null&&s.routeFrom==null&&s.routeTo==null
   (onRoute||global)&&s.salience>0&&
    s.statusFor(characterId) !in setOf(SignalStatus.INVESTIGATED,SignalStatus.IGNORED)
  }.sortedWith(compareByDescending<WorldSignal>{it.salience}.thenBy{it.id})
 fun notice(state:DiscoveryState,signalId:String,characterId:String):DiscoveryState{
  val s=state.signals[signalId]?:return state
  return state.copy(signals=state.signals+(signalId to s.copy(noticedBy=s.noticedBy+characterId)))
 }
 fun investigate(state:DiscoveryState,signalId:String,characterId:String):DiscoveryState{
  val s=state.signals[signalId]?:return state
  return state.copy(signals=state.signals+(signalId to s.copy(noticedBy=s.noticedBy+characterId,investigatedBy=s.investigatedBy+characterId)))
 }
 fun ignore(state:DiscoveryState,signalId:String,characterId:String):DiscoveryState{
  val s=state.signals[signalId]?:return state
  return state.copy(signals=state.signals+(signalId to s.copy(noticedBy=s.noticedBy+characterId,ignoredBy=s.ignoredBy+characterId)))
 }
 fun reactivate(state:DiscoveryState,signalId:String,turn:Long,newSurface:String?=null,newSalience:Int?=null,newHiddenCause:String?=null):DiscoveryState{
  val s=state.signals[signalId]?:return state
  if(!s.persistent || turn<=s.lastChangedTurn)return state
  val updated=s.copy(
   surface=newSurface?:s.surface,
   salience=(newSalience?:s.salience).coerceIn(0,100),
   hiddenCause=newHiddenCause?:s.hiddenCause,
   ignoredBy=emptySet(),
   investigatedBy=emptySet(),
   revision=s.revision+1,
   lastChangedTurn=turn
  )
  return state.copy(signals=state.signals+(signalId to updated))
 }
}
