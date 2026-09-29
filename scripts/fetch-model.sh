#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p app/src/main/assets app/src/androidTest/assets
fetch_model() {
    local model_path="$1" model_sha="$2" model_url="$3"
    if [ -f "$model_path" ] && printf '%s  %s\n' "$model_sha" "$model_path" | sha256sum --check --status; then
        return
    fi
    curl --fail --location --retry 3 --max-time 600 "$model_url" --output "$model_path.tmp"
    printf '%s  %s\n' "$model_sha" "$model_path.tmp" | sha256sum --check
    mv "$model_path.tmp" "$model_path"
}
fetch_model app/src/main/assets/midas-small.onnx 2d8c6cb8f415229daf1eb041024208e2608c9f98e17c81cc7c6ecb449c56fd58 \
    https://github.com/isl-org/MiDaS/releases/download/v2_1/model-small.onnx

fetch_model app/src/main/assets/ssd-mobilenet.onnx b8fba5e404077d4048d27fcd1667e85e27e192eb9bf51e696c46a3acd7d21058 \
    https://media.githubusercontent.com/media/onnx/models/main/validated/vision/object_detection_segmentation/ssd-mobilenetv1/model/ssd_mobilenet_v1_12.onnx

# Reference photo belongs only to the instrumented test APK.
fetch_model app/src/androidTest/assets/dogs.jpg 78094cc48fbcfd9b6d321fe13619ecc72b65e006fc1b4c4458409ade9979486d \
    https://raw.githubusercontent.com/tensorflow/models/master/research/object_detection/test_images/image1.jpg
