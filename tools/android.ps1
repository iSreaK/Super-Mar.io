param(
    [ValidateSet('build', 'install', 'run', 'test', 'sha')]
    [string]$Action = 'build',
    [string]$Device,
    [switch]$Connected,
    [switch]$AllowPhoneInteraction
)
$ErrorActionPreference = 'Stop'
# Le drapeau exige un accord humain préalable ; il ne constitue pas cet accord.
if (($Action -eq 'run' -or ($Action -eq 'test' -and $Connected)) -and !$AllowPhoneInteraction) {
    throw 'Accord explicite préalable requis pour ouvrir le jeu ou tester sur le téléphone. Utiliser install pour mettre à jour uniquement.'
}
$taskProject = Split-Path -Parent $PSScriptRoot
$taskCache = 'C:\Android\Caches\MarioSprint'
$taskGradle = Join-Path $taskProject 'gradlew.bat'
function Invoke-Gradle([string[]]$Tasks) {
    & $taskGradle -p $taskProject --console=plain --project-cache-dir $taskCache @Tasks
    if ($LASTEXITCODE -ne 0) { throw 'La commande Gradle a échoué.' }
}
function Find-Adb {
    $taskSdk = $env:ANDROID_HOME
    if (!$taskSdk) { $taskSdk = 'C:\Android\Sdk' }
    $taskAdb = Join-Path $taskSdk 'platform-tools\adb.exe'
    if (!(Test-Path -LiteralPath $taskAdb)) { throw 'ADB introuvable. Renseigner ANDROID_HOME.' }
    return $taskAdb
}
function Invoke-Adb([string[]]$Arguments) {
    if (!$Device) { throw 'Indiquer le téléphone avec -Device NUMERO. Aucune détection automatique du téléphone.' }
    $taskAdb = Find-Adb
    & $taskAdb -s $Device @Arguments
    if ($LASTEXITCODE -ne 0) { throw 'La commande ADB a échoué.' }
}
switch ($Action) {
    'build' { Invoke-Gradle -Tasks @(':app:assembleDebug') }
    'sha' { Invoke-Gradle -Tasks @(':app:signingReport') }
    'test' {
        Invoke-Gradle -Tasks @(':app:testDebugUnitTest', ':app:lintDebug')
        if ($Connected) { Invoke-Gradle -Tasks @(':app:connectedDebugAndroidTest') }
    }
    { $_ -in 'install', 'run' } {
        if (!$Device) { throw 'Indiquer -Device NUMERO avant toute installation.' }
        Invoke-Gradle -Tasks @(':app:assembleDebug')
        Invoke-Adb -Arguments @('install', '-r', (Join-Path $taskProject 'app\build\outputs\apk\debug\app-debug.apk'))
        if ($Action -eq 'run') { Invoke-Adb -Arguments @('shell', 'am', 'start', '-n', 'fr.julie.mariosprint/.MainActivity') }
    }
}
