package com.historiasvivas.engine.director

import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.narrative.ResponseMode
import kotlin.test.*

class LatentEventDirectorTest {
 @Test fun maturePublicEventCanResurface(){
  val latent=StoryEvent("latent-1",5,"A distant tower collapses")
  val story=Story("s","Nadir","p",turn=7,director=DirectorState(latentEmergentEventIds=setOf(latent.id),latentEmergentEvents=mapOf(latent.id to latent)))
  val decision=LatentEventDirector().choose(story,ResponseMode.ADVANCE,false)
  assertEquals("latent-1",decision.event?.id)
 }

 @Test fun freshLatentEventWaits(){
  val latent=StoryEvent("latent-1",6,"A distant tower collapses")
  val story=Story("s","Nadir","p",turn=7,director=DirectorState(latentEmergentEvents=mapOf(latent.id to latent)))
  assertNull(LatentEventDirector().choose(story,ResponseMode.ADVANCE,false).event)
 }

 @Test fun latentEventDoesNotCompeteWithCurrentEmergentEvent(){
  val latent=StoryEvent("latent-1",5,"A distant tower collapses")
  val story=Story("s","Nadir","p",turn=9,director=DirectorState(latentEmergentEvents=mapOf(latent.id to latent)))
  assertNull(LatentEventDirector().choose(story,ResponseMode.ADVANCE,true).event)
 }

 @Test fun breathingRoomCanProtectAgainstLatentResurfacing(){
  val latent=StoryEvent("latent-1",5,"A distant tower collapses")
  val story=Story("s","Nadir","p",turn=9,director=DirectorState(tension=60,latentEmergentEvents=mapOf(latent.id to latent)))
  assertNull(LatentEventDirector().choose(story,ResponseMode.BREATHE,false).event)
 }
}
