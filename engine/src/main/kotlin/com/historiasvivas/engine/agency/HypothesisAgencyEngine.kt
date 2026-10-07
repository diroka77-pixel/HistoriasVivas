package com.historiasvivas.engine.agency
import kotlinx.serialization.Serializable
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.reasoning.*
import com.historiasvivas.engine.spatial.SpatialEngine

@Serializable enum class AutonomousActionKind { INVESTIGATE, TRAVEL, WARN, PROTECT, HIDE_EVIDENCE, WITHDRAW }
@Serializable data class AutonomousActionPlan(
 val id:String,val characterId:String,val hypothesisId:String,val kind:AutonomousActionKind,
 val priority:Int,val targetLocationId:String?=null,val targetCharacterId:String?=null,val motivation:String,val turn:Long
)

class HypothesisAgencyEngine(private val spatial:SpatialEngine=SpatialEngine()) {
 fun plan(story:Story,characterId:String,hypothesis:Hypothesis):AutonomousActionPlan?{
  val c=story.world.characters[characterId]?:return null
  if(!c.alive||hypothesis.characterId!=characterId||hypothesis.status==HypothesisStatus.REJECTED)return null
  val p=c.mind.personality.dimensions
  val courage=p["courage"]?:0; val caution=p["caution"]?:0; val compassion=p["compassion"]?:0; val secrecy=p["secrecy"]?:0
  val base=(hypothesis.confidence+courage/4-caution/6).coerceIn(10,95)
  val targetLocation=story.world.locations.keys.firstOrNull{hypothesis.subject.contains(it,true)}
  val currentLocation=spatial.locationOf(story.world.spatial,characterId)
  val kind=when{
   secrecy>=65&&hypothesis.confidence>=65->AutonomousActionKind.HIDE_EVIDENCE
   compassion>=65&&hypothesis.confidence>=60->AutonomousActionKind.WARN
   caution>=70&&courage<30->AutonomousActionKind.WITHDRAW
   targetLocation!=null&&targetLocation!=currentLocation&&hypothesis.confidence>=55->AutonomousActionKind.TRAVEL
   else->AutonomousActionKind.INVESTIGATE
  }
  return AutonomousActionPlan(
   "auto-"+characterId+"-"+hypothesis.id+"-"+story.turn,characterId,hypothesis.id,kind,base,
   targetLocationId=targetLocation,motivation="Acts on private hypothesis: "+hypothesis.claim,turn=story.turn
  )
 }
}