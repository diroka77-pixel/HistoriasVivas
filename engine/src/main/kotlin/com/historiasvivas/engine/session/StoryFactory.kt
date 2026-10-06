package com.historiasvivas.engine.session
import com.historiasvivas.engine.character.*
import com.historiasvivas.engine.model.*

data class StorySeed(
 val id:String,
 val title:String,
 val premise:String,
 val protagonistId:String,
 val protagonistName:String
)

class StoryFactory {
 fun create(seed:StorySeed):Story{
  require(seed.premise.isNotBlank()){"Premise cannot be blank"}
  require(seed.protagonistName.isNotBlank()){"Protagonist name cannot be blank"}
  val protagonist=Character(
   id=seed.protagonistId,
   name=seed.protagonistName,
   traits=setOf("protagonist"),
   mind=CharacterMind()
  )
  return Story(
   id=seed.id,title=seed.title,premise=seed.premise,
   world=WorldState(
    characters=mapOf(protagonist.id to protagonist),
    facts=setOf(seed.premise)
   )
  )
 }
}
