package com.historiasvivas.engine.narrative
import com.historiasvivas.engine.model.Story

enum class NarrativeWeight { AMBIENT, MINOR, MEANINGFUL, MAJOR, DEFINING }
enum class ResponseMode { BREATHE, REACT_SUBTLY, DEVELOP, ESCALATE }

data class NarrativeBeat(
 val weight:NarrativeWeight,
 val mode:ResponseMode,
 val shouldOfferChoices:Boolean=false,
 val preservePlayerDecisionSpace:Boolean=true,
 val reason:String
)

class NarrativeWeightEngine {
 fun assess(story:Story,userAction:String):NarrativeBeat {
  val action=userAction.lowercase()
  val defining=listOf("mato ","kill ","abandono ","traiciono ","betray ","confieso ","confess ")
  val major=listOf("ataco ","attack ","robo ","steal ","salvo ","save ","amenazo ","threaten ")
  val meaningful=listOf("pregunto ","ask ","investigo ","investigate ","ayudo ","help ","miento ","lie ")
  return when {
   defining.any{it in action}->NarrativeBeat(NarrativeWeight.DEFINING,ResponseMode.ESCALATE,false,true,"Potentially identity-defining action")
   major.any{it in action}->NarrativeBeat(NarrativeWeight.MAJOR,ResponseMode.DEVELOP,false,true,"Action can materially change world or relationships")
   meaningful.any{it in action}->NarrativeBeat(NarrativeWeight.MEANINGFUL,ResponseMode.DEVELOP,false,true,"Action deserves room for character response")
   story.world.pendingConsequences.any{!it.resolved&&it.earliestTurn<=story.turn+1}->NarrativeBeat(NarrativeWeight.MINOR,ResponseMode.REACT_SUBTLY,false,true,"A prior consequence may surface naturally")
   else->NarrativeBeat(NarrativeWeight.AMBIENT,ResponseMode.BREATHE,false,true,"No forced escalation is needed")
  }
 }
}
