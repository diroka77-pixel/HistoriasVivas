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