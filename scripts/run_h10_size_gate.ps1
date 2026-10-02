<#
.SYNOPSIS
  H10 classical CG size gate: assemble release APKs; DEX+ZIP+mapping scan + on-demand delivery assert.

.EXAMPLE
  .\scripts\run_h10_size_gate.ps1
  .\scripts\run_h10_size_gate.ps1 -SkipAssemble
#>
param(
    [switch]$SkipAssemble
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not $SkipAssemble) {
    Write-Host "Assembling release base + feature_cg + feature_titles3d..."
    & .\gradlew.bat :app:assembleRelease :feature_cg:assembleRelease :feature_titles3d:assembleRelease --console=plain
    if ($LASTEXITCODE -ne 0) { throw "assembleRelease failed ($LASTEXITCODE)" }
}

Write-Host "Running :app:reportH10SizeGate..."
& .\gradlew.bat :app:reportH10SizeGate --console=plain
if ($LASTEXITCODE -ne 0) { throw "reportH10SizeGate failed ($LASTEXITCODE)" }

Write-Host "H10 size gate done."
exit 0
