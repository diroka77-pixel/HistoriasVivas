package com.historiasvivas.engine.revelation
import com.historiasvivas.engine.character.*
import com.historiasvivas.engine.discovery.Evidence
import kotlin.test.*
class ContradictionEngineTest {
 @Test fun ambiguousEvidenceCreatesDoubtNotArtificialRevelation(){
  val belief=Belief("erik:t1","There is nothing beneath the ruins",85,"t1")
  val evidence=Evidence("e1","signal","A faint unexplained vibration",25,20,"daniel")
  val c=ContradictionEngine().evaluate("daniel",belief,evidence,35,20,"erik")
  assertEquals(ContradictionOutcome.DOUBT,c.outcome)
  assertTrue(ContradictionEngine().beliefAfter(c).confidence>50)
 }
 @Test fun strongDirectEvidenceCanBreakBeliefAndTrust(){
  val belief=Belief("erik:t1","There is nothing beneath the ruins",60,"t1")
  val evidence=Evidence("e2","signal","A functioning transmitter is uncovered beneath the ruins",90,40,"daniel")
  val c=ContradictionEngine().evaluate("daniel",belief,evidence,90,40,"erik")
  assertEquals(ContradictionOutcome.REVELATION,c.outcome)
  val effects=RevelationEffectsEngine().effects(c)
  assertTrue(effects.updatedBelief.confidence<belief.confidence)
  assertTrue((effects.perceptionDelta!!.impressionShifts["trust"]?:0)<0)
  assertTrue(effects.emotionalTrace!!.salience>=90)
 }
}