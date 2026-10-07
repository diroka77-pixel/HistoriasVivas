package com.historiasvivas.engine.director
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.world.*
import com.historiasvivas.engine.discovery.*
import com.historiasvivas.engine.spatial.*
import kotlin.test.*
class CausalEventDirectorTest {
 @Test fun oneWeakConditionDoesNotManufactureDrama(){
  val story=Story("s","Nadir","p",world=WorldState(environment=EnvironmentState(resources=mapOf("water" to ResourcePool("water",29,100,20)))))
  assertNull(CausalEventDirector().propose(story))
 }
 @Test fun criticalResourcesPlusHighFaunaCanReachCrisisThreshold(){
  val env=EnvironmentState(resources=mapOf("water" to ResourcePool("water",25,100,20)),pressures=mapOf("fauna" to WorldPressure("fauna",60,1,"Predators pressure supply routes")))
  val proposal=CausalEventDirector().propose(Story("s","Nadir","p",world=WorldState(environment=env)))
  assertEquals(EmergentEventKind.RESOURCE_CRISIS,proposal?.kind)
  assertTrue((proposal?.score?:0)>=65)
 }
 @Test fun convergingSocialCausesCanCreateEmergentConflict(){
  val traces=mapOf("a" to com.historiasvivas.engine.character.EmotionalTrace("a","betrayal",mapOf("anger" to 80),90),"b" to com.historiasvivas.engine.character.EmotionalTrace("b","fear",mapOf("fear" to 70),80))
  val erik=Character("erik","Erik",mind=com.historiasvivas.engine.character.CharacterMind(emotionalMemory=traces))
  val story=Story("s","Nadir","p",world=WorldState(characters=mapOf("erik" to erik),relationships=listOf(Relationship("erik","daniel",trust=-50))))
  val proposal=CausalEventDirector().propose(story)
  assertEquals(EmergentEventKind.SOCIAL_FRACTURE,proposal?.kind)
  assertTrue((proposal?.factors?.size?:0)>=2)
 }
 @Test fun cooldownPreventsConstantEscalation(){
  val traces=mapOf("a" to com.historiasvivas.engine.character.EmotionalTrace("a","betrayal",salience=90),"b" to com.historiasvivas.engine.character.EmotionalTrace("b","fear",salience=80))
  val erik=Character("erik","Erik",mind=com.historiasvivas.engine.character.CharacterMind(emotionalMemory=traces))
  val story=Story("s","Nadir","p",world=WorldState(characters=mapOf("erik" to erik),relationships=listOf(Relationship("erik","daniel",trust=-50))),turn=20)
  assertNull(CausalEventDirector().propose(story,lastEmergentTurn=19))
 }
 @Test fun persistentUnresolvedSignalsCanCreateDiscoveryOpportunityWithoutPrivateCause(){
  val signals=mapOf(
   "a" to WorldSignal("a","ruins",sense=SignalSense.RADIO,surface="Repeating pulse",hiddenCause="secret beacon",createdTurn=1,salience=80),
   "b" to WorldSignal("b","ruins",sense=SignalSense.TRACE,surface="Fresh machine marks",hiddenCause="hidden team",createdTurn=2,salience=75)
  )
  val story=Story("s","Nadir","p",world=WorldState(
   characters=mapOf("erik" to Character("erik","Erik")),
   discovery=DiscoveryState(signals=signals)
  ),turn=5)
  val proposal=CausalEventDirector().propose(story)!!
  assertEquals(EmergentEventKind.DISCOVERY_OPPORTUNITY,proposal.kind)
  assertFalse(proposal.summary.contains("secret beacon"))
  assertFalse(proposal.summary.contains("hidden team"))
 }
 @Test fun activeJourneyPlusStrongEnvironmentalPressureCreatesExpeditionDanger(){
  val env=EnvironmentState(pressures=mapOf("weather" to WorldPressure("weather",75,1,"Severe storm")))
  val spatial=SpatialState(
   positions=mapOf("erik" to CharacterPosition("erik","settlement")),
   routes=listOf(Route("settlement","ruins",3))
  )
  val started=SpatialEngine().startJourney(spatial,"erik","ruins",4)
  val story=Story("s","Nadir","p",world=WorldState(
   characters=mapOf("erik" to Character("erik","Erik")),environment=env,spatial=started
  ),turn=5)
  assertEquals(EmergentEventKind.EXPEDITION_DANGER,CausalEventDirector().propose(story)?.kind)
 }

}
