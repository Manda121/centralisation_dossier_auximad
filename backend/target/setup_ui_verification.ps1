$ErrorActionPreference = 'Stop'
$uiRoot = 'D:/Stage Auximad/centralisation_app'
$uiProperties = @{}
Get-Content (Join-Path $uiRoot 'backend/src/main/resources/application.properties') | ForEach-Object {
    if ($_ -match '^(spring\.datasource\.[^=]+)=(.*)$') { $uiProperties[$matches[1]] = $matches[2].Trim() }
}
if ($uiProperties['spring.datasource.url'] -notmatch '^jdbc:postgresql://([^:/]+):(\d+)/([^?]+)') { throw 'URL PostgreSQL non reconnue.' }
$uiDbHost=$matches[1]; $uiDbPort=$matches[2]; $uiDbName=$matches[3]
$uiSchema='ui_verification_' + [Guid]::NewGuid().ToString('N')
$uiBase=[IO.File]::ReadAllText((Join-Path $uiRoot 'backend/bdd/base_centralisation.sql'))
$uiBase=$uiBase.Substring($uiBase.IndexOf('CREATE TABLE agence('))
$uiSeed=[IO.File]::ReadAllText((Join-Path $uiRoot 'backend/bdd/donne_deepseek.sql'))
$uiChecks=[IO.File]::ReadAllText((Join-Path $uiRoot 'backend/bdd/tests/verification_donne_deepseek.sql'))
$uiNode='C:/Users/Mandaniaina/AppData/Local/Author Software/nvm/installs/v24.20.0/node.exe'
$uiHash = & $uiNode -e "const b=require('./frontend/node_modules/bcryptjs'); process.stdout.write(b.hashSync('UiVerification_2026',4))"
if ($LASTEXITCODE -ne 0) { throw 'Échec préparation identifiants de test.' }
$uiSql="\set ON_ERROR_STOP on`nCREATE SCHEMA $uiSchema;`nSET search_path TO $uiSchema;`n" + $uiBase + "`n" + $uiSeed + "`n" + $uiChecks + "`nUPDATE utilisateur SET mot_de_passe='$uiHash' WHERE nom_utilisateur='user1';`n"
[IO.File]::WriteAllText((Join-Path $uiRoot 'backend/target/ui_setup.sql'),$uiSql,(New-Object Text.UTF8Encoding($false)))
$env:PGPASSWORD=$uiProperties['spring.datasource.password']; $env:PGCLIENTENCODING='UTF8'
$ErrorActionPreference='Continue'
& 'C:/Program Files/PostgreSQL/17/bin/psql.exe' -w -h $uiDbHost -p $uiDbPort -U $uiProperties['spring.datasource.username'] -d $uiDbName -v ON_ERROR_STOP=1 -f (Join-Path $uiRoot 'backend/target/ui_setup.sql') *> (Join-Path $uiRoot 'backend/target/ui_setup.log')
if ($LASTEXITCODE -ne 0) { Get-Content (Join-Path $uiRoot 'backend/target/ui_setup.log') -Tail 20; exit 1 }
$uiCode = & 'C:/Program Files/PostgreSQL/17/bin/psql.exe' -w -h $uiDbHost -p $uiDbPort -U $uiProperties['spring.datasource.username'] -d $uiDbName -Atc "SELECT a.code FROM $uiSchema.utilisateur u JOIN $uiSchema.agence a ON a.id=u.id_agence WHERE u.nom_utilisateur='user1';"
[xml]$uiReport=Get-Content -LiteralPath (Join-Path $uiRoot 'backend/target/surefire-reports/TEST-com.auximad.service.ConteneurEligibilityTest.xml')
$uiClassPath=($uiReport.testsuite.properties.property | Where-Object { $_.name -eq 'java.class.path' }).value
$env:SPRING_DATASOURCE_URL=$uiProperties['spring.datasource.url']+'?currentSchema='+$uiSchema
$env:SPRING_DATASOURCE_USERNAME=$uiProperties['spring.datasource.username']
$env:SPRING_DATASOURCE_PASSWORD=$uiProperties['spring.datasource.password']
$env:SPRING_JPA_PROPERTIES_HIBERNATE_DEFAULT_SCHEMA=$uiSchema
$env:SERVER_PORT='18081'
$env:APP_MAIL_POLL_DELAY='3600000'
$uiJava=Start-Process -FilePath 'C:/Program Files/Java/jdk-17/bin/java.exe' -ArgumentList @('-cp',('"'+$uiClassPath+'"'),'com.auximad.AuximadApplication') -WorkingDirectory (Join-Path $uiRoot 'backend') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $uiRoot 'backend/target/ui_backend.log') -RedirectStandardError (Join-Path $uiRoot 'backend/target/ui_backend_error.log')
$uiVite=Start-Process -FilePath $uiNode -ArgumentList @('node_modules/vite/bin/vite.js','--host','localhost','--port','5173','--strictPort') -WorkingDirectory (Join-Path $uiRoot 'frontend') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $uiRoot 'backend/target/ui_frontend.log') -RedirectStandardError (Join-Path $uiRoot 'backend/target/ui_frontend_error.log')
$uiEdge=Start-Process -FilePath 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe' -ArgumentList @('--headless=new','--disable-gpu','--no-first-run','--remote-debugging-address=127.0.0.1','--remote-debugging-port=19222',('--user-data-dir="'+(Join-Path $uiRoot 'backend/target/ui-browser-profile')+'"'),'--window-size=1440,1100','about:blank') -WorkingDirectory $uiRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $uiRoot 'backend/target/ui_browser.log') -RedirectStandardError (Join-Path $uiRoot 'backend/target/ui_browser_error.log')
@{ schema=$uiSchema; javaPid=$uiJava.Id; vitePid=$uiVite.Id; browserPid=$uiEdge.Id; codeAgence=[int]$uiCode } | ConvertTo-Json | Set-Content -Encoding UTF8 (Join-Path $uiRoot 'backend/target/ui_runtime.json')
Write-Output 'Schéma de test créé ; backend, frontend et navigateur démarrés en arrière-plan.'
