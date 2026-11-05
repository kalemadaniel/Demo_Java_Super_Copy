param([string]$Jdk = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
if (-not $Jdk) { throw 'Indiquez le JDK : .\Compiler.ps1 -Jdk "C:\Program Files\Java\jdk1.8.0_92"' }
Push-Location $PSScriptRoot
try {
New-Item -ItemType Directory -Force -Path build | Out-Null
$sources = @(Get-ChildItem src\supercopy\*.java | ForEach-Object { $_.FullName })
& "$Jdk\bin\javac.exe" -encoding UTF-8 -source 8 -target 8 -d build $sources
if ($LASTEXITCODE -ne 0) { throw 'Compilation impossible.' }
& "$Jdk\bin\jar.exe" cfe Demo_Java_Super_Copy.jar supercopy.App -C build .
if ($LASTEXITCODE -ne 0) { throw 'Creation du JAR impossible.' }
Write-Host 'Demo_Java_Super_Copy.jar est pret.'
} finally { Pop-Location }
