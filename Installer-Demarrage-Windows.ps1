param([ValidateSet('Compte','Systeme')][string]$Mode = 'Compte')
$ErrorActionPreference = 'Stop'
# Run elevated. Password is prompted securely and passed only to Windows Task Scheduler.
$identity = [Security.Principal.WindowsIdentity]::GetCurrent()
$principal = New-Object Security.Principal.WindowsPrincipal($identity)
if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) { throw 'Ouvrez PowerShell en tant qu administrateur puis relancez ce script.' }
$java = (Get-Command java.exe -ErrorAction Stop).Source
$jar = Join-Path $PSScriptRoot 'Demo_Java_Super_Copy.jar'
if (-not (Test-Path -LiteralPath $jar)) { throw 'JAR manquant.' }
$taskName = 'Demo_Java_Super_Copy'
if (Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue) { throw 'Une tache porte deja ce nom. Verifiez-la dans le Planificateur avant de la remplacer.' }
$action = New-ScheduledTaskAction -Execute $java -Argument ('-cp "' + $jar + '" supercopy.Server') -WorkingDirectory $PSScriptRoot
$trigger = New-ScheduledTaskTrigger -AtStartup
$settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -RestartCount 999 -RestartInterval (New-TimeSpan -Minutes 1) -ExecutionTimeLimit ([TimeSpan]::Zero) -MultipleInstances IgnoreNew -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries
if ($Mode -eq 'Systeme') {
    $p = New-ScheduledTaskPrincipal -UserId 'SYSTEM' -LogonType ServiceAccount -RunLevel Highest
    Register-ScheduledTask -TaskName $taskName -Action $action -Trigger $trigger -Settings $settings -Principal $p -Description 'Copies periodiques sans suppression' | Out-Null
} else {
    $credential = Get-Credential -Message 'Compte Windows autorise a lire la source et ecrire dans la destination (reseau compris)'
    if (-not $credential) { throw 'Installation annulee.' }
    $p = New-ScheduledTaskPrincipal -UserId $credential.UserName -LogonType Password -RunLevel Highest
    $task = New-ScheduledTask -Action $action -Trigger $trigger -Settings $settings -Principal $p
    Register-ScheduledTask -TaskName $taskName -InputObject $task -User $credential.UserName -Password $credential.GetNetworkCredential().Password | Out-Null
    $credential = $null
}
Start-ScheduledTask -TaskName $taskName
Write-Host 'Demarrage automatique installe. Activez les copies depuis Lancer.bat.'
Write-Host 'Si un serveur est deja ouvert, redemarrez Windows pour utiliser le compte choisi.'
