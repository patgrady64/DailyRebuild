param(
    [string]$ProjectRoot = "C:\Users\iminv\Documents\pat\code\DailyRebuild"
)

$ErrorActionPreference = "Stop"

$PackageRoot = Join-Path $PSScriptRoot "COPY_TO_PROJECT"
$ReplacementSource = Join-Path $PackageRoot "app\src\main\java\com\pgdevhouse\dailyrebuild"
$TargetSource = Join-Path $ProjectRoot "app\src\main\java\com\pgdevhouse\dailyrebuild"
$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$BackupRoot = Join-Path (Split-Path $ProjectRoot -Parent) "DailyRebuild_Phase2_Backup_$Timestamp"

function Require-Path([string]$Path, [string]$Description) {
    if (-not (Test-Path -LiteralPath $Path)) {
        throw "$Description was not found: $Path"
    }
}

Require-Path $PackageRoot "Package folder"
Require-Path $ReplacementSource "Replacement dailyrebuild folder"
Require-Path $ProjectRoot "DailyRebuild project"
Require-Path $TargetSource "Current dailyrebuild folder"

Write-Host ""
Write-Host "DAILY REBUILD PHASE 2 INSTALLER" -ForegroundColor Cyan
Write-Host "Project: $ProjectRoot"
Write-Host "Backup:  $BackupRoot"
Write-Host ""
Write-Host "Close Android Studio before continuing." -ForegroundColor Yellow
$Answer = Read-Host "Type INSTALL to create a backup and replace the source folder"
if ($Answer -cne "INSTALL") {
    Write-Host "Cancelled. Nothing was changed."
    exit 0
}

New-Item -ItemType Directory -Path $BackupRoot -Force | Out-Null

# Back up the complete current source folder and the project files changed by Phase 2.
Copy-Item -LiteralPath $TargetSource -Destination (Join-Path $BackupRoot "dailyrebuild") -Recurse -Force
Copy-Item -LiteralPath (Join-Path $ProjectRoot "app\build.gradle.kts") -Destination $BackupRoot -Force
Copy-Item -LiteralPath (Join-Path $ProjectRoot "gradle.properties") -Destination $BackupRoot -Force
Copy-Item -LiteralPath (Join-Path $ProjectRoot "gradle\libs.versions.toml") -Destination $BackupRoot -Force

# Replace the complete source package. The backup remains outside app\src\main\java.
Remove-Item -LiteralPath $TargetSource -Recurse -Force
Copy-Item -LiteralPath $ReplacementSource -Destination $TargetSource -Recurse -Force

# Replace project configuration files.
Copy-Item -LiteralPath (Join-Path $PackageRoot "app\build.gradle.kts") -Destination (Join-Path $ProjectRoot "app\build.gradle.kts") -Force
Copy-Item -LiteralPath (Join-Path $PackageRoot "gradle.properties") -Destination (Join-Path $ProjectRoot "gradle.properties") -Force
Copy-Item -LiteralPath (Join-Path $PackageRoot "gradle\libs.versions.toml") -Destination (Join-Path $ProjectRoot "gradle\libs.versions.toml") -Force

# Add/replace only the Phase 2 domain tests. Other project tests are left alone.
$TestSource = Join-Path $PackageRoot "app\src\test\java\com\pgdevhouse\dailyrebuild\domain"
$TestTarget = Join-Path $ProjectRoot "app\src\test\java\com\pgdevhouse\dailyrebuild\domain"
New-Item -ItemType Directory -Path $TestTarget -Force | Out-Null
Copy-Item -Path (Join-Path $TestSource "*") -Destination $TestTarget -Recurse -Force

Write-Host ""
Write-Host "Phase 2 files installed successfully." -ForegroundColor Green
Write-Host "Backup created at: $BackupRoot"
Write-Host ""
Write-Host "Open Android Studio, wait for Gradle Sync, then run:" -ForegroundColor Cyan
Write-Host "  gradlew.bat --stop"
Write-Host "  gradlew.bat clean"
Write-Host "  gradlew.bat testDebugUnitTest --stacktrace"
Write-Host "  gradlew.bat :app:compileDebugKotlin --stacktrace"
Write-Host ""
Write-Host "Do not uninstall the app or clear its storage." -ForegroundColor Yellow
