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
