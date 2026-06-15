# Builds a portable Windows ZIP from the Compose app image (GameLauncher.exe + bundled runtime).
# No installer — unzip and run. Does not require WiX.
# The zip contains a versioned top-level folder (GameLauncher-0.0.1-build42/) for side-by-side extracts.

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
if (-not (Test-Path $appImage)) {
    Write-Error "App image not found at $appImage"
}

$artifactVersion = (& .\gradlew.bat -q :composeApp:printArtifactVersion --no-daemon "-PbuildNumber=$BuildNumber").Trim()
$versionedFolderName = "GameLauncher-$artifactVersion"
$zipPath = Join-Path $LauncherRoot "GameLauncher-$artifactVersion-windows.zip"
if (Test-Path $zipPath) {
    Remove-Item $zipPath -Force
}

$stagingDir = Join-Path $LauncherRoot "composeApp\build\windows-portable-staging"
if (Test-Path $stagingDir) {
    Remove-Item $stagingDir -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $stagingDir | Out-Null

$versionedDir = Join-Path $stagingDir $versionedFolderName
Copy-Item -Path $appImage -Destination $versionedDir -Recurse

Write-Host "Creating portable ZIP $zipPath ($versionedFolderName/)..."
Compress-Archive -Path $versionedDir -DestinationPath $zipPath

Remove-Item $stagingDir -Recurse -Force

Write-Host "ZIP written to $zipPath"
