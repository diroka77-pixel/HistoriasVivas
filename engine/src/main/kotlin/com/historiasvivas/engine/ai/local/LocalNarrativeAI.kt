package com.historiasvivas.engine.ai.local
import com.historiasvivas.engine.ai.*

enum class LocalModelSize { SMALL, MEDIUM, LARGE }
data class LocalModelProfile(
 val id:String,
 val displayName:String,
 val size:LocalModelSize,
 val contextWindow:Int,
 val estimatedRamMb:Int,
 val multilingual:Boolean=true
)
data class LocalGenerationConfig(
 val maxOutputTokens:Int=700,
 val temperature:Double=0.8,
 val topP:Double=0.95
)
interface LocalInferenceRuntime {
 suspend fun generate(packet:NarrativePacket,userAction:String,config:LocalGenerationConfig):NarrativeResponse
}
class LocalNarrativeAI(
 private val protagonistId:String,
 private val runtime:LocalInferenceRuntime,
 private val packetBuilder:NarrativePacketBuilder=NarrativePacketBuilder(),
 private val config:LocalGenerationConfig=LocalGenerationConfig()
):NarrativeAI{
 override suspend fun continueStory(request:NarrativeRequest):NarrativeResponse{
  val packet=packetBuilder.build(request.story,protagonistId,request.context.narrativeBeat)
  return runtime.generate(packet,request.userAction,config)
 }
}
