package com.historiasvivas.engine.character
import kotlinx.serialization.Serializable

@Serializable data class PersonalityProfile(val dimensions:Map<String,Int> = emptyMap()){
 fun shifted(changes:Map<String,Int>)=copy(dimensions=(dimensions.keys+changes.keys).associateWith{((dimensions[it]?:0)+(changes[it]?:0)).coerceIn(-100,100)})
}
@Serializable data class Belief(val subject:String,val claim:String,val confidence:Int=50,val sourceEventId:String?=null)
@Serializable data class Perception(val observerId:String,val subjectId:String,val impressions:Map<String,Int> = emptyMap(),val notes:Set<String> = emptySet())
@Serializable data class EmotionalTrace(
 val eventId:String,
 val interpretation:String,
 val emotions:Map<String,Int> = emptyMap(),
 val salience:Int=50,
 val unresolved:Boolean=true,
 val revision:Int=0
)
@Serializable data class CharacterMind(
 val personality:PersonalityProfile=PersonalityProfile(),
 val beliefs:List<Belief> = emptyList(),
 val perceptions:Map<String,Perception> = emptyMap(),
 val personalMemories:Set<String> = emptySet(),
 val emotionalMemory:Map<String,EmotionalTrace> = emptyMap()
)
data class MindDelta(
 val characterId:String,
 val personalityShifts:Map<String,Int> = emptyMap(),
 val beliefsToAdd:List<Belief> = emptyList(),
 val memoriesToAdd:Set<String> = emptySet(),
 val emotionalTracesToAdd:List<EmotionalTrace> = emptyList()
)
data class PerceptionDelta(val observerId:String,val subjectId:String,val impressionShifts:Map<String,Int> = emptyMap(),val notesToAdd:Set<String> = emptySet())
data class EmotionalMemoryRevision(
 val characterId:String,val eventId:String,val newInterpretation:String?=null,
 val emotionShifts:Map<String,Int> = emptyMap(),val salienceShift:Int=0,val resolved:Boolean?=null
)
