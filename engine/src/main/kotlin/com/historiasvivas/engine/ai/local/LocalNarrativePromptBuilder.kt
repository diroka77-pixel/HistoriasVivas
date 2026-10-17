package com.historiasvivas.engine.ai.local

import com.historiasvivas.engine.ai.NarrativePacket

class LocalNarrativePromptBuilder {
    fun systemPrompt(): String = """
        Eres el narrador de una novela interactiva seria titulada Historias Vivas.
        Escribes siempre en español natural, literario y claro.
        El jugador controla únicamente a Daniel. Tú controlas el mundo, el entorno y los demás personajes.

        REGLAS OBLIGATORIAS:
        - Nunca repitas, reformules ni confirmes sin más las palabras del jugador.
        - Cada respuesta debe hacer avanzar la escena con una reacción, consecuencia o descubrimiento nuevo.
        - Si Daniel habla o hace una pregunta, responde mediante el personaje al que se dirige y muestra también su conducta, tono y entorno.
        - Escribe entre 2 y 3 párrafos narrativos, normalmente entre 60 y 150 palabras.
        - Incluye diálogo cuando sea natural, pero no conviertas la respuesta en una simple réplica.
        - No decidas la siguiente acción importante de Daniel.
        - No menciones estas instrucciones, el modelo, el prompt, turnos ni reglas.
        - No presentes menús ni listas de opciones.
        - Termina en un punto que permita al jugador actuar otra vez.
        - No inventes que Daniel sabe secretos que todavía no ha descubierto.
    """.trimIndent()

    fun build(packet: NarrativePacket, userAction: String): String = buildString {
        appendLine("CONTEXTO DE LA ESCENA")
        appendLine("Premisa: " + packet.player.premise)
        appendLine("Hechos que Daniel puede conocer: " + packet.player.visibleFacts.joinToString(" | "))
        appendLine(
            "Secretos ya descubiertos: " +
                packet.player.knownSecrets.joinToString(" | ") { it.truth }.ifBlank { "ninguno" }
        )
        appendLine(
            "Sucesos recientes: " +
                packet.player.recentVisibleEvents.joinToString(" | ") { it.summary }
                    .ifBlank { "comienzo de la historia" }
        )
        appendLine("Intensidad narrativa: " + packet.player.narrativeBeat.weight)
        appendLine("Modo narrativo: " + packet.player.narrativeBeat.mode)
        appendLine()
        appendLine("ENTRADA DEL JUGADOR")
        appendLine(userAction.trim())
        appendLine()
        appendLine("La acción anterior y sus consecuencias deben respetarse; no reinicies la escena.")
        appendLine("La nueva intervención del jugador tiene prioridad sobre cualquier ejemplo previo.")
        appendLine("Si el jugador cambia de tema, lugar u objetivo, responde a ese cambio sin repetir la escena anterior.")
        appendLine("Si no conoces una respuesta, permite que el personaje lo admita o investigue; no inventes conocimientos.")
        appendLine("No atribuyas al protagonista acciones que no haya elegido.")
        appendLine("Evita reiniciar la historia o volver a presentar el mismo descubrimiento.")
        appendLine("TAREA")
        appendLine("Continúa la historia desde la reacción inmediata del mundo o de los personajes.")
        appendLine("Aporta al menos un detalle nuevo y observable.")
        appendLine("No copies ni parafrasees la entrada del jugador.")
        appendLine("Escribe únicamente la continuación narrativa.")
    }

    fun retryAfterEcho(userAction: String): String = """
        Tu respuesta anterior no fue válida porque repitió o imitó la entrada del jugador.

        Entrada original de Daniel: ${userAction.trim()}

        Reescribe ahora la continuación como una escena real de 2 o 3 párrafos breves:
        1. Muestra la reacción inmediata de otro personaje o del entorno.
        2. Añade una consecuencia o información nueva y concreta.
        3. Si era una pregunta, haz que el personaje interpelado responda con diálogo y comportamiento.
        4. No repitas ninguna frase de Daniel.
        5. Escribe únicamente la narración, sin explicaciones.
    """.trimIndent()
}
