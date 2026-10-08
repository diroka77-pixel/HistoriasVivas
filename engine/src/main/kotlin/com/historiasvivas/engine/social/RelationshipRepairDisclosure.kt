package com.historiasvivas.engine.social

import com.historiasvivas.engine.model.Story

data class RelationshipRepairDisclosureResult(
 val story:Story,
 val testimony:Testimony
)

class RelationshipRepairDisclosure {
 fun disclose(
  story:Story,
  attemptId:String,
  speakerId:String,
  turn:Long
 ):RelationshipRepairDisclosureResult?{
  val outcome=story.world.socialKnowledge.relationshipRepairs[attemptId]?:return null
  if(speakerId!=outcome.offenderId&&speakerId!=outcome.harmedId)return null
  val hiddenEventId="hidden-"+outcome.attemptId
  if(story.world.hiddenEvents.none{it.id==hiddenEventId&&speakerId in it.knownBy})return null
  val offenderName=story.world.characters[outcome.offenderId]?.name?:outcome.offenderId
  val harmedName=story.world.characters[outcome.harmedId]?.name?:outcome.harmedId
  val claim=when{
   speakerId==outcome.offenderId&&outcome.accepted->
    "I admitted what I did to "+harmedName+", and they accepted a limited repair."
   speakerId==outcome.offenderId->
    "I tried to make amends with "+harmedName+", but they did not accept it."
   outcome.accepted->
    offenderName+" admitted what happened, and I accepted a limited repair."
   else->
    offenderName+" tried to make amends, but I did not accept it."
  }
  val testimony=Testimony(
   id="repair-testimony-"+outcome.attemptId+"-"+speakerId+"-"+turn,
   speakerId=speakerId,
   claim=claim,
   subject=if(speakerId==outcome.offenderId)outcome.harmedId else outcome.offenderId,
   turn=turn,
   channel=InformationChannel.DIRECT_TESTIMONY,
   intent=TestimonyIntent.HONEST,
   privateTruth=story.world.hiddenEvents.first{it.id==hiddenEventId}.summary,
   reliability=80,
   relatedEventId=hiddenEventId
  )
  val social=story.world.socialKnowledge.copy(
   testimonies=story.world.socialKnowledge.testimonies+(testimony.id to testimony)
  )
  return RelationshipRepairDisclosureResult(
   story.copy(world=story.world.copy(socialKnowledge=social)),
   testimony
  )
 }
}
