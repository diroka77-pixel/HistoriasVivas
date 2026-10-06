package com.historiasvivas.engine.ai
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.narrative.NarrativeBeat
import com.historiasvivas.engine.offscreen.HiddenEvent

data class PlayerNarrativeView(val protagonistId:String,val premise:String,val visibleFacts:Set<String>,val knownSecrets:List<Secret>,val knownHiddenEvents:List<HiddenEvent>,val recentVisibleEvents:List<StoryEvent>,val narrativeBeat:NarrativeBeat)
data class PrivateSimulationView(val worldFacts:Set<String>,val allSecrets:List<Secret>,val hiddenEvents:List<HiddenEvent>,val pendingConsequences:List<Consequence>)
data class NarrativePacket(val player:PlayerNarrativeView,val simulation:PrivateSimulationView,val rules:List<String>)

class NarrativePacketBuilder {
 fun build(story:Story,protagonistId:String,beat:NarrativeBeat):NarrativePacket {
  require(protagonistId in story.world.characters){"Unknown protagonist"}
  val protagonist=story.world.characters.getValue(protagonistId)
  val knownSecrets=story.world.secrets.filter{protagonistId in it.knownBy || it.revealed}
  val visibleHidden=story.world.hiddenEvents.filter{protagonistId in it.knownBy || protagonistId in it.discoveredBy}
  val scopedVisible=story.world.scopedFacts.filter{it.visibleTo(protagonistId)}.map{it.text}
  val visibleFacts=story.world.facts + scopedVisible + protagonist.knowledge
  val allFacts=story.world.facts + story.world.scopedFacts.map{it.text}
  return NarrativePacket(
   PlayerNarrativeView(protagonistId,story.premise,visibleFacts,knownSecrets,visibleHidden,story.memory.recentEvents,beat),
   PrivateSimulationView(allFacts,story.world.secrets,story.world.hiddenEvents,story.world.pendingConsequences),
   listOf(
    "Never reveal information absent from the player view merely because it exists in simulation.",
    "Private simulation may shape causality, but hidden causes must appear only through observable effects until discovered.",
    "Never choose a significant action, belief, promise, confession or emotional commitment for the protagonist.",
    "Do not force menu choices; end with meaningful decision space whenever the scene calls for user agency.",
    "Match response intensity to narrative weight. Quiet moments may remain quiet.",
    "Preserve uncertainty: rumor, belief, testimony and objective truth are not interchangeable."
   )
  )
 }
}
