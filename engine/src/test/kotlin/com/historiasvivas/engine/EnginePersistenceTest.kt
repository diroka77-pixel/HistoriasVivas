package com.historiasvivas.engine

import com.historiasvivas.engine.director.NarrativeDirector
import com.historiasvivas.engine.memory.MemoryEngine
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.persistence.StoryPersistence
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EnginePersistenceTest {
    @Test
    fun consequencesAndCanonicalMemorySurviveReload() {
        val event = StoryEvent(
            id = "ev-abandon-erik", turn = 3,
            summary = "El protagonista abandona a Erik herido en el bosque.",
            participants = setOf("erik"), permanent = true
        )
        val consequence = Consequence(
            id = "con-erik-returns", causeEventId = event.id,
            description = "Erik puede regresar marcado por el abandono.", earliestTurn = 20
        )
        val memory = MemoryEngine().remember(StoryMemory(), event)
        val original = Story(
            id = "story-1", title = "Prueba", premise = "Una aventura.",
            turn = 20, memory = memory,
            world = WorldState(
                characters = mapOf("erik" to Character("erik", "Erik", traits = setOf("orgulloso"))),
                pendingConsequences = listOf(consequence),
                facts = setOf("Erik fue abandonado herido en el bosque")
            )
        )

        val persistence = StoryPersistence()
        val restored = persistence.deserialize(persistence.serialize(original))

        assertEquals(original, restored)
        assertTrue(restored.memory.canonicalEvents.any { it.id == event.id })
        assertTrue(NarrativeDirector().dueConsequences(restored).any { it.id == consequence.id })
    }
}
