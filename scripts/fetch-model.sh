#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
model_path=app/src/main/assets/midas-small.onnx
model_sha=2d8c6cb8f415229daf1eb041024208e2608c9f98e17c81cc7c6ecb449c56fd58
mkdir -p app/src/main/assets
if [ -f "$model_path" ] && printf '%s  %s\n' "$model_sha" "$model_path" | sha256sum --check --status; then
    exit 0
fi
curl --fail --location --retry 3 --max-time 600 \
    https://github.com/isl-org/MiDaS/releases/download/v2_1/model-small.onnx \
    --output "$model_path.tmp"
printf '%s  %s\n' "$model_sha" "$model_path.tmp" | sha256sum --check
mv "$model_path.tmp" "$model_path"
