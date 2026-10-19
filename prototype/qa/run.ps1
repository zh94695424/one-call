$ErrorActionPreference = "Stop"

$prototypeRoot = Split-Path -Parent $PSScriptRoot
$fragmentPath = Join-Path $prototypeRoot "onecall-miniapp-pages.html"
$previewPath = Join-Path $prototypeRoot "preview.html"
$renderScript = "C:\Users\Administrator\.codex\plugins\cache\openai-bundled\visualize\1.0.49\skills\visualize\scripts\render.py"
$nodeExecutable = "C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe"
$qaScript = Join-Path $PSScriptRoot "qa.js"

python $renderScript $fragmentPath $previewPath --force
if ($LASTEXITCODE -ne 0) {
  throw "Failed to render the prototype preview."
}

& $nodeExecutable $qaScript
if ($LASTEXITCODE -ne 0) {
  throw "Prototype QA failed."
}
