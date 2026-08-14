$ErrorActionPreference = 'SilentlyContinue'

function Stop-PortProcess([int]$port) {
    Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique |
        ForEach-Object { Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue }
}

Stop-PortProcess 8081
Stop-PortProcess 3005

Write-Host 'Stopped backend (8081) and frontend (3005).'
