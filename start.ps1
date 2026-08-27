$ErrorActionPreference = 'Stop'

$root = $PSScriptRoot
$backend = Join-Path $root 'backend'
$frontend = Join-Path $root 'frontend'
$backendLog = Join-Path $backend 'run.log'
$backendErr = Join-Path $backend 'run.err'
$frontendLog = Join-Path $frontend 'dev.log'
$frontendErr = Join-Path $frontend 'dev.err'

function Test-Port([int]$port) {
    return [bool](Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue)
}

function Stop-PortProcess([int]$port) {
    Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique |
        ForEach-Object { Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue }
}

Write-Host 'Stopping old processes...'
Stop-PortProcess 8081
Stop-PortProcess 3005
Start-Sleep -Seconds 1

Write-Host 'Starting backend on 8081...'
Start-Process -FilePath 'cmd.exe' `
    -ArgumentList "/c cd /d `"$backend`" && mvn -q spring-boot:run" `
    -WorkingDirectory $backend `
    -RedirectStandardOutput $backendLog `
    -RedirectStandardError $backendErr `
    -WindowStyle Hidden

Write-Host 'Starting frontend on 3005...'
Start-Process -FilePath 'cmd.exe' `
    -ArgumentList "/c cd /d `"$frontend`" && npm run dev" `
    -WorkingDirectory $frontend `
    -RedirectStandardOutput $frontendLog `
    -RedirectStandardError $frontendErr `
    -WindowStyle Hidden

Write-Host 'Waiting for services...'
$backendReady = $false
$frontendReady = $false

for ($i = 0; $i -lt 60; $i++) {
    if (-not $backendReady -and (Test-Port 8081)) {
        $backendReady = $true
        Write-Host 'Backend is ready.'
    }
    if (-not $frontendReady -and (Test-Port 3005)) {
        $frontendReady = $true
        Write-Host 'Frontend is ready.'
    }
    if ($backendReady -and $frontendReady) {
        break
    }
    Start-Sleep -Seconds 1
}

if (-not ($backendReady -and $frontendReady)) {
    Write-Host 'Services did not become ready in time. Check logs:'
    Write-Host "  $backendLog"
    Write-Host "  $frontendLog"
    exit 1
}

Write-Host 'Opening http://localhost:3005'
Start-Process 'http://localhost:3005'
Write-Host 'All services are running.'
