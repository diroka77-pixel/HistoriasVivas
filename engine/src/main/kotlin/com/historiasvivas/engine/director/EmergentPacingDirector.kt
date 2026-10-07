package com.historiasvivas.engine.director
import com.historiasvivas.engine.model.Story
import com.historiasvivas.engine.narrative.ResponseMode

enum class EmergentFocus { SURFACE_NOW, KEEP_LATENT, PROTECT_QUIET }
data class EmergentPacingDecision(val focus:EmergentFocus,val reason:String)

class EmergentPacingDirector {
 fun decide(story:Story,causal:CausalWorldResult,responseMode:ResponseMode):EmergentPacingDecision{
  if(causal.publicEvents.isEmpty()&&causal.hiddenEvents.isEmpty())return EmergentPacingDecision(EmergentFocus.KEEP_LATENT,"No emergent event")
  if(responseMode==ResponseMode.BREATHE && story.director.tension>=55)return EmergentPacingDecision(EmergentFocus.PROTECT_QUIET,"Current scene needs breathing room")
  if(causal.publicEvents.isNotEmpty() && story.director.tension<80)return EmergentPacingDecision(EmergentFocus.SURFACE_NOW,"Observable causal pressure is relevant now")
  return EmergentPacingDecision(EmergentFocus.KEEP_LATENT,"World event exists but does not need immediate focus")
 }
}
data class LatentResurfacingDecision(val event:com.historiasvivas.engine.model.StoryEvent?,val reason:String)

class LatentEventDirector(private val minimumAgeTurns:Long=2){
 fun choose(story:Story,responseMode:ResponseMode,alreadySurfacing:Boolean):LatentResurfacingDecision{
  if(alreadySurfacing)return LatentResurfacingDecision(null,"Current turn already surfaces an emergent event")
  if(responseMode==ResponseMode.BREATHE && story.director.tension>=55)return LatentResurfacingDecision(null,"Protect breathing room")
  val candidate=story.director.latentEmergentEvents.values
   .filter{story.turn-it.turn>=minimumAgeTurns}
   .sortedWith(compareBy<com.historiasvivas.engine.model.StoryEvent>{it.turn}.thenBy{it.id})
   .firstOrNull()
  return LatentResurfacingDecision(candidate,if(candidate==null)"No mature latent public event" else "Mature latent public event can resurface")
 }
}
