package com.historiasvivas.engine.agency
import kotlinx.serialization.Serializable
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.reasoning.*
import com.historiasvivas.engine.spatial.SpatialEngine
import com.historiasvivas.engine.discovery.DiscoveryEngine

@Serializable enum class AutonomousActionKind { INVESTIGATE, TRAVEL, WARN, PROTECT, HIDE_EVIDENCE, WITHDRAW }
@Serializable data class AutonomousActionPlan(
 val id:String,val characterId:String,val hypothesisId:String,val kind:AutonomousActionKind,
 val priority:Int,val targetLocationId:String?=null,val targetCharacterId:String?=null,val motivation:String,val turn:Long,val targetSignalId:String?=null
)

class HypothesisAgencyEngine(private val spatial:SpatialEngine=SpatialEngine(),private val discovery:DiscoveryEngine=DiscoveryEngine()) {
 fun plan(story:Story,characterId:String,hypothesis:Hypothesis):AutonomousActionPlan?{
  val c=story.world.characters[characterId]?:return null
  if(!c.alive||hypothesis.characterId!=characterId||hypothesis.status==HypothesisStatus.REJECTED)return null
  val p=c.mind.personality.dimensions
  val courage=p["courage"]?:0; val caution=p["caution"]?:0; val compassion=p["compassion"]?:0; val secrecy=p["secrecy"]?:0
  val base=(hypothesis.confidence+courage/4-caution/6).coerceIn(10,95)
  val spatialLocationIds=(story.world.spatial.positions.values.map{it.locationId}+story.world.spatial.routes.flatMap{listOf(it.from,it.to)}).toSet()\n  val targetLocation=(story.world.locations.keys+spatialLocationIds).distinct().firstOrNull{hypothesis.subject.contains(it,true)}
  val currentLocation=spatial.locationOf(story.world.spatial,characterId)
  val valued=story.world.relationships.asSequence()
   .filter{it.fromCharacterId==characterId&&story.world.characters[it.toCharacterId]?.alive==true}
   .map{it to (it.affection+it.trust/2-it.fear/3)}
   .sortedWith(compareByDescending<Pair<Relationship,Int>>{it.second}.thenBy{it.first.toCharacterId})
   .firstOrNull()
  val protectTarget=valued?.takeIf{it.second>=55}?.first?.toCharacterId
  val presentListeners=currentLocation?.let{spatial.presentAt(story.world.spatial,it)}.orEmpty()-characterId
  val warnTarget=valued?.first?.toCharacterId?.takeIf{it in presentListeners}
   ?:presentListeners.filter{story.world.characters[it]?.alive==true}.sorted().firstOrNull()
  val hypothesisSignalIds=hypothesis.supportingEvidenceIds.mapNotNull{story.world.discovery.evidence[it]?.signalId}.toSet()
  val relatedVisibleSignal=discovery.visibleAt(story.world.discovery,characterId,currentLocation).firstOrNull{it.id in hypothesisSignalIds}
  val kind=when{
   secrecy>=65&&hypothesis.confidence>=65&&relatedVisibleSignal!=null->AutonomousActionKind.HIDE_EVIDENCE
   compassion>=75&&hypothesis.confidence>=65&&protectTarget!=null->AutonomousActionKind.PROTECT
   compassion>=65&&hypothesis.confidence>=60&&warnTarget!=null->AutonomousActionKind.WARN
   caution>=70&&courage<30->AutonomousActionKind.WITHDRAW
   targetLocation!=null&&targetLocation!=currentLocation&&hypothesis.confidence>=55->AutonomousActionKind.TRAVEL
   else->AutonomousActionKind.INVESTIGATE
  }
  val targetCharacter=when(kind){
   AutonomousActionKind.PROTECT->protectTarget
   AutonomousActionKind.WARN->warnTarget
   else->null
  }
  val targetSignal=if(kind==AutonomousActionKind.INVESTIGATE||kind==AutonomousActionKind.HIDE_EVIDENCE)
   relatedVisibleSignal?.id
  else null
  return AutonomousActionPlan(
   "auto-"+characterId+"-"+hypothesis.id+"-"+story.turn,characterId,hypothesis.id,kind,base,
   targetLocationId=targetLocation,targetCharacterId=targetCharacter,targetSignalId=targetSignal,
   motivation="Acts on private hypothesis: "+hypothesis.claim,turn=story.turn
  )
 }
}