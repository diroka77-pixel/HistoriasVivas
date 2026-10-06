package com.historiasvivas.engine.director
import com.historiasvivas.engine.model.*

class DirectorEngine {
 fun plan(story:Story,state:DirectorState):DirectorIntent {
  val due=story.world.pendingConsequences
   .filter{!it.resolved && it.earliestTurn<=story.turn+1}
   .minByOrNull{it.earliestTurn}
  val thread=state.threads
   .filter{it.status==ThreadStatus.OPEN || it.status==ThreadStatus.DORMANT}
   .maxByOrNull{it.urgency + ((story.turn-it.lastAdvancedTurn).coerceAtMost(30)).toInt()}
  val tooQuiet=state.quietTurns>=3
  val target=when {
   due!=null -> (state.tension+20).coerceAtMost(100)
   tooQuiet -> (state.tension+15).coerceAtMost(85)
   state.tension>=80 -> (state.tension-20).coerceAtLeast(35)
   else -> state.tension
  }
  return DirectorIntent(
   targetTension=target,
   threadToAdvance=thread?.id,
   consequenceToSurface=due?.id,
   allowQuietMoment=due==null && state.tension>=80
  )
 }
}
