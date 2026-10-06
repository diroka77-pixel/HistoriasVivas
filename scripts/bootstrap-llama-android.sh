#!/usr/bin/env bash
set -euo pipefail
VERSION="b11429"
mkdir -p third_party
if [ ! -d third_party/llama.cpp/.git ]; then
  git clone --depth 1 --branch "$VERSION" https://github.com/ggml-org/llama.cpp.git third_party/llama.cpp
fi
echo "llama.cpp $VERSION ready"
