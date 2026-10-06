# Arquitectura — Engine 0.1

Historias Vivas se divide en capas independientes:

1. **Domain** — mundo, personajes, relaciones, eventos, secretos y consecuencias.
2. **Memory** — memoria episódica, hechos canónicos y resúmenes de largo plazo.
3. **Director** — ritmo, conflictos, hilos abiertos y evaluación de consecuencias.
4. **AI Gateway** — contrato independiente del proveedor de IA.
5. **Persistence** — guardado y restauración del estado completo de una historia.
6. **Application** — casos de uso: crear historia, procesar acción, continuar y finalizar.

## Regla central

El texto generado nunca es la única fuente de verdad. Los hechos relevantes se consolidan en un estado estructurado del mundo para conservar coherencia entre sesiones largas.

## Objetivo de la prueba 0.1

Crear una historia, tomar decisiones, persistirla, reconstruirla en una nueva sesión y verificar que personajes, relaciones, secretos, acontecimientos y consecuencias siguen vigentes.


## Requisito de coste e inferencia local

Engine 0.1 se diseña con **cero coste variable por jugar** como requisito.

- La narración debe poder ejecutarse mediante un modelo local.
- El núcleo no depende de OpenAI, Gemini ni de ninguna API remota.
- Ningún proveedor con facturación por uso se activa por defecto.
- El modelo local es la voz; el motor conserva mundo, causalidad, memoria, personajes, secretos, agencia y continuidad.
- El runtime local es sustituible para poder comparar tecnologías y tamaños de modelo sin modificar el dominio.
- Se prioriza enviar al modelo solo el contexto narrativo necesario para reducir RAM, latencia y ventana de contexto.
- Los materiales de referencia incorporados al proyecto deberán ser de dominio público, propios o utilizables legalmente; se extraerán principios y contexto, no copias extensas de obras.
