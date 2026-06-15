# Builds a branded Windows MSI using jpackage directly so WiX overrides in
# composeApp/installer/windows/jpackage are applied (Compose clears compose/tmp/resources).

param(
    [string]$BuildNumber = $env:BUILD_NUMBER
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$LauncherRoot = Join-Path $RepoRoot "launcher"
Set-Location $LauncherRoot

if (-not $BuildNumber) {
    Write-Error "BUILD_NUMBER is required (workflow run number or -BuildNumber)."
}

$gradleArgs = @(":composeApp:createDistributable", "--no-daemon", "-PbuildNumber=$BuildNumber")
& .\gradlew.bat @gradleArgs
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$appImage = Join-Path $LauncherRoot "composeApp\build\compose\binaries\main\app\GameLauncher"
$resourceDir = Join-Path $LauncherRoot "composeApp\installer\windows\jpackage"
$licenseFile = Join-Path $LauncherRoot "composeApp\installer-license.rtf"
$iconFile = Join-Path $LauncherRoot "composeApp\icons\icon.ico"
$destDir = Join-Path $LauncherRoot "composeApp\build\compose\binaries\main\msi"
$msiVersion = (& .\gradlew.bat -q :composeApp:printWindowsMsiProductVersion --no-daemon "-PbuildNumber=$BuildNumber").Trim()

if (-not (Test-Path $appImage)) {
    Write-Error "App image not found at $appImage"
}
if (-not (Test-Path $resourceDir)) {
    Write-Error "Installer resources not found at $resourceDir"
}

New-Item -ItemType Directory -Force -Path $destDir | Out-Null

$jpackage = Join-Path $env:JAVA_HOME "bin\jpackage.exe"
if (-not (Test-Path $jpackage)) {
    Write-Error "jpackage not found at $jpackage"
}

# JDK 17 jpackage requires WiX 3.x candle.exe/light.exe. GitHub runners ship WiX 3.14+ on
# PATH, which can break jpackage's WiX sources (candle exit code 5). Prefer WiX 3.11
# downloaded by Compose during createDistributable.
$wixCandidates = @(
    (Join-Path $LauncherRoot "composeApp\build\wix311")
    (Join-Path $LauncherRoot "build\wix311")
)
foreach ($wixDir in $wixCandidates) {
    $candle = Join-Path $wixDir "candle.exe"
    if (Test-Path $candle) {
        $env:PATH = "$wixDir;$env:PATH"
        Write-Host "Using WiX from $wixDir"
        break
    }
}
$candleCmd = Get-Command candle.exe -ErrorAction SilentlyContinue
if ($candleCmd) {
    Write-Host "candle.exe: $($candleCmd.Source)"
} else {
    Write-Error "candle.exe not found on PATH after WiX setup"
}

Write-Host "Packaging MSI product version $msiVersion with branded WiX resources..."

& $jpackage `
    --type msi `
    --app-image $appImage `
    --resource-dir $resourceDir `
    --license-file $licenseFile `
    --name GameLauncher `
    --description "Desktop launcher for curated game builds and prototypes" `
    --vendor "Game Launcher" `
    --copyright "Game Launcher" `
    --app-version $msiVersion `
    --dest $destDir `
    --icon $iconFile `
    --win-menu `
    --win-menu-group "Game Launcher" `
    --win-shortcut `
    --win-dir-chooser `
    --win-upgrade-uuid "8f2a1b3c-4d5e-6f70-8a9b-0c1d2e3f4a5b"

if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "MSI written to $destDir"
