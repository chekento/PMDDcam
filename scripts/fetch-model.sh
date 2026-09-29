#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
model_path=app/src/main/assets/midas-small.onnx
model_sha=f319b72b1d6fc28097b1d6474a467d76aa44c156af283b752eb9c325a88a8af1
mkdir -p app/src/main/assets
if [ -f "$model_path" ] && printf '%s  %s\n' "$model_sha" "$model_path" | sha256sum --check --status; then
    exit 0
fi
curl --fail --location --retry 3 --max-time 600 \
    https://github.com/isl-org/MiDaS/releases/download/v2_1/model-small.onnx \
    --output "$model_path.tmp"
printf '%s  %s\n' "$model_sha" "$model_path.tmp" | sha256sum --check
mv "$model_path.tmp" "$model_path"
