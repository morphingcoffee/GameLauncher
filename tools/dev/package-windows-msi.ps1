# Builds a Windows MSI using jpackage directly so icon/properties and WiX banner assets
# reach jpackage (Compose clears compose/tmp/resources).
# Do not add MsiInstallerStrings_en.wxl — jpackage duplicates localization and light fails.

param(
    [string]$BuildNumber = $env:BUILD_NUMBER,
    [switch]$Dev
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$LauncherRoot = Join-Path $RepoRoot "launcher"
Set-Location $LauncherRoot

if (-not $BuildNumber) {
    Write-Error "BUILD_NUMBER is required (workflow run number or -BuildNumber)."
}
if ($BuildNumber -notmatch '^\d+$') {
    Write-Error "BUILD_NUMBER must be a non-negative integer (jpackage MSI/DMG versions), got: '$BuildNumber'. If you passed -Dev via an array splat (@('-Dev')), PowerShell binds it positionally to BuildNumber — call the script with -Dev as a named switch instead."
}

# Sentry DSN is read by Gradle from SENTRY_DSN env (do not pass on the CLI — DSN contains '@').
$devProperty = @()
if ($Dev) {
    $devProperty = @("-PgameLauncherDev=true")
}

Write-Host "Packaging MSI (BuildNumber=$BuildNumber, Dev=$Dev)"
$gradleArgs = @(":composeApp:createDistributable", "--no-daemon", "-PbuildNumber=$BuildNumber") + $devProperty
& .\gradlew.bat @gradleArgs
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$packageName = (& .\gradlew.bat -q :composeApp:printAppPackageName --no-daemon "-PbuildNumber=$BuildNumber" @devProperty).Trim()
$artifactVersion = (& .\gradlew.bat -q :composeApp:printArtifactVersion --no-daemon "-PbuildNumber=$BuildNumber" @devProperty).Trim()
$upgradeUuid = (& .\gradlew.bat -q :composeApp:printWindowsUpgradeUuid --no-daemon "-PbuildNumber=$BuildNumber" @devProperty).Trim()
$appImage = Join-Path $LauncherRoot "composeApp\build\compose\binaries\main\app\$packageName"
$msiDir = Join-Path $LauncherRoot "composeApp\installer\windows\msi"
$resourceSourceDir = Join-Path $msiDir "jpackage"
$propertiesFileName = if ($Dev) { "GameLauncherDev.properties" } else { "GameLauncher.properties" }
$licenseFile = Join-Path $msiDir "installer-license.rtf"
$iconFile = Join-Path $LauncherRoot "composeApp\icons\icon.ico"
$destDir = Join-Path $LauncherRoot "composeApp\build\compose\binaries\main\msi"
$msiVersion = (& .\gradlew.bat -q :composeApp:printWindowsMsiProductVersion --no-daemon "-PbuildNumber=$BuildNumber" @devProperty).Trim()
$publishedMsi = Join-Path $LauncherRoot "GameLauncher-$artifactVersion.msi"

if (-not (Test-Path $appImage)) {
    Write-Error "App image not found at $appImage"
}
if (-not (Test-Path $resourceSourceDir)) {
    Write-Error "Installer resources not found at $resourceSourceDir"
}

$requiredSources = @(
    "GameLauncher.ico",
    $propertiesFileName,
    "installer-banner.bmp",
    "installer-dialog.bmp",
    "main.wxs",
    "overrides.wxi"
)
foreach ($fileName in $requiredSources) {
    $sourceFile = Join-Path $resourceSourceDir $fileName
    if (-not (Test-Path $sourceFile)) {
        Write-Error "Missing installer resource: $sourceFile"
    }
}

# Stage resources for jpackage. WiX light resolves WixUIBannerBmp at link time; relative
# filenames are not found under jpackage's temp config dir, so inject absolute BMP paths.
$stagingDir = Join-Path $LauncherRoot "composeApp\build\windows-jpackage-resources"
if (Test-Path $stagingDir) {
    Remove-Item $stagingDir -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $stagingDir | Out-Null

Copy-Item -Path (Join-Path $resourceSourceDir "GameLauncher.ico") -Destination (Join-Path $stagingDir "GameLauncher.ico")
Copy-Item -Path (Join-Path $resourceSourceDir $propertiesFileName) -Destination (Join-Path $stagingDir "$packageName.properties")
Copy-Item -Path (Join-Path $resourceSourceDir "overrides.wxi") -Destination (Join-Path $stagingDir "overrides.wxi")
Copy-Item -Path (Join-Path $resourceSourceDir "installer-banner.bmp") -Destination (Join-Path $stagingDir "installer-banner.bmp")
Copy-Item -Path (Join-Path $resourceSourceDir "installer-dialog.bmp") -Destination (Join-Path $stagingDir "installer-dialog.bmp")

$bannerPath = (Resolve-Path (Join-Path $stagingDir "installer-banner.bmp")).Path.Replace("\", "/")
$dialogPath = (Resolve-Path (Join-Path $stagingDir "installer-dialog.bmp")).Path.Replace("\", "/")

$mainContent = Get-Content -Path (Join-Path $resourceSourceDir "main.wxs") -Raw -Encoding UTF8
$mainContent = $mainContent.Replace('Value="installer-banner.bmp"', "Value=`"$bannerPath`"")
$mainContent = $mainContent.Replace('Value="installer-dialog.bmp"', "Value=`"$dialogPath`"")
Set-Content -Path (Join-Path $stagingDir "main.wxs") -Value $mainContent -Encoding utf8NoBOM

New-Item -ItemType Directory -Force -Path $destDir | Out-Null

$jpackage = Join-Path $env:JAVA_HOME "bin\jpackage.exe"
if (-not (Test-Path $jpackage)) {
    Write-Error "jpackage not found at $jpackage"
}

# JDK 25 jpackage shells out to WiX 3.x candle.exe/light.exe (or WiX 4/5 wix.exe).
# Prefer candle.exe already on PATH (GitHub windows-latest ships WiX 3.14). Fall back to
# the WiX 3.11 tree Gradle createDistributable may download — do not force 3.11 ahead of PATH.
$candleCmd = Get-Command candle.exe -ErrorAction SilentlyContinue
if (-not $candleCmd) {
    $wixCandidates = @(
        (Join-Path $LauncherRoot "build\wix311")
        (Join-Path $LauncherRoot "composeApp\build\wix311")
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
}
if ($candleCmd) {
    Write-Host "candle.exe: $($candleCmd.Source)"
} else {
    Write-Error "candle.exe not found on PATH after WiX setup"
}

Write-Host "Packaging MSI product version $msiVersion ($packageName) with branded WiX resources..."
Write-Host "Installer banner: $bannerPath"
Write-Host "Installer dialog: $dialogPath"

# --verbose surfaces candle/light diagnostics (e.g. LGHT****) on failure.
& $jpackage `
    --verbose `
    --type msi `
    --app-image $appImage `
    --resource-dir $stagingDir `
    --license-file $licenseFile `
    --name $packageName `
    --description Curated-indie-game-launcher `
    --vendor GameLauncher `
    --copyright GameLauncher `
    --app-version $msiVersion `
    --dest $destDir `
    --icon $iconFile `
    --win-menu `
    --win-menu-group $(if ($Dev) { "Game Launcher DEV" } else { "Game Launcher" }) `
    --win-shortcut `
    --win-dir-chooser `
    --win-upgrade-uuid $upgradeUuid

if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$builtMsi = Get-ChildItem -Path $destDir -Filter *.msi | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $builtMsi) {
    Write-Error "No MSI produced in $destDir"
}

Copy-Item -Path $builtMsi.FullName -Destination $publishedMsi -Force
Write-Host "MSI written to $publishedMsi"
