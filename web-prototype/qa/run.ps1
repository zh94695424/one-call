$ErrorActionPreference = 'Stop'

$prototypeRoot = Split-Path -Parent $PSScriptRoot
$sourcePath = Join-Path $prototypeRoot 'evidence-hub.html'
$previewPath = Join-Path $prototypeRoot 'preview.html'

Copy-Item -LiteralPath $sourcePath -Destination $previewPath -Force
node (Join-Path $PSScriptRoot 'qa.js')
