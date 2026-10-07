package com.historiasvivas.engine.director
import kotlinx.serialization.Serializable

@Serializable enum class ThreadStatus { OPEN, DORMANT, RESOLVED, FAILED }
@Serializable data class NarrativeThread(
 val id:String,
 val title:String,
 val status:ThreadStatus=ThreadStatus.OPEN,
 val urgency:Int=50,
 val lastAdvancedTurn:Long=0,
 val relatedCharacterIds:Set<String> = emptySet()
)
@Serializable data class DirectorState(
 val tension:Int=25,
 val threads:List<NarrativeThread> = emptyList(),
 val quietTurns:Int=0,
 val lastEmergentTurn:Long?=null,
 val latentEmergentEventIds:Set<String> = emptySet()
)
data class DirectorIntent(
 val targetTension:Int,
 val threadToAdvance:String?=null,
 val consequenceToSurface:String?=null,
 val allowQuietMoment:Boolean=false
)
