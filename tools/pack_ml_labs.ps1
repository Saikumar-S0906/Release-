# Pack Wave M labs into zips for Colab / Release- main
# Usage (repo root):  .\tools\pack_ml_labs.ps1

$ErrorActionPreference = "Stop"
$tools = $PSScriptRoot

$shared = @(
    (Join-Path $tools "ml_train_presets.py"),
    (Join-Path $tools "real_data.py"),
    (Join-Path $tools "ml_ckpt.py"),
    (Join-Path $tools "colab_fetch.py"),
    (Join-Path $tools "ML_QUALITY_BAR.md")
)
foreach ($s in $shared) {
    if (-not (Test-Path $s)) { throw "missing shared $s" }
}

$labs = @(
    @{ Name = "inpaint"; Files = @("config.py","model.py","dataset.py","train.py","export_tflite.py","verify_contract.py","prepare_quality_data.py","README.md","train_colab.ipynb","data\README.md") },
    @{ Name = "depth";   Files = @("config.py","model.py","dataset.py","train.py","export_tflite.py","verify_contract.py","prepare_quality_data.py","README.md","train_colab.ipynb","data\README.md") },
    @{ Name = "matting"; Files = @("config.py","model.py","dataset.py","train.py","export_tflite.py","verify_contract.py","prepare_quality_data.py","README.md","train_colab.ipynb","data\README.md") },
    @{ Name = "flow";    Files = @("config.py","model.py","dataset.py","train.py","export_tflite.py","verify_contract.py","prepare_quality_data.py","README.md","train_colab.ipynb","data\README.md") }
)

foreach ($lab in $labs) {
    $dir = Join-Path $tools $lab.Name
    if (-not (Test-Path $dir)) { throw "missing lab $dir" }
    $zip = Join-Path $dir ($lab.Name + "_lab.zip")
    if (Test-Path $zip) { Remove-Item $zip -Force }
    $toZip = @()
    foreach ($f in $lab.Files) {
        $p = Join-Path $dir $f
        if (-not (Test-Path $p)) { throw "missing $p" }
        $toZip += $p
    }
    # Shared train helpers live one level up; Colab zips put them next to train.py
    # so `from real_data import ...` / `ml_train_presets` resolve via tools path OR cwd.
    # Copy into a temp sibling folder named so extract lands flat... actually Compress-Archive
    # preserves leaf names only when files share dirs — put shared copies into lab dir temporarily.
    $copied = @()
    foreach ($s in $shared) {
        $dest = Join-Path $dir (Split-Path $s -Leaf)
        if (-not (Test-Path $dest)) {
            Copy-Item $s $dest
            $copied += $dest
        }
        $toZip += $dest
    }
    Compress-Archive -Path ($toZip | Select-Object -Unique) -DestinationPath $zip -Force
    foreach ($c in $copied) { Remove-Item $c -Force }
    $len = (Get-Item $zip).Length
    Write-Host ("wrote {0} ({1} bytes)" -f $zip, $len)
}

Write-Host ""
Write-Host "Next: upload *_lab.zip + notebooks to Saikumar-S0906/Release- main."
Write-Host "Colab: inpaint (fixed) | depth (M2) | matting (M3) | flow (M4)."
