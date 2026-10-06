package com.historiasvivas.engine.agency
import com.historiasvivas.engine.model.Story

class AgencySelector(private val agency:CharacterAgency=CharacterAgency()){
 fun select(story:Story,maxIntents:Int=3):List<AgencyIntent>{
  if(maxIntents<=0)return emptyList()
  return agency.intents(story).distinctBy{it.characterId to it.kind}.take(maxIntents)
 }
}
