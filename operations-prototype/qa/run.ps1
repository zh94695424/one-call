$ErrorActionPreference = 'Stop'

$prototypeRoot = Split-Path -Parent $PSScriptRoot
$repositoryRoot = Split-Path -Parent $prototypeRoot
$sourcePath = Join-Path $prototypeRoot 'onecall-operations-admin.html'
$previewPath = Join-Path $prototypeRoot 'preview.html'
$generatorPath = Join-Path $repositoryRoot 'shared\build-prototypes.js'

node $generatorPath
if ($LASTEXITCODE -ne 0) {
    throw "ONECALL business rule generation failed (exit $LASTEXITCODE)"
}
Copy-Item -LiteralPath $sourcePath -Destination $previewPath -Force
node (Join-Path $PSScriptRoot 'qa.js')
if ($LASTEXITCODE -ne 0) {
    throw "Operations prototype QA failed (exit $LASTEXITCODE)"
}

