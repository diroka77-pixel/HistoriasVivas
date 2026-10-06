package com.historiasvivas.engine.persistence

import com.historiasvivas.engine.model.Story
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class StoryPersistence {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    fun serialize(story: Story): String = json.encodeToString(story)
    fun deserialize(data: String): Story = json.decodeFromString(data)
}
