set -x
cd /app/lab/cand
H=https://huggingface.co
curl -sL -o migan.onnx $H/andraniksargsyan/migan/resolve/main/migan_pipeline_v2.onnx
curl -sL -o migan_raw.onnx $H/andraniksargsyan/migan/resolve/main/migan.onnx
curl -sL -o lama_fp32.onnx $H/Carve/LaMa-ONNX/resolve/main/lama_fp32.onnx
curl -sL -o modnet.onnx $H/Xenova/modnet/resolve/main/onnx/model.onnx
curl -sL -o neuflow_mixed.onnx $H/bukuroo/NeuFlowV2-ONNX/resolve/main/neuflow_mixed.onnx
python3 -c "from huggingface_hub import snapshot_download as s; s('depth-anything/Depth-Anything-V2-Small-hf',local_dir='dav2s')"
python3 -c "import torchvision; torchvision.models.optical_flow.raft_small(weights='DEFAULT')"
cd /app/lab/data
curl -sL -o p3m500p.parquet $H/datasets/nobg/P3M-10K/resolve/main/data/P3M_500_P-00000-of-00001.parquet
curl -sL -o nyu0.parquet "$H/datasets/vikhyatk/nyu_depth_v2/resolve/main/data/train-00000-of-00157-5cc7c41b0cc9e4f4.parquet"
echo FETCH_DONE
