package com.historiasvivas.engine.ai.local
import com.historiasvivas.engine.ai.NarrativePacket

class LocalNarrativePromptBuilder {
 fun build(packet:NarrativePacket,userAction:String):String = buildString {
  appendLine("Eres la voz literaria de Historias Vivas. Escribe en español natural y evocador.")
  appendLine("No eres el motor del mundo: no reveles información privada que el protagonista desconoce.")
  packet.rules.forEach { appendLine("- $it") }
  appendLine()
  appendLine("PREMISA: " + packet.player.premise)
  appendLine("HECHOS VISIBLES: " + packet.player.visibleFacts.joinToString(" | "))
  appendLine("SECRETOS CONOCIDOS: " + packet.player.knownSecrets.joinToString(" | ") { it.description })
  appendLine("EVENTOS RELEVANTES/RECIENTES: " + packet.player.recentVisibleEvents.joinToString(" | ") { it.summary })
  appendLine("PESO DE LA ESCENA: " + packet.player.narrativeBeat.weight)
  appendLine("MODO: " + packet.player.narrativeBeat.mode)
  appendLine()
  appendLine("ACCIÓN/INTENCIÓN DEL PROTAGONISTA: " + userAction)
  appendLine()
  appendLine("Continúa solo hasta el siguiente momento natural de agencia. No decidas por el protagonista qué hará después.")
 }
}
