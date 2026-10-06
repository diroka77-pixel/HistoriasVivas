package com.historiasvivas.engine.director

import com.historiasvivas.engine.model.*

class NarrativeDirector {
    fun dueConsequences(story: Story): List<Consequence> =
        story.world.pendingConsequences.filter { !it.resolved && it.earliestTurn <= story.turn }

    fun shouldConsiderEnding(story: Story): Boolean =
        story.status == StoryStatus.ACTIVE &&
        story.turn > 10 &&
        story.world.pendingConsequences.none { !it.resolved }
}
