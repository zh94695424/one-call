$ErrorActionPreference = 'Stop'

$prototypeRoot = Split-Path -Parent $PSScriptRoot
$sourcePath = Join-Path $prototypeRoot 'onecall-operations-admin.html'
$previewPath = Join-Path $prototypeRoot 'preview.html'

Copy-Item -LiteralPath $sourcePath -Destination $previewPath -Force
node (Join-Path $PSScriptRoot 'qa.js')

