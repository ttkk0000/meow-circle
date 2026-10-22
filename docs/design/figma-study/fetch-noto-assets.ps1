param([switch]$Force)
$ErrorActionPreference = 'Stop'
$taskRoot = $PSScriptRoot
$manifest = Get-Content -LiteralPath (Join-Path $taskRoot 'expression-library-manifest.json') -Raw | ConvertFrom-Json
$assetDir = Join-Path $taskRoot 'assets\noto-emoji'
New-Item -ItemType Directory -Path $assetDir -Force | Out-Null
$results = $manifest.assets | ForEach-Object -Parallel {
    $entry = $_
    $target = Join-Path $using:taskRoot $entry.file
    try {
        if (-not (Test-Path -LiteralPath $target) -or $using:Force) {
            Invoke-WebRequest -Uri $entry.source -OutFile $target -TimeoutSec 30
        }
        $content = Get-Content -LiteralPath $target -Raw
        if ($content -notmatch '<svg') { throw 'Expected SVG image' }
        [pscustomobject]@{key=$entry.key; ok=$true; bytes=(Get-Item -LiteralPath $target).Length}
    } catch { [pscustomobject]@{key=$entry.key; ok=$false; error=$_.Exception.Message} }
} -ThrottleLimit 8
$licensePath = Join-Path $assetDir 'CC-BY-4.0.txt'
if (-not (Test-Path -LiteralPath $licensePath)) {
    Invoke-WebRequest -Uri 'https://creativecommons.org/licenses/by/4.0/legalcode.txt' -OutFile $licensePath -TimeoutSec 30
}
$results | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $taskRoot 'expression-library-download.json') -Encoding utf8NoBOM
$failed = @($results | Where-Object { -not $_.ok })
[pscustomobject]@{count=@($results).Count; success=@($results | Where-Object ok).Count; failed=$failed} | ConvertTo-Json -Depth 4
if ($failed.Count) { exit 1 }
