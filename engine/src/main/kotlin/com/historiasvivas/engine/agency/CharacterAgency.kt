package com.historiasvivas.engine.agency
import com.historiasvivas.engine.character.BeliefStatus
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.offscreen.DiscoverySources
import com.historiasvivas.engine.offscreen.KnowledgeGate

enum class AgencyKind { PURSUE_GOAL, PROTECT, CONFRONT, WITHDRAW, INVESTIGATE, RECONNECT }
data class AgencyIntent(
 val characterId:String,
 val kind:AgencyKind,
 val motivation:String,
 val targetCharacterId:String?=null,
 val priority:Int=0
)

class CharacterAgency(
 private val knowledge:KnowledgeGate=KnowledgeGate()
){
 fun intents(story:Story):List<AgencyIntent> =
  story.world.characters.values.filter{it.alive}.flatMap{character->buildIntents(character,story)}
   .sortedWith(
    compareByDescending<AgencyIntent>{it.priority}
     .thenBy{it.characterId}
     .thenBy{it.kind.name}
     .thenBy{it.targetCharacterId?:""}
     .thenBy{it.motivation}
   )

 private fun buildIntents(c:Character,story:Story):List<AgencyIntent>{
  val currentTurn=story.turn
  val result=mutableListOf<AgencyIntent>()
  val personality=c.mind.personality.dimensions
  val courage=personality["courage"]?:0
  val caution=personality["caution"]?:0
  val patience=personality["patience"]?:0
  val compassion=personality["compassion"]?:0
  val secrecy=personality["secrecy"]?:0
  c.goals.forEachIndexed{i,goal->
   result+=AgencyIntent(c.id,AgencyKind.PURSUE_GOAL,goal,priority=(70-i*5).coerceAtLeast(30))
  }
  c.mind.beliefRevisions.asSequence()
   .filter{it.confirmed!=null&&it.sourceCharacterId!=null&&it.turn==currentTurn}
   .groupBy{it.sourceCharacterId!!}
   .mapValues{(_,items)->items.maxBy{it.turn}}
   .forEach{(sourceId,revision)->
    val priority=(60+revision.previous.confidence/5+courage/5+caution/8-patience/8).coerceIn(35,95)
    val response=when{
     secrecy>=65->AgencyIntent(c.id,AgencyKind.INVESTIGATE,"Verifies future claims before revealing what was discovered",sourceId,priority)
     caution>=60&&courage<45->AgencyIntent(c.id,AgencyKind.WITHDRAW,"Keeps distance after discovering a false account",sourceId,priority)
     courage>=40&&patience<70->AgencyIntent(c.id,AgencyKind.CONFRONT,"Confronts the source of a disproven account",sourceId,priority)
     else->AgencyIntent(c.id,AgencyKind.INVESTIGATE,"Seeks independent confirmation after being misled",sourceId,priority)
    }
    result+=response
   }
  c.mind.perceptions.values.forEach{perception->
   val danger=perception.impressions["dangerous"]?:0
   val trust=perception.impressions["trustworthy"]?:perception.impressions["trust"]?:0
   if(danger>=35) result+=AgencyIntent(c.id,AgencyKind.WITHDRAW,"Considers target dangerous",perception.subjectId,(50+danger/2+caution/5-courage/6).coerceIn(20,95))
   if(trust<=-35) result+=AgencyIntent(c.id,AgencyKind.CONFRONT,"No longer trusts target",perception.subjectId,(55+(-trust)/2+courage/6-patience/6).coerceIn(20,95))
   if(trust>=35 || compassion>=65) result+=AgencyIntent(c.id,AgencyKind.RECONNECT,"Values or empathizes with target",perception.subjectId,(45+trust/3+compassion/6).coerceIn(20,90))
  }
  c.mind.beliefs.filter{it.status!=BeliefStatus.REJECTED&&it.confidence>=70}.forEach{belief->
   if(belief.claim.contains("unknown",true)||belief.claim.contains("mystery",true))
    result+=AgencyIntent(c.id,AgencyKind.INVESTIGATE,"Strong belief: "+belief.claim,priority=60)
  }
  story.world.socialKnowledge.heardClaims.values.asSequence()
   .filter{heard->
    val age=currentTurn-heard.turn
    heard.listenerId==c.id&&heard.channel==InformationChannel.RUMOR&&
     heard.confidence>=40&&age in 0L..3L
   }
   .groupBy{it.relatedEventId?:"testimony:"+it.testimonyId}
   .mapValues{(_,claims)->claims.maxBy{it.confidence}}
   .values
   .filterNot{heard->isGrounded(story,c.id,heard.relatedEventId)}
   .forEach{heard->
    result+=AgencyIntent(
     c.id,
     AgencyKind.INVESTIGATE,
     "Seeks independent confirmation of a recent uncertain report",
     priority=(45+heard.confidence/2+caution/5).coerceIn(45,85)
    )
   }
  return result
 }

 private fun isGrounded(story:Story,characterId:String,eventId:String?):Boolean{
  val event=eventId?.let{id->story.world.hiddenEvents.firstOrNull{it.id==id}}?:return false
  return knowledge.hasGroundedKnowledge(
   event,
   characterId,
   DiscoverySources(
    evidence=story.world.discovery.evidence,
    testimonies=story.world.socialKnowledge.testimonies,
    heardClaims=story.world.socialKnowledge.heardClaims
   )
  )
 }
}
