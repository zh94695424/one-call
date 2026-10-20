param(
    [string]$QaScriptPath
)

$ErrorActionPreference = 'Stop'

$prototypeRoot = Split-Path -Parent $PSScriptRoot
$repositoryRoot = Split-Path -Parent $prototypeRoot
$sourcePath = Join-Path $prototypeRoot 'evidence-hub.html'
$previewPath = Join-Path $prototypeRoot 'preview.html'
$generatorPath = Join-Path $repositoryRoot 'shared\build-prototypes.js'
$resolvedQaScript = if ([string]::IsNullOrWhiteSpace($QaScriptPath)) { Join-Path $PSScriptRoot 'qa.js' } else { $QaScriptPath }

node $generatorPath
if ($LASTEXITCODE -ne 0) {
    throw "ONECALL business rule generation failed (exit $LASTEXITCODE)"
}
Copy-Item -LiteralPath $sourcePath -Destination $previewPath -Force
node $resolvedQaScript
$qaExitCode = $LASTEXITCODE
if ($qaExitCode -ne 0) {
    throw "Web prototype QA failed (exit $qaExitCode)"
}
