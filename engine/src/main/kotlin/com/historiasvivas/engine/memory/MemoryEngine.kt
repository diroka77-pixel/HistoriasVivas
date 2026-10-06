package com.historiasvivas.engine.memory

import com.historiasvivas.engine.model.*

class MemoryEngine(private val recentLimit: Int = 12) {
    fun remember(memory: StoryMemory, event: StoryEvent): StoryMemory {
        val recent = (memory.recentEvents + event).takeLast(recentLimit)
        val canonical = if (event.permanent && memory.canonicalEvents.none { it.id == event.id })
            memory.canonicalEvents + event else memory.canonicalEvents
        return memory.copy(recentEvents = recent, canonicalEvents = canonical)
    }
}
