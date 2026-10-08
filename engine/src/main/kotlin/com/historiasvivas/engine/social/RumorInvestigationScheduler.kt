package com.historiasvivas.engine.social

import com.historiasvivas.engine.discovery.DiscoveryEngine
import com.historiasvivas.engine.discovery.InvestigationEngine
import com.historiasvivas.engine.model.Story
import com.historiasvivas.engine.offscreen.Discovery
import com.historiasvivas.engine.offscreen.DiscoveryChannel
import com.historiasvivas.engine.offscreen.DiscoverySources
import com.historiasvivas.engine.offscreen.KnowledgeGate
import com.historiasvivas.engine.spatial.SpatialEngine

data class RumorInvestigation(
 val characterId:String,
 val eventId:String,
 val signalId:String,
 val evidenceId:String
)

data class RumorInvestigationCycle(
 val investigations:List<RumorInvestigation> = emptyList()
)

class RumorInvestigationScheduler(
 private val discovery:DiscoveryEngine=DiscoveryEngine(),
 private val investigation:InvestigationEngine=InvestigationEngine(),
 private val knowledge:KnowledgeGate=KnowledgeGate(),
 private val beliefs:EvidenceBeliefReconciliationEngine=EvidenceBeliefReconciliationEngine(),
 private val spatial:SpatialEngine=SpatialEngine()
){
 fun run(
  story:Story,
  excludedCharacterIds:Set<String> = emptySet(),
  maxInvestigations:Int=1
 ):Pair<Story,RumorInvestigationCycle>{
  if(maxInvestigations<=0)return story to RumorInvestigationCycle()
  var current=story
  val completed=mutableListOf<RumorInvestigation>()

  story.world.socialKnowledge.heardClaims.values.asSequence()
   .filter{heard->
    val age=story.turn-heard.turn
    heard.channel==InformationChannel.RUMOR&&heard.confidence>=40&&
     age in 1L..4L&&heard.listenerId !in excludedCharacterIds&&heard.relatedEventId!=null
   }
   .sortedWith(compareByDescending<HeardClaim>{it.confidence}.thenBy{it.listenerId})
   .forEach{heard->
    if(completed.size>=maxInvestigations)return@forEach
    val character=current.world.characters[heard.listenerId]?.takeIf{it.alive}?:return@forEach
    val eventId=heard.relatedEventId?:return@forEach
    val hidden=current.world.hiddenEvents.firstOrNull{it.id==eventId}?:return@forEach
    if(character.id in hidden.knownBy||character.id in hidden.discoveredBy)return@forEach
    if(current.world.discovery.evidence.values.any{
      it.discoveredBy==character.id&&it.relatedEventId==eventId
     })return@forEach
    val location=spatial.locationOf(current.world.spatial,character.id)?:return@forEach
    val signal=discovery.visibleAt(current.world.discovery,character.id,location)
     .firstOrNull{it.relatedEventId==eventId}?:return@forEach
    val result=investigation.investigate(
     current.world.discovery,signal.id,character.id,character.mind,current.turn
    )?:return@forEach
    val events=knowledge.discover(
     current.world.hiddenEvents,
     Discovery(
      eventId,character.id,DiscoveryChannel.INVESTIGATION,
      result.interpretation.hypothesis,result.evidence.id
     ),
     DiscoverySources(evidence=result.state.evidence)
    )
    val updatedMind=beliefs.integrate(
     character.mind,eventId,result.evidence,result.interpretation,
     current.world.socialKnowledge.testimonies,current.turn
    )
    current=current.copy(world=current.world.copy(
     characters=current.world.characters+(character.id to character.copy(mind=updatedMind)),
     discovery=result.state,
     hiddenEvents=events
    ))
    completed+=RumorInvestigation(character.id,eventId,signal.id,result.evidence.id)
   }
  return current to RumorInvestigationCycle(completed)
 }
}
