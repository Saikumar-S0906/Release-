"""onnx2tf wrapper that skips its online test-image download (fails offline / pickle).

/tmp/conv/bin/python o2t.py model.onnx out_dir
"""
import sys

import numpy as np
import onnx2tf
import onnx2tf.onnx2tf as core
import onnx2tf.utils.common_functions as cf

fake = lambda *a, **k: np.random.rand(20, 128, 128, 3).astype(np.float32)
cf.download_test_image_data = fake
core.download_test_image_data = fake
onnx2tf.convert(input_onnx_file_path=sys.argv[1], output_folder_path=sys.argv[2],
                copy_onnx_input_output_names_to_tflite=True, not_use_onnxsim=True, non_verbose=True)
print("converted", sys.argv[2])
