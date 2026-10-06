package com.historiasvivas.lab
import com.arm.aichat.InferenceEngine
import com.historiasvivas.engine.ai.*
import com.historiasvivas.engine.ai.local.*
import kotlinx.coroutines.flow.toList

class LlamaCppNarrativeRuntime(
 private val engine:InferenceEngine,
 private val promptBuilder:LocalNarrativePromptBuilder=LocalNarrativePromptBuilder()
):LocalInferenceRuntime{
 override suspend fun generate(packet:NarrativePacket,userAction:String,config:LocalGenerationConfig):NarrativeResponse{
  val prompt=promptBuilder.build(packet,userAction)
  val prose=engine.sendUserPrompt(prompt,config.maxOutputTokens).toList().joinToString("")
  return NarrativeResponse(
   prose=prose.trim(),
   eventSummary="Daniel actuó: "+userAction.take(180),
   participantIds=setOf(packet.player.protagonistId),
   tags=setOf("local-narrative")
  )
 }
}
