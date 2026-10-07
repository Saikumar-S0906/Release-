# Rebuild the exact converter env used for the Model Lab TFLite exports (CPU only).
set -e
V=${1:-/opt/conv}
python3 -m venv $V
P="$V/bin/python -m pip install -q --no-cache-dir"
$P "torch==2.6.*" "torchvision==0.21.*" --index-url https://download.pytorch.org/whl/cpu
$P ai-edge-torch==0.4.0 "jax==0.4.38" "jaxlib==0.4.38" "transformers==4.46.3" "onnx==1.17.0" onnxruntime
$P --no-deps onnx2tf
$P "tf_keras==2.19.*" onnx_graphsurgeon sng4onnx psutil pillow
echo CONV_ENV_OK
