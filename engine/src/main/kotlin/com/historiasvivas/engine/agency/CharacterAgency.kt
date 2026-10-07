package com.historiasvivas.engine.agency
import com.historiasvivas.engine.model.*

enum class AgencyKind { PURSUE_GOAL, PROTECT, CONFRONT, WITHDRAW, INVESTIGATE, RECONNECT }
data class AgencyIntent(
 val characterId:String,
 val kind:AgencyKind,
 val motivation:String,
 val targetCharacterId:String?=null,
 val priority:Int=0
)

class CharacterAgency {
 fun intents(story:Story):List<AgencyIntent> =
  story.world.characters.values.filter{it.alive}.flatMap{character->buildIntents(character)}.sortedByDescending{it.priority}

 private fun buildIntents(c:Character):List<AgencyIntent>{
  val result=mutableListOf<AgencyIntent>()
  val personality=c.mind.personality.dimensions
  val courage=personality["courage"]?:0
  val caution=personality["caution"]?:0
  val patience=personality["patience"]?:0
  val compassion=personality["compassion"]?:0
  c.goals.forEachIndexed{i,goal->
   result+=AgencyIntent(c.id,AgencyKind.PURSUE_GOAL,goal,priority=(70-i*5).coerceAtLeast(30))
  }
  c.mind.perceptions.values.forEach{perception->
   val danger=perception.impressions["dangerous"]?:0
   val trust=perception.impressions["trustworthy"]?:0
   if(danger>=35) result+=AgencyIntent(c.id,AgencyKind.WITHDRAW,"Considers target dangerous",perception.subjectId,(50+danger/2+caution/5-courage/6).coerceIn(20,95))
   if(trust<=-35) result+=AgencyIntent(c.id,AgencyKind.CONFRONT,"No longer trusts target",perception.subjectId,(55+(-trust)/2+courage/6-patience/6).coerceIn(20,95))
   if(trust>=35 || compassion>=65) result+=AgencyIntent(c.id,AgencyKind.RECONNECT,"Values or empathizes with target",perception.subjectId,(45+trust/3+compassion/6).coerceIn(20,90))
  }
  c.mind.beliefs.filter{it.confidence>=70}.forEach{belief->
   if(belief.claim.contains("unknown",true)||belief.claim.contains("mystery",true))
    result+=AgencyIntent(c.id,AgencyKind.INVESTIGATE,"Strong belief: "+belief.claim,priority=60)
  }
  return result
 }
}
