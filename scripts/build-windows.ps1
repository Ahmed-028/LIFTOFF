$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

$packageInput = Join-Path $projectRoot 'target\package-input'
$distribution = Join-Path $projectRoot 'dist'
$appImage = Join-Path $distribution 'LIFTOFF'
$icon = Join-Path $projectRoot 'src\main\resources\com\liftoff\art\liftoff.ico'

if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) {
    throw 'jpackage was not found. Install a JDK 21+ and add it to PATH.'
}

if (Test-Path $packageInput) {
    Remove-Item -Recurse -Force $packageInput
}
New-Item -ItemType Directory -Force -Path $packageInput | Out-Null

& .\mvnw.cmd -DskipTests package dependency:copy-dependencies `
    -DincludeScope=runtime "-DoutputDirectory=$packageInput"
if ($LASTEXITCODE -ne 0) {
    throw 'Maven could not build the game and collect its runtime dependencies.'
}
Copy-Item 'target\liftoff-0.1.0-SNAPSHOT.jar' (Join-Path $packageInput 'liftoff.jar')

if (Test-Path $appImage) {
    Remove-Item -Recurse -Force $appImage
}
New-Item -ItemType Directory -Force -Path $distribution | Out-Null

& jpackage --type app-image --input $packageInput --dest $distribution `
    --name LIFTOFF --main-jar liftoff.jar --main-class com.liftoff.Launcher `
    --icon $icon --app-version 0.1.0 --vendor 404PlanetNotFound `
    --add-modules java.base,java.desktop,java.logging,java.prefs,java.xml,java.datatransfer,java.scripting,java.sql,jdk.jfr,jdk.unsupported
if ($LASTEXITCODE -ne 0) {
    throw 'jpackage could not create the Windows app image.'
}

Write-Host "Windows app created: $(Join-Path $appImage 'LIFTOFF.exe')"
