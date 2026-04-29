param(
  [ValidateSet("frontend", "backend", "all")]
  [string]$Target = "frontend",

  [string]$SshUser = "ubuntu",
  [string]$ServerIp = "43.139.76.37",
  [string]$FrontendDeployRoot = "/home/ubuntu/zhixi",
  [string]$BackendDeployRoot = "/home/ubuntu/apps/backend-api",
  [int]$BackendPort = 8080
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$websiteRoot = Join-Path $repoRoot "zhixi-website"
$frontendDeployScript = Join-Path $websiteRoot "scripts/deploy_to_server.sh"
$backendDeployScript = Join-Path $repoRoot "scripts/deploy_backend_api.sh"
$healthCheckScript = Join-Path $websiteRoot "scripts/health_check.sh"
$bashCommand = $null

function Assert-CommandExists {
  param([string]$CommandName)

  if (-not (Get-Command $CommandName -ErrorAction SilentlyContinue)) {
    throw "Required command not found: $CommandName"
  }
}

function Resolve-BashCommand {
  $candidates = @(
    "C:\Program Files\Git\bin\bash.exe",
    "C:\Program Files\Git\usr\bin\bash.exe",
    "C:\Program Files (x86)\Git\bin\bash.exe",
    "C:\Program Files (x86)\Git\usr\bin\bash.exe"
  )

  foreach ($candidate in $candidates) {
    if (Test-Path $candidate) {
      return $candidate
    }
  }

  $command = Get-Command bash -ErrorAction SilentlyContinue
  if ($command) {
    return $command.Source
  }

  throw "Required command not found: bash"
}

function Invoke-BashScript {
  param(
    [string]$ScriptPath,
    [string[]]$Arguments
  )

  if (-not (Test-Path $ScriptPath)) {
    throw "Script not found: $ScriptPath"
  }

  $argList = @($ScriptPath) + $Arguments
  & $bashCommand @argList
  if ($LASTEXITCODE -ne 0) {
    throw "Script failed: $ScriptPath"
  }
}

Assert-CommandExists "ssh"
Assert-CommandExists "scp"
$bashCommand = Resolve-BashCommand

Write-Host "==> Cloud preview target: $Target"
Write-Host "==> Server: $SshUser@$ServerIp"
Write-Host "==> Bash: $bashCommand"

if ($Target -in @("frontend", "all")) {
  Write-Host ""
  Write-Host "==> Deploying frontend..."
  Invoke-BashScript -ScriptPath $frontendDeployScript -Arguments @(
    $SshUser,
    $ServerIp,
    $FrontendDeployRoot
  )
}

if ($Target -in @("backend", "all")) {
  Write-Host ""
  Write-Host "==> Deploying backend..."
  Invoke-BashScript -ScriptPath $backendDeployScript -Arguments @(
    $SshUser,
    $ServerIp,
    $BackendDeployRoot,
    "$BackendPort"
  )
}

Write-Host ""
Write-Host "==> Running health checks..."
Invoke-BashScript -ScriptPath $healthCheckScript -Arguments @(
  "https://mashishi.com",
  "https://api.mashishi.com/api/health"
)

Write-Host ""
Write-Host "Cloud preview completed."
switch ($Target) {
  "frontend" {
    Write-Host "Refresh now: https://mashishi.com/#products"
  }
  "backend" {
    Write-Host "Verify now: https://api.mashishi.com/api/health"
  }
  "all" {
    Write-Host "Refresh both now:"
    Write-Host "  https://mashishi.com/#products"
    Write-Host "  https://api.mashishi.com/api/health"
  }
}
