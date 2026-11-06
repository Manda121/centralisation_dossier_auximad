$ErrorActionPreference='Stop'
$uiRoot='D:/Stage Auximad/centralisation_app'
$uiState=Get-Content -Raw -Encoding UTF8 (Join-Path $uiRoot 'backend/target/ui_runtime.json') | ConvertFrom-Json
$uiSchema=$uiState.schema
if ($uiSchema -notmatch '^ui_verification_[a-f0-9]{32}$') { throw 'Seul le schéma UI isolé peut être rechargé.' }
$uiProperties=@{}
Get-Content (Join-Path $uiRoot 'backend/src/main/resources/application.properties') | ForEach-Object {
    if ($_ -match '^(spring\.datasource\.[^=]+)=(.*)$') { $uiProperties[$matches[1]]=$matches[2].Trim() }
}
if ($uiProperties['spring.datasource.url'] -notmatch '^jdbc:postgresql://([^:/]+):(\d+)/([^?]+)') { throw 'URL non reconnue.' }
$uiDbHost=$matches[1]; $uiDbPort=$matches[2]; $uiDbName=$matches[3]
$uiHash=& 'C:/Users/Mandaniaina/AppData/Local/Author Software/nvm/installs/v24.20.0/node.exe' -e "const b=require('./frontend/node_modules/bcryptjs');process.stdout.write(b.hashSync('UiVerification_2026',4))"
$uiReset=[IO.File]::ReadAllText((Join-Path $uiRoot 'backend/bdd/reinitialiser_donnees.sql'))
$uiSeed=[IO.File]::ReadAllText((Join-Path $uiRoot 'backend/bdd/donne_deepseek.sql'))
$uiSql="SET search_path TO $uiSchema;`n"+$uiReset+"`n"+$uiSeed+"`nUPDATE utilisateur SET mot_de_passe='$uiHash' WHERE nom_utilisateur='user1';"
$uiPath=Join-Path $uiRoot 'backend/target/ui_reset.sql'
[IO.File]::WriteAllText($uiPath,$uiSql,(New-Object Text.UTF8Encoding($false)))
$env:PGPASSWORD=$uiProperties['spring.datasource.password']; $env:PGCLIENTENCODING='UTF8'
$ErrorActionPreference='Continue'
& 'C:/Program Files/PostgreSQL/17/bin/psql.exe' -w -h $uiDbHost -p $uiDbPort -U $uiProperties['spring.datasource.username'] -d $uiDbName -v ON_ERROR_STOP=1 -f $uiPath *> (Join-Path $uiRoot 'backend/target/ui_reset.log')
Get-Content (Join-Path $uiRoot 'backend/target/ui_reset.log') -Tail 4
exit $LASTEXITCODE
