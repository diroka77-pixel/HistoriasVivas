package com.historiasvivas.engine.agency
import kotlinx.serialization.Serializable

@Serializable data class AutonomousAgencyState(
 val plans:Map<String,AutonomousActionPlan> = emptyMap(),
 val results:Map<String,AutonomousActionResult> = emptyMap()
)