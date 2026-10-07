# Rebuild every Model Lab TFLite into /app/lab/out (needs /opt/conv from setup_conv_env.sh).
set -x
PY=/app/lab/.conv/bin/python
C=/app/lab/convert
O=/app/lab/out
mkdir -p $O && cd $O
$PY $C/convert_depth.py --weights /app/lab/kit/dav2s --out $O/depth_dav2s.tflite
$PY $C/quantize.py $O/depth_dav2s.tflite $O/depth_dav2s
$PY $C/convert_migan.py --weights /app/lab/kit/weights/migan_256_places2.pt --out $O/inpaint_migan256.tflite
$PY $C/quantize.py $O/inpaint_migan256.tflite $O/inpaint_migan256
$PY $C/wrap_onnx.py matting /app/lab/cand/modnet.onnx $O/matting_modnet || true
$PY - <<'P'
import onnxruntime as ort
o=ort.SessionOptions(); o.graph_optimization_level=ort.GraphOptimizationLevel.ORT_ENABLE_BASIC
o.optimized_model_filepath="/app/lab/out/matting_modnet_fold.onnx"
ort.InferenceSession("/app/lab/out/matting_modnet.onnx",o,providers=["CPUExecutionProvider"])
P
$PY $C/o2t.py $O/matting_modnet_fold.onnx $O/matting_modnet_tf
$PY $C/quantize.py $O/matting_modnet_tf/matting_modnet_fold_float32.tflite $O/matting_modnet
ls -la $O
echo REBUILD_DONE
