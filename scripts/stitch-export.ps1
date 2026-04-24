#Requires -Version 5.1
<#
  Export Stitch screen HTML into this repo (default: web/_stitch_ref) using the
  official-style CLI: npx @_davideast/stitch-mcp tool get_screen_code

  Prereqs:
    - Node + npx
    - Auth: set STITCH_API_KEY, OR run once: npx @_davideast/stitch-mcp init

  Discover ids:
    npx -y @_davideast/stitch-mcp view --projects
    npx -y @_davideast/stitch-mcp view --project <project-id>

  Examples:
    $env:STITCH_API_KEY = "<from Stitch / Google Cloud>"
    .\scripts\stitch-export.ps1 -ProjectId "YOUR_PROJECT" -ScreenId "YOUR_SCREEN" -OutFile "web\_stitch_ref\profile.html"

  Bulk (map routes → HTML files) uses build_site:
    .\scripts\stitch-export.ps1 -Mode build_site -ProjectId "P" -RoutesJson '[{"screenId":"a","route":"/"},{"screenId":"b","route":"/x"}]' -OutDir "web\_stitch_ref"
#>
param(
    [ValidateSet("get_screen_code", "build_site")]
    [string]$Mode = "get_screen_code",
    [Parameter(Mandatory = $true)]
    [string]$ProjectId,
    [string]$ScreenId = "",
    [string]$OutFile = "",
    [string]$OutDir = "web/_stitch_ref",
    [string]$RoutesJson = ""
)

$ErrorActionPreference = "Stop"
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $repoRoot

function Write-Utf8NoBom([string]$Path, [string]$Content) {
    $enc = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText($Path, $Content, $enc)
}

function Try-Extract-Html([string]$Raw) {
    $t = $Raw.Trim()
    if (-not $t.StartsWith("{") -and -not $t.StartsWith("[")) { return $null }
    try {
        $o = $t | ConvertFrom-Json
    } catch {
        return $null
    }
    foreach ($name in @("html", "code", "content")) {
        if ($null -ne $o.$name -and $o.$name -is [string]) {
            $s = [string]$o.$name
            if ($s -match "<html|<!DOCTYPE") { return $s }
        }
    }
    if ($null -ne $o.data) {
        foreach ($name in @("html", "code")) {
            if ($null -ne $o.data.$name -and $o.data.$name -is [string]) {
                $s = [string]$o.data.$name
                if ($s -match "<html|<!DOCTYPE") { return $s }
            }
        }
    }
    return $null
}

if (-not $env:STITCH_API_KEY -and -not $env:STITCH_ACCESS_TOKEN) {
    Write-Host "Missing auth: set STITCH_API_KEY (or STITCH_ACCESS_TOKEN + Google project per stitch-mcp docs)." -ForegroundColor Yellow
    Write-Host "Setup: npx -y @_davideast/stitch-mcp init" -ForegroundColor Yellow
    exit 2
}

if ($Mode -eq "get_screen_code") {
    if (-not $ScreenId) {
        Write-Error "get_screen_code requires -ScreenId"
    }
    if (-not $OutFile) {
        Write-Error "get_screen_code requires -OutFile (e.g. web\_stitch_ref\profile.html)"
    }
    $outPath = Join-Path $repoRoot ($OutFile -replace "/", [IO.Path]::DirectorySeparatorChar)
    $parent = Split-Path $outPath -Parent
    if (-not (Test-Path $parent)) { New-Item -ItemType Directory -Path $parent -Force | Out-Null }

    $payload = (@{ projectId = $ProjectId; screenId = $ScreenId } | ConvertTo-Json -Compress)
    Write-Host "Calling: npx @_davideast/stitch-mcp tool get_screen_code ..."
    $raw = & npx -y @_davideast/stitch-mcp tool get_screen_code -d $payload 2>&1 | Out-String

    $html = Try-Extract-Html $raw
    if ($html) {
        Write-Utf8NoBom $outPath $html
        Write-Host "Wrote HTML: $outPath" -ForegroundColor Green
    } else {
        $dbg = $outPath + ".stitch-raw.txt"
        Write-Utf8NoBom $dbg $raw
        Write-Host "Could not parse HTML from CLI output. Full output saved to:" -ForegroundColor Yellow
        Write-Host $dbg
        exit 1
    }
    exit 0
}

if ($Mode -eq "build_site") {
    if (-not $RoutesJson) {
        Write-Error "build_site requires -RoutesJson (JSON array of {screenId, route})"
    }
    $outDirPath = Join-Path $repoRoot ($OutDir -replace "/", [IO.Path]::DirectorySeparatorChar)
    if (-not (Test-Path $outDirPath)) { New-Item -ItemType Directory -Path $outDirPath -Force | Out-Null }

    $routes = $RoutesJson | ConvertFrom-Json
    $body = @{ projectId = $ProjectId; routes = @($routes) } | ConvertTo-Json -Depth 10 -Compress
    Write-Host "Calling: npx @_davideast/stitch-mcp tool build_site ..."
    $raw = & npx -y @_davideast/stitch-mcp tool build_site -d $body 2>&1 | Out-String

    try {
        $o = $raw.Trim() | ConvertFrom-Json
    } catch {
        $dbg = Join-Path $outDirPath "build_site.stitch-raw.txt"
        Write-Utf8NoBom $dbg $raw
        Write-Host "build_site returned non-JSON. Saved: $dbg" -ForegroundColor Yellow
        exit 1
    }

    # Heuristic: pages[] with route + html, or dictionary keyed by route
    $written = 0
    if ($o.pages) {
        foreach ($p in $o.pages) {
            $route = [string]$p.route
            $html = [string]$p.html
            if (-not $route -or -not $html) { continue }
            $name = ($route -replace "^/", "" -replace "/", "_")
            if (-not $name) { $name = "index" }
            $fp = Join-Path $outDirPath ($name + ".html")
            Write-Utf8NoBom $fp $html
            $written++
        }
    }

    if ($written -eq 0) {
        $dbg = Join-Path $outDirPath "build_site.stitch-raw.txt"
        Write-Utf8NoBom $dbg $raw
        Write-Host "Could not find .pages[] in JSON. Saved raw output to: $dbg" -ForegroundColor Yellow
        exit 1
    }

    Write-Host "Wrote $written file(s) under $outDirPath" -ForegroundColor Green
    exit 0
}
