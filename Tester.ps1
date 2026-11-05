param([string]$Jdk = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
if (-not $Jdk) { throw 'Indiquez -Jdk avec le chemin du JDK.' }
& (Join-Path $PSScriptRoot 'Compiler.ps1') -Jdk $Jdk
Push-Location $PSScriptRoot
try {
    $testRoot = Join-Path ([IO.Path]::GetTempPath()) ('SuperCopyTests-' + [guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Force -Path $testRoot, 'build-tests' | Out-Null
    $sources = @(Get-ChildItem test\supercopy\*.java | ForEach-Object { $_.FullName })
    & "$Jdk\bin\javac.exe" -encoding UTF-8 -cp build -d build-tests $sources
    if ($LASTEXITCODE -ne 0) { throw 'Compilation des tests impossible.' }
    foreach ($testName in @('EngineTest', 'ServerTest', 'UiTest')) {
        & "$Jdk\bin\java.exe" -cp 'build;build-tests' "supercopy.$testName" $testRoot
        if ($LASTEXITCODE -ne 0) { throw "Echec : $testName" }
    }
    Write-Host "Tests reussis. Donnees de test conservees dans $testRoot"
} finally { Pop-Location }
