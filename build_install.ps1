# Arrête les processus qui verrouillent R.jar, compile, puis n'installe que si le build a réussi
$ErrorActionPreference = "Stop"
$adb = "C:\Android\Sdk\platform-tools\adb.exe"
$apk = Join-Path $PSScriptRoot "app\build\outputs\apk\debug\app-debug.apk"

Set-Location $PSScriptRoot

& .\gradlew.bat --stop | Out-Null
Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force
Remove-Item -LiteralPath "app\build" -Recurse -Force -ErrorAction SilentlyContinue

& .\gradlew.bat assembleDebug --no-daemon
if ($LASTEXITCODE -ne 0) {
    Write-Host "BUILD ÉCHOUÉ : rien n'a été installé." -ForegroundColor Red
    exit 1
}

& $adb install -r $apk
if ($LASTEXITCODE -eq 0) {
    Write-Host "Build et installation OK." -ForegroundColor Green
} else {
    Write-Host "Installation échouée." -ForegroundColor Red
    exit 1
}