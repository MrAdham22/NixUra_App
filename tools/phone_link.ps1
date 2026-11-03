# Keeps USB phones linked to the local backend: re-runs `adb reverse` whenever a phone connects.
# The port comes from backend/.env (PORT), falling back to 8081. Leave this running; Ctrl+C stops it.
param([int]$Port = 0, [int]$IntervalSeconds = 3)

if ($Port -eq 0) {
    $Port = 8081
    $envFile = Join-Path $PSScriptRoot '..\backend\.env'
    if (Test-Path $envFile) {
        $line = Select-String -Path $envFile -Pattern '^PORT=(\d+)' | Select-Object -First 1
        if ($line) { $Port = [int]$line.Matches[0].Groups[1].Value }
    }
}

$adb = (Get-Command adb -ErrorAction SilentlyContinue).Source
if (-not $adb) {
    $sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA\Android\Sdk" }
    $adb = Join-Path $sdk 'platform-tools\adb.exe'
}
if (-not (Test-Path $adb)) { throw "adb not found at $adb" }

$rule = "tcp:$Port"
Write-Output "Watching for phones, linking port $Port. Press Ctrl+C to stop."

while ($true) {
    $serials = & $adb devices | Where-Object { $_ -match '\tdevice$' } | ForEach-Object { ($_ -split '\t')[0] }
    foreach ($serial in $serials) {
        $linked = (& $adb -s $serial reverse --list) -match "\s$rule\s+$rule$"
        if (-not $linked) {
            & $adb -s $serial reverse $rule $rule | Out-Null
            Write-Output "$(Get-Date -Format HH:mm:ss) linked $serial on port $Port"
        }
    }
    Start-Sleep -Seconds $IntervalSeconds
}
