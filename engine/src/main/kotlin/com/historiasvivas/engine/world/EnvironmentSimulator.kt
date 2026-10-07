package com.historiasvivas.engine.world
import kotlinx.serialization.Serializable
import com.historiasvivas.engine.model.*

@Serializable enum class WeatherKind { CLEAR, WIND, RAIN, STORM, COLD, HEAT, UNKNOWN }
@Serializable data class ResourcePool(val id:String,val amount:Int,val capacity:Int=100,val criticalBelow:Int=20)
@Serializable data class WorldPressure(val id:String,val intensity:Int=0,val trend:Int=0,val description:String="")
@Serializable data class EnvironmentState(
 val day:Long=0,val phase:Int=0,val weather:WeatherKind=WeatherKind.CLEAR,
 val resources:Map<String,ResourcePool> = emptyMap(),
 val pressures:Map<String,WorldPressure> = emptyMap()
)
data class EnvironmentTick(val next:EnvironmentState,val publicEvents:List<StoryEvent> = emptyList())

class EnvironmentSimulator {
 fun tick(story:Story):EnvironmentTick{
  val e=story.world.environment
  val nextPhase=(e.phase+1)%4
  val nextDay=e.day+if(nextPhase==0)1 else 0
  val resources=e.resources.mapValues{(_,r)->
   val consumption=if(nextPhase==0)1 else 0
   r.copy(amount=(r.amount-consumption).coerceAtLeast(0))
  }
  val pressures=e.pressures.mapValues{(_,p)->p.copy(intensity=(p.intensity+p.trend).coerceIn(0,100))}
  val events=resources.values.filter{it.amount<=it.criticalBelow}.map{r->
   StoryEvent("env-"+(story.turn+1)+"-"+r.id,story.turn+1,r.id+" supplies are becoming critical",tags=setOf("environment","resource",r.id))
  }
  return EnvironmentTick(e.copy(day=nextDay,phase=nextPhase,resources=resources,pressures=pressures),events)
 }
}
