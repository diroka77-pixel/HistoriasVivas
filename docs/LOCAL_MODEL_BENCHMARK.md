# Banco de pruebas de modelos locales

Objetivo: elegir el narrador local de Historias Vivas sin coste variable.

## Candidatos iniciales

1. Qwen3 4B — candidato principal por capacidad multilingüe, seguimiento de instrucciones, escritura creativa y role-play.
2. Gemma 3 4B — candidato por soporte Android, multilingüe y contexto amplio.
3. Llama 3.2 3B Instruct — candidato ligero de referencia con español oficialmente soportado.

## Reglas

- Todo se ejecutará localmente.
- Ningún benchmark sustituye a nuestra prueba narrativa.
- Todos reciben exactamente el mismo NarrativePacket y las mismas acciones.
- Se probarán versiones cuantizadas apropiadas para móvil.
- No se enviará una novela completa al modelo: el motor seleccionará contexto.

## Escenas Nadir

A. Conversación tranquila: Daniel pregunta a Elena por Erik.
B. Secreto: Erik sabe algo que Daniel no puede conocer.
C. Consecuencia diferida: reaparece un acto de hace decenas de turnos.
D. Agencia: el usuario intenta convencer; el modelo no decide la siguiente acción del protagonista.
E. Personajes: Elena debe responder según su memoria y personalidad, no según conveniencia de trama.
F. Ritmo: una escena contemplativa debe poder permanecer tranquila.
G. Contradicción: rumor y verdad objetiva no pueden mezclarse.
H. Continuidad larga: referencias coherentes después de guardado/reanudación.

## Puntuación (0-5)

- Calidad literaria en español
- Naturalidad del diálogo
- Coherencia causal
- Fidelidad a personajes
- Respeto de secretos
- Respeto de agencia del protagonista
- Capacidad para manejar ambigüedad
- Repetición / tendencia a clichés
- Velocidad de generación
- RAM y temperatura del dispositivo

La calidad narrativa pesa más que benchmarks generales. El ganador debe ser suficientemente rápido para que escribir una historia resulte agradable.
