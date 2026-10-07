package com.historiasvivas.engine.manuscript
import kotlin.test.*

class StoryManuscriptTest {
 @Test fun manuscriptKeepsEveryTurnLosslessly(){
  var book=StoryManuscript("nadir","Las ruinas de Nadir")
  repeat(250){i->
   book=book.append(ManuscriptEntry((i+1).toLong(),"acción $i","prosa completa $i",i.toLong()))
  }
  assertEquals(250,book.entries.size)
  assertEquals("acción 0",book.entries.first().userAction)
  assertEquals("prosa completa 249",book.entries.last().prose)
 }
 @Test fun serializationRoundTripPreservesUserAndNarratorText(){
  val persistence=ManuscriptPersistence()
  val original=StoryManuscript("nadir","Las ruinas de Nadir",listOf(ManuscriptEntry(1,"No entro en las ruinas.","La lluvia golpeaba Nadir.",123)))
  val restored=persistence.deserialize(persistence.serialize(original))
  assertEquals(original,restored)
  assertTrue(restored.plainText().contains("La lluvia golpeaba Nadir."))
 }
}
