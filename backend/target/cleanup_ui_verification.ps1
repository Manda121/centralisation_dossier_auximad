$ErrorActionPreference='Stop'
$uiRoot='D:/Stage Auximad/centralisation_app'
$uiState=Get-Content -Raw -Encoding UTF8 (Join-Path $uiRoot 'backend/target/ui_runtime.json') | ConvertFrom-Json
if ($uiState.schema -notmatch '^ui_verification_[a-f0-9]{32}$') { throw 'Schéma de test non reconnu.' }
& 'C:/Users/Mandaniaina/AppData/Local/Author Software/nvm/installs/v24.20.0/node.exe' (Join-Path $uiRoot 'backend/target/close_ui_browser.mjs')
$uiJava=Get-CimInstance Win32_Process -Filter ('ProcessId = '+$uiState.javaPid) -ErrorAction SilentlyContinue
if ($uiJava -and $uiJava.CommandLine -like '*com.auximad.AuximadApplication*') {
    Stop-Process -Id $uiState.javaPid
    Wait-Process -Id $uiState.javaPid -Timeout 10 -ErrorAction SilentlyContinue
}
$uiProperties=@{}
Get-Content (Join-Path $uiRoot 'backend/src/main/resources/application.properties') | ForEach-Object {
    if ($_ -match '^(spring\.datasource\.[^=]+)=(.*)$') { $uiProperties[$matches[1]]=$matches[2].Trim() }
}
if ($uiProperties['spring.datasource.url'] -notmatch '^jdbc:postgresql://([^:/]+):(\d+)/([^?]+)') { throw 'URL non reconnue.' }
$uiDbHost=$matches[1]; $uiDbPort=$matches[2]; $uiDbName=$matches[3]
$env:PGPASSWORD=$uiProperties['spring.datasource.password']; $env:PGCLIENTENCODING='UTF8'
$ErrorActionPreference='Continue'
& 'C:/Program Files/PostgreSQL/17/bin/psql.exe' -w -h $uiDbHost -p $uiDbPort -U $uiProperties['spring.datasource.username'] -d $uiDbName -v ON_ERROR_STOP=1 -c ('SET client_min_messages TO warning; DROP SCHEMA IF EXISTS '+$uiState.schema+' CASCADE;') *> (Join-Path $uiRoot 'backend/target/ui_cleanup.log')
$uiResult=$LASTEXITCODE
$ErrorActionPreference='Stop'
$uiProfile=[IO.Path]::GetFullPath((Join-Path $uiRoot 'backend/target/ui-browser-profile'))
$uiTarget=[IO.Path]::GetFullPath((Join-Path $uiRoot 'backend/target'))+[IO.Path]::DirectorySeparatorChar
if (-not $uiProfile.StartsWith($uiTarget,[StringComparison]::OrdinalIgnoreCase) -or (Split-Path $uiProfile -Leaf) -ne 'ui-browser-profile') { throw 'Profil navigateur hors du répertoire de test.' }
if (Test-Path -LiteralPath $uiProfile) { Remove-Item -LiteralPath $uiProfile -Recurse -Force }
Write-Output 'Backend et navigateur de test arrêtés ; schéma isolé et profil temporaire supprimés.'
exit $uiResult
