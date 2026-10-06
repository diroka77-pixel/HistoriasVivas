package com.historiasvivas.engine
import com.historiasvivas.engine.ai.NarrativePacketBuilder
import com.historiasvivas.engine.narrative.NarrativeWeightEngine
import com.historiasvivas.engine.scenario.RuinsScenario
import kotlin.test.*

class NarrativePacketTest {
 @Test fun protagonistCannotSeeUndiscoveredSecrets(){
  val story=RuinsScenario.create()
  val beat=NarrativeWeightEngine().assess(story,"Miro las ruinas desde lejos.")
  val packet=NarrativePacketBuilder().build(story,"daniel",beat)
  assertTrue(packet.player.knownSecrets.isEmpty())
  assertEquals(2,packet.simulation.allSecrets.size)
  assertTrue(packet.simulation.allSecrets.any{it.id=="signal"})
  assertTrue(packet.simulation.allSecrets.any{it.id=="council"})
 }
 @Test fun secretKnownByAnotherCharacterStaysOutOfPlayerView(){
  val story=RuinsScenario.create()
  val beat=NarrativeWeightEngine().assess(story,"Pregunto a Erik qué piensa.")
  val packet=NarrativePacketBuilder().build(story,"daniel",beat)
  assertTrue(packet.simulation.allSecrets.first{it.id=="signal"}.knownBy.contains("erik"))
  assertFalse(packet.player.knownSecrets.any{it.id=="signal"})
 }
 @Test fun explicitDiscoveryMakesSecretNarratable(){
  val original=RuinsScenario.create()
  val revealed=original.world.secrets.map{if(it.id=="signal")it.copy(knownBy=it.knownBy+"daniel") else it}
  val story=original.copy(world=original.world.copy(secrets=revealed))
  val beat=NarrativeWeightEngine().assess(story,"Examino el transmisor.")
  val packet=NarrativePacketBuilder().build(story,"daniel",beat)
  assertTrue(packet.player.knownSecrets.any{it.id=="signal"})
 }
}
