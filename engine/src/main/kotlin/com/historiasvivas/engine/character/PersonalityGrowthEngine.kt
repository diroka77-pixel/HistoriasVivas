package com.historiasvivas.engine.character
import com.historiasvivas.engine.model.Character

/**
 * Converts accumulated emotional experience into slow personality drift.
 * A single ordinary memory should not rewrite a person.
 */
class PersonalityGrowthEngine {
 fun derive(character:Character):Map<String,Int>{
  val traces=character.mind.emotionalMemory.values
  if(traces.isEmpty())return emptyMap()
  fun weighted(emotion:String)=traces.sumOf{(it.emotions[emotion]?:0)*it.salience/100}
  val shifts=mutableMapOf<String,Int>()
  val fear=weighted("fear"); val anger=weighted("anger"); val trust=weighted("trust")
  val compassion=weighted("compassion"); val guilt=weighted("guilt"); val hope=weighted("hope")
  if(kotlin.math.abs(fear)>=90) shifts["caution"]=(fear/90).coerceIn(-3,3)
  if(kotlin.math.abs(anger)>=100) shifts["patience"]=(-anger/100).coerceIn(-3,3)
  if(kotlin.math.abs(trust)>=100) shifts["trust"]=(trust/100).coerceIn(-3,3)
  if(kotlin.math.abs(compassion)>=100) shifts["compassion"]=(compassion/100).coerceIn(-3,3)
  if(kotlin.math.abs(guilt)>=110) shifts["self_doubt"]=(guilt/110).coerceIn(-3,3)
  if(kotlin.math.abs(hope)>=110) shifts["hope"]=(hope/110).coerceIn(-3,3)
  return shifts.filterValues{it!=0}
 }
 fun evolve(character:Character):Character{
  val shifts=derive(character)
  return if(shifts.isEmpty())character else character.copy(mind=character.mind.copy(personality=character.mind.personality.shifted(shifts)))
 }
}
