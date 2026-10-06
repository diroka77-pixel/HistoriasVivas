package com.historiasvivas.engine.ai.local

object LocalNarratorProfiles {
 val QWEN3_4B_Q4_K_M = LocalModelProfile(
  id = "qwen3-4b-q4-k-m",
  displayName = "Qwen3 4B Q4_K_M",
  size = LocalModelSize.MEDIUM,
  contextWindow = 4096,
  estimatedRamMb = 3800,
  multilingual = true
 )
}

data class LocalInferenceMetrics(
 val modelId:String,
 val promptTokens:Int=0,
 val generatedTokens:Int=0,
 val loadMillis:Long=0,
 val firstTokenMillis:Long=0,
 val generationMillis:Long=0,
 val peakRamMb:Int?=null
){
 val tokensPerSecond:Double
  get() = if(generationMillis<=0) 0.0 else generatedTokens*1000.0/generationMillis
}
