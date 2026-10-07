package com.historiasvivas.engine.memory
import com.historiasvivas.engine.model.*

data class RelevantMemory(val event:StoryEvent,val score:Int,val reasons:Set<String>)

class MemoryRelevanceEngine(private val limit:Int=10){
 fun select(story:Story,userAction:String):List<RelevantMemory>{
  val terms=tokens(userAction)
  val consequences=story.world.pendingConsequences.filter{!it.resolved}
  val all=(story.memory.canonicalEvents+story.memory.recentEvents).distinctBy{it.id}
  return all.mapNotNull{event->
   var score=0;val reasons=linkedSetOf<String>()
   val eventTerms=tokens(event.summary)+event.tags.flatMap(::tokens)
   val overlap=terms.intersect(eventTerms.toSet()).size
   if(overlap>0){score+=overlap*5;reasons+="tema"}
   if(event.participants.any{p->terms.any{it==p||story.world.characters[p]?.name?.lowercase()==it}}){score+=12;reasons+="personaje"}
   if(event.permanent){score+=8;reasons+="canon"}
   if(consequences.any{it.causeEventId==event.id}){score+=15;reasons+="consecuencia-pendiente"}
   val age=(story.turn-event.turn).coerceAtLeast(0)
   score+=(6-(age/20).toInt()).coerceAtLeast(0)
   if(score>0) RelevantMemory(event,score,reasons) else null
  }.sortedWith(compareByDescending<RelevantMemory>{it.score}.thenByDescending{it.event.turn}).take(limit)
 }
 private fun tokens(text:String)=text.lowercase().split(Regex("[^\\p{L}\\p{N}]+")).filter{it.length>=3}.toSet()
}
