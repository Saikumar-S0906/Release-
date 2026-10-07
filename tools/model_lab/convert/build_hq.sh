# Build the flagship HQ tier (512² app tensors) into /app/lab/out/ml-packs-v2-hq
set -x
PY=/app/lab/.conv/bin/python
C=/app/lab/convert
O=/app/lab/out/hq
mkdir -p $O /app/lab/out/ml-packs-v2-hq && cd $O
$PY $C/convert_depth.py --weights /app/lab/kit/dav2s --io-edge 512 --model-edge 518 --out $O/depth_hq.tflite
$PY $C/quantize.py $O/depth_hq.tflite $O/depth_hq && rm -f $O/depth_hq.tflite
$PY $C/wrap_onnx.py matting /app/lab/cand/modnet.onnx $O/matting_hq 512
$PY -c "import onnxruntime as o;s=o.SessionOptions();s.graph_optimization_level=o.GraphOptimizationLevel.ORT_ENABLE_BASIC;s.optimized_model_filepath='$O/matting_hq_fold.onnx';o.InferenceSession('$O/matting_hq.onnx',s,providers=['CPUExecutionProvider'])"
$PY $C/o2t.py $O/matting_hq_fold.onnx $O/matting_hq_tf
$PY $C/quantize.py $O/matting_hq_tf/matting_hq_fold_float32.tflite $O/matting_hq && rm -rf $O/matting_hq_tf $O/*.onnx
while [ ! -s /app/lab/kit/weights/migan_512_places2.pt ] || ! grep -q OK /app/lab/gd512.log; do sleep 5; done
$PY $C/convert_migan.py --weights /app/lab/kit/weights/migan_512_places2.pt --res 512 --out $O/inpaint_hq.tflite
$PY $C/quantize.py $O/inpaint_hq.tflite $O/inpaint_hq && rm -f $O/inpaint_hq.tflite
cp $O/depth_hq_dyn8.tflite /app/lab/out/ml-packs-v2-hq/depth_hq.tflite
cp $O/matting_hq_dyn8.tflite /app/lab/out/ml-packs-v2-hq/matting_hq.tflite
cp $O/inpaint_hq_dyn8.tflite /app/lab/out/ml-packs-v2-hq/inpaint_hq.tflite
cd /app/lab/out/ml-packs-v2-hq && sha256sum *.tflite > SHA256SUMS && ls -la && cat SHA256SUMS
echo HQ_DONE
