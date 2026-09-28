# Wave M on Kaggle GPU (~30 hours / week)

Kaggle free GPU quota is typically **~30 hours per week**, then resets. Use it instead of Colab when Colab GPU is blocked.

## One-time setup

1. [kaggle.com](https://www.kaggle.com) → account verified (phone) so GPU unlocks  
2. New Notebook → **Code**  
3. Right panel → **Accelerator → GPU T4** (or P100)  
4. **Internet → On**  
5. Save

## Fastest path (upload notebook)

Download from Release- and **File → Import Notebook**:

| Pack | Notebook file on Release- |
|------|---------------------------|
| M1 Inpaint | `train_kaggle.ipynb` |
| M2 Depth | `train_depth_kaggle.ipynb` |
| M3 Matting | `train_matting_kaggle.ipynb` |
| M4 Flow | `train_flow_kaggle.ipynb` |

Raw (example M1):  
`https://raw.githubusercontent.com/Saikumar-S0906/Release-/main/train_kaggle.ipynb`

Then **Run All**.

## Resume next week / next session

`/kaggle/working` is wiped when the session ends unless you keep outputs:

1. After a good run: download `dist/*.ckpt.keras` + `train_state.json`  
   **or** Save Version → Save & Run All  
2. Create a private **Dataset** from those files  
3. Next notebook → Add Input → that dataset  
4. Re-run — the notebook auto-copies matching ckpt names into `dist/` and `--resume`s

## Tips

- Prefer **one pack at a time** on GPU so you don’t burn 30h on parallel jobs  
- M1 inpaint is heaviest — do it first while quota is full  
- Pin to Release- `ml-packs-v1` only if verify prints **SHIP_OK** / **OK**
