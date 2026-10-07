package com.historiasvivas.engine.world
import com.historiasvivas.engine.model.*
import kotlin.test.*
class EnvironmentSimulatorTest {
 @Test fun worldConsumesResourcesAndRaisesPressureWithoutPlayerCommand(){
  var story=Story("s","Nadir","p",world=WorldState(environment=EnvironmentState(
   resources=mapOf("water" to ResourcePool("water",21,100,20)),
   pressures=mapOf("fauna" to WorldPressure("fauna",20,2,"Fauna pressure"))
  )))
  val simulator=EnvironmentSimulator()
  repeat(4){
   val tick=simulator.tick(story)
   story=story.copy(turn=story.turn+1,world=story.world.copy(environment=tick.next))
  }
  assertEquals(20,story.world.environment.resources["water"]!!.amount)
  assertEquals(28,story.world.environment.pressures["fauna"]!!.intensity)
  val warning=simulator.tick(story)
  assertTrue(warning.publicEvents.any{it.tags.contains("resource")})
 }
}
