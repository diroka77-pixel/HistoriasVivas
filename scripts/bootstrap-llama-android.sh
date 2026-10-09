#!/usr/bin/env bash
set -euo pipefail
VERSION="b11429"
mkdir -p third_party
if [ ! -d third_party/llama.cpp/.git ]; then
  git clone --depth 1 --branch "$VERSION" https://github.com/ggml-org/llama.cpp.git third_party/llama.cpp
fi

AI_CHAT="third_party/llama.cpp/examples/llama.android/lib/src/main/cpp/ai_chat.cpp"

# The upstream Android example uses a very low temperature and does not enable
# a repetition penalty. Those defaults are unsuitable for narrative prose.
if grep -Fq "constexpr float DEFAULT_SAMPLER_TEMP    = 0.3f;" "$AI_CHAT"; then
  sed -i 's/constexpr float DEFAULT_SAMPLER_TEMP    = 0.3f;/constexpr float DEFAULT_SAMPLER_TEMP    = 0.7f;/' "$AI_CHAT"
fi

if ! grep -Fq "sparams.penalty_repeat = 1.12f;" "$AI_CHAT"; then
  sed -i '/sparams.temp = temp;/a\    sparams.penalty_repeat = 1.12f;\n    sparams.penalty_last_n = 128;' "$AI_CHAT"
fi

# Upstream double-counts the prompt length here, allowing far more output
# tokens than n_predict and causing slow, repetitive generations.
if grep -Fq "stop_generation_position = current_position + user_prompt_size + n_predict;" "$AI_CHAT"; then
  sed -i 's/stop_generation_position = current_position + user_prompt_size + n_predict;/stop_generation_position = current_position + n_predict;/' "$AI_CHAT"
fi

grep -Fq "constexpr float DEFAULT_SAMPLER_TEMP    = 0.7f;" "$AI_CHAT"
grep -Fq "sparams.penalty_repeat = 1.12f;" "$AI_CHAT"
grep -Fq "stop_generation_position = current_position + n_predict;" "$AI_CHAT"

echo "llama.cpp $VERSION ready with narrative generation fixes"
