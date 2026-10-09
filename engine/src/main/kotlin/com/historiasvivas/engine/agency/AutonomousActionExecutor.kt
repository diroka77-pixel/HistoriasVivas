package com.historiasvivas.engine.agency
import kotlinx.serialization.Serializable
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.spatial.SpatialEngine
import com.historiasvivas.engine.discovery.DiscoveryEngine
import com.historiasvivas.engine.discovery.InvestigationEngine
import com.historiasvivas.engine.offscreen.*
import com.historiasvivas.engine.social.*

@Serializable enum class ActionExecutionStatus { STARTED, BLOCKED, DEFERRED }
@Serializable data class AutonomousActionResult(
 val planId:String,
 val status:ActionExecutionStatus,
 val reason:String,
 val event:StoryEvent?=null,
 val eventWitnessIds:Set<String> = emptySet()
)

class AutonomousActionExecutor(
 private val spatial:SpatialEngine=SpatialEngine(),
 private val discovery:DiscoveryEngine=DiscoveryEngine(),
 private val investigation:InvestigationEngine=InvestigationEngine(),
 private val knowledge:KnowledgeGate=KnowledgeGate(),
 private val evidenceBeliefs:EvidenceBeliefReconciliationEngine=EvidenceBeliefReconciliationEngine(),
 private val disclosure:DisclosureEngine=DisclosureEngine(),
 private val social:SocialInformationEngine=SocialInformationEngine()
){
 fun execute(story:Story,plan:AutonomousActionPlan):Pair<Story,AutonomousActionResult>{
  val c=story.world.characters[plan.characterId]
  if(c==null||!c.alive)return story to AutonomousActionResult(plan.id,ActionExecutionStatus.BLOCKED,"Character unavailable")
  return when(plan.kind){
   AutonomousActionKind.TRAVEL->{
    val to=plan.targetLocationId?:return story to AutonomousActionResult(plan.id,ActionExecutionStatus.BLOCKED,"No destination")
    val before=story.world.spatial; val after=spatial.startJourney(before,plan.characterId,to,story.turn)
    if(after==before) story to AutonomousActionResult(plan.id,ActionExecutionStatus.BLOCKED,"No viable route")
    else {
     val event=StoryEvent(plan.id+"-departure",story.turn,c.name+" leaves to pursue a private lead",setOf(c.id),setOf("autonomous","travel"))
     story.copy(world=story.world.copy(spatial=after)) to AutonomousActionResult(plan.id,ActionExecutionStatus.STARTED,"Journey started",event)
    }
   }
   AutonomousActionKind.INVESTIGATE->{
    val location=spatial.locationOf(story.world.spatial,plan.characterId)
    val visible=discovery.visibleAt(story.world.discovery,plan.characterId,location)
    val signal=plan.targetSignalId?.let{id->visible.firstOrNull{it.id==id}}?:visible.firstOrNull()
     ?:return story to AutonomousActionResult(plan.id,ActionExecutionStatus.BLOCKED,"No observable signal to investigate")
    val outcome=investigation.investigate(story.world.discovery,signal.id,plan.characterId,c.mind,story.turn)
     ?:return story to AutonomousActionResult(plan.id,ActionExecutionStatus.BLOCKED,"Investigation could not resolve")
    val event=StoryEvent(
     plan.id+"-investigation",story.turn,c.name+" investigates an observable anomaly",
     setOf(c.id),setOf("autonomous","investigation")
    )
    val hidden=signal.relatedEventId?.let{eventId->
     story.world.hiddenEvents.firstOrNull{it.id==eventId}
    }
    val hiddenEvents=hidden?.let{
     knowledge.discover(
      story.world.hiddenEvents,
      Discovery(
       it.id,c.id,DiscoveryChannel.INVESTIGATION,
       outcome.interpretation.hypothesis,outcome.evidence.id
      ),
      DiscoverySources(evidence=outcome.state.evidence)
     )
    }?:story.world.hiddenEvents
    val investigator=hidden?.let{
     c.copy(mind=evidenceBeliefs.integrate(
      c.mind,it.id,outcome.evidence,outcome.interpretation,
      story.world.socialKnowledge.testimonies,story.turn
     ))
    }?:c
    story.copy(world=story.world.copy(
     characters=story.world.characters+(c.id to investigator),
     discovery=outcome.state,
     hiddenEvents=hiddenEvents
    )) to AutonomousActionResult(
     plan.id,ActionExecutionStatus.STARTED,"Observable signal investigated",event
    )
   }
   AutonomousActionKind.WITHDRAW->{
    val from=spatial.locationOf(story.world.spatial,plan.characterId)
     ?:return story to AutonomousActionResult(plan.id,ActionExecutionStatus.BLOCKED,"Character is not at a stable location")
    val destination=story.world.spatial.routes.asSequence()
     .filter{!it.blocked}
     .mapNotNull{route->
      when{
       route.from==from->route.to
       route.bidirectional&&route.to==from->route.from
       else->null
      }
     }
     .filter{it!=plan.targetLocationId}
     .distinct().sorted().firstOrNull()
     ?:return story to AutonomousActionResult(plan.id,ActionExecutionStatus.BLOCKED,"No safe withdrawal route")
    val after=spatial.startJourney(story.world.spatial,plan.characterId,destination,story.turn)
    if(after==story.world.spatial) story to AutonomousActionResult(plan.id,ActionExecutionStatus.BLOCKED,"No viable withdrawal route")
    else{
     val event=StoryEvent(plan.id+"-withdrawal",story.turn,c.name+" withdraws from a perceived threat",setOf(c.id),setOf("autonomous","withdraw"))
     story.copy(world=story.world.copy(spatial=after)) to AutonomousActionResult(plan.id,ActionExecutionStatus.STARTED,"Withdrawal started",event)
    }
   }
   AutonomousActionKind.WARN->{
    val hypothesis=story.world.reasoning.hypotheses[plan.hypothesisId]
     ?:return story to AutonomousActionResult(plan.id,ActionExecutionStatus.BLOCKED,"Hypothesis unavailable")
    val location=spatial.locationOf(story.world.spatial,plan.characterId)
     ?:return story to AutonomousActionResult(plan.id,ActionExecutionStatus.BLOCKED,"Speaker is not at a stable location")
    val listenerId=plan.targetCharacterId?:spatial.presentAt(story.world.spatial,location)
     .filter{it!=plan.characterId&&story.world.characters[it]?.alive==true}.sorted().firstOrNull()
     ?:return story to AutonomousActionResult(plan.id,ActionExecutionStatus.BLOCKED,"No listener present")
    if(listenerId !in spatial.presentAt(story.world.spatial,location))
     return story to AutonomousActionResult(plan.id,ActionExecutionStatus.BLOCKED,"Listener is not present")
    val listener=story.world.characters[listenerId]
     ?:return story to AutonomousActionResult(plan.id,ActionExecutionStatus.BLOCKED,"Listener unavailable")
    val decision=disclosure.decide(story,plan.characterId,listenerId,hypothesis)
     ?:return story to AutonomousActionResult(plan.id,ActionExecutionStatus.BLOCKED,"Disclosure unavailable")
    val testimony=disclosure.toTestimony(decision,hypothesis)
     ?:return story to AutonomousActionResult(plan.id,ActionExecutionStatus.DEFERRED,"Speaker delays warning")
    val heard=social.hear(testimony,listenerId,listener.mind)
    val testimonies=story.world.socialKnowledge.testimonies+(testimony.id to testimony)
    val updatedListener=listener.copy(mind=social.integrateBelief(
     listener.mind,heard,testimonies,story.turn
    ))
    val knowledge=story.world.socialKnowledge.copy(
     testimonies=testimonies,
     heardClaims=story.world.socialKnowledge.heardClaims+(testimony.id+":"+listenerId to heard),
     disclosureHistory=story.world.socialKnowledge.disclosureHistory+(decision.id to decision)
    )
    val event=StoryEvent(plan.id+"-warning",story.turn,c.name+" warns "+listener.name+" about a perceived danger",setOf(c.id,listenerId),setOf("autonomous","warning"))
    story.copy(world=story.world.copy(characters=story.world.characters+(listenerId to updatedListener),socialKnowledge=knowledge)) to
     AutonomousActionResult(
      plan.id,ActionExecutionStatus.STARTED,"Warning delivered",event,
      eventWitnessIds=setOf(c.id,listenerId)
     )
   }
   AutonomousActionKind.HIDE_EVIDENCE->{
    val location=spatial.locationOf(story.world.spatial,plan.characterId)
     ?:return story to AutonomousActionResult(plan.id,ActionExecutionStatus.BLOCKED,"Character is not at a stable location")
    val signal=plan.targetSignalId?.let{story.world.discovery.signals[it]}
     ?.takeIf{it.locationId==location&&it.salience>0}
     ?:return story to AutonomousActionResult(plan.id,ActionExecutionStatus.BLOCKED,"No hypothesis-related signal to conceal")
    val concealed=signal.copy(
     surface="The remaining traces here are difficult to interpret.",
     salience=(signal.salience-60).coerceAtLeast(0),
     revision=signal.revision+1,
     lastChangedTurn=story.turn
    )
    val state=story.world.discovery.copy(signals=story.world.discovery.signals+(signal.id to concealed))
    val event=StoryEvent(plan.id+"-concealment",story.turn,c.name+" conceals an observable trace",setOf(c.id),setOf("autonomous","concealment"))
    story.copy(world=story.world.copy(discovery=state)) to
     AutonomousActionResult(plan.id,ActionExecutionStatus.STARTED,"Observable trace concealed",event)
   }
   AutonomousActionKind.PROTECT->{
    val targetId=plan.targetCharacterId
     ?:return story to AutonomousActionResult(plan.id,ActionExecutionStatus.BLOCKED,"No character to protect")
    val target=story.world.characters[targetId]
     ?:return story to AutonomousActionResult(plan.id,ActionExecutionStatus.BLOCKED,"Protected character unavailable")
    if(!target.alive)return story to AutonomousActionResult(plan.id,ActionExecutionStatus.BLOCKED,"Protected character unavailable")
    val from=spatial.locationOf(story.world.spatial,plan.characterId)
     ?:return story to AutonomousActionResult(plan.id,ActionExecutionStatus.BLOCKED,"Protector is not at a stable location")
    val targetLocation=spatial.locationOf(story.world.spatial,targetId)
     ?:return story to AutonomousActionResult(plan.id,ActionExecutionStatus.DEFERRED,"Protected character is travelling")
    if(from==targetLocation)return story to AutonomousActionResult(plan.id,ActionExecutionStatus.DEFERRED,"Protector is already with protected character")
    val after=spatial.startJourney(story.world.spatial,plan.characterId,targetLocation,story.turn)
    if(after==story.world.spatial)return story to AutonomousActionResult(plan.id,ActionExecutionStatus.BLOCKED,"Protected character is not reachable")
    val event=StoryEvent(plan.id+"-protective-travel",story.turn,c.name+" moves to protect "+target.name,setOf(c.id,targetId),setOf("autonomous","protect","travel"))
    story.copy(world=story.world.copy(spatial=after)) to
     AutonomousActionResult(plan.id,ActionExecutionStatus.STARTED,"Protective journey started",event)
   }
  }
 }
}