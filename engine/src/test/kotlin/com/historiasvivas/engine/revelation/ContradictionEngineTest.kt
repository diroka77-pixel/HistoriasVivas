package com.historiasvivas.engine.revelation
import com.historiasvivas.engine.character.*
import com.historiasvivas.engine.discovery.Evidence
import com.historiasvivas.engine.model.Character
import kotlin.test.*

class ContradictionEngineTest {
 @Test fun ambiguousEvidenceCreatesDoubtNotArtificialRevelation(){
  val belief=Belief("erik:t1","There is nothing beneath the ruins",85,"t1")
  val evidence=Evidence("e1","signal","A faint unexplained vibration",25,20,"daniel")
  val c=ContradictionEngine().evaluate("daniel",belief,evidence,35,20,"erik")
  assertEquals(ContradictionOutcome.DOUBT,c.outcome)
  val revised=ContradictionEngine().beliefAfter(c)
  assertTrue(revised.confidence>50)
  assertEquals(BeliefStatus.DOUBTED,revised.status)
  assertNull(ContradictionEngine().revisionAfter(c,evidence.observation).confirmed)
 }
 @Test fun strongDirectEvidencePreservesHistoryAndAddsConfirmedTruth(){
  val belief=Belief("erik:t1","There is nothing beneath the ruins",60,"t1")
  val evidence=Evidence("e2","signal","A functioning transmitter is uncovered beneath the ruins",90,40,"daniel")
  val c=ContradictionEngine().evaluate("daniel",belief,evidence,90,40,"erik")
  assertEquals(ContradictionOutcome.REVELATION,c.outcome)

  val engine=RevelationEffectsEngine()
  val effects=engine.effects(c,evidence.observation)
  assertEquals(BeliefStatus.REJECTED,effects.updatedBelief.status)
  assertEquals(evidence.observation,effects.confirmedBelief?.claim)
  assertEquals(BeliefStatus.ACTIVE,effects.confirmedBelief?.status)
  assertEquals(belief,effects.beliefRevision.previous)
  assertTrue((effects.perceptionDelta!!.impressionShifts["trust"]?:0)<0)
  assertTrue(effects.emotionalTrace!!.salience>=90)
  assertTrue(effects.memoryNote!!.contains("erik"))

  val character=Character("daniel","Daniel",mind=CharacterMind(beliefs=listOf(belief)))
  val changed=CharacterMindReducer().apply(character,engine.toMindDelta(c,evidence.observation))
  assertEquals(2,changed.mind.beliefs.size)
  assertTrue(changed.mind.beliefs.any{it.claim==belief.claim&&it.status==BeliefStatus.REJECTED})
  assertTrue(changed.mind.beliefs.any{it.claim==evidence.observation&&it.status==BeliefStatus.ACTIVE})
  assertEquals(1,changed.mind.beliefRevisions.size)
  assertTrue(changed.mind.personalMemories.any{it.contains("account")&&it.contains("false")})
 }
 @Test fun repeatedApplicationDoesNotDuplicateRevisionHistory(){
  val belief=Belief("ruins","The ruins are empty",55,"t1")
  val evidence=Evidence("e3","signal","People are living in the ruins",95,12,"daniel")
  val c=ContradictionEngine().evaluate("daniel",belief,evidence,95,12,"elena")
  val delta=RevelationEffectsEngine().toMindDelta(c,evidence.observation)
  val reducer=CharacterMindReducer()
  val initial=Character("daniel","Daniel",mind=CharacterMind(beliefs=listOf(belief)))
  val once=reducer.apply(initial,delta)
  val twice=reducer.apply(once,delta)
  assertEquals(1,twice.mind.beliefRevisions.size)
  assertEquals(2,twice.mind.beliefs.size)
 }
}
