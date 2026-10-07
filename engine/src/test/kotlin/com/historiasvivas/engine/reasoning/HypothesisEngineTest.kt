package com.historiasvivas.engine.reasoning
import com.historiasvivas.engine.discovery.Evidence
import kotlin.test.*
class HypothesisEngineTest {
 @Test fun characterCannotReasonFromEvidenceTheyDoNotKnow(){
  val evidence=listOf(Evidence("e1","ruins-a","Marks on stone",80,2,"daniel"),Evidence("e2","ruins-b","Repeated pulse",80,3,"erik"))
  assertNull(HypothesisEngine().formulate("daniel","ruins","Something operates below",evidence,emptyList(),4))
 }
 @Test fun multipleKnownCluesCanCreateAndLaterDestroyHypothesis(){
  val e1=Evidence("e1","ruins-a","Marks on stone",75,2,"daniel")
  val e2=Evidence("e2","ruins-b","Repeated pulse",70,3,"daniel")
  val engine=HypothesisEngine(); val h=engine.formulate("daniel","ruins","The clues share a source",listOf(e1,e2),emptyList(),4)!!
  assertTrue(h.confidence>=40)
  val contradictions=listOf(
   Evidence("e3","ruins-c","Independent origin confirmed",95,8,"daniel"),
   Evidence("e4","ruins-d","Second independent origin confirmed",90,8,"daniel"),
   Evidence("e5","ruins-e","Timing rules out a common source",85,8,"daniel")
  )
  val revised=engine.update(h,emptyList(),contradictions,8)
  assertTrue(revised.confidence<h.confidence)
  assertTrue(revised.status==HypothesisStatus.WEAKENED||revised.status==HypothesisStatus.REJECTED)
 }
}