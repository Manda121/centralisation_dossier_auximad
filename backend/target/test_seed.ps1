$ErrorActionPreference = 'Stop'
$seedRoot = 'D:/Stage Auximad/centralisation_app'
$seedDbProperties = @{}
Get-Content (Join-Path $seedRoot 'backend/src/main/resources/application.properties') | ForEach-Object {
    if ($_ -match '^(spring\.datasource\.[^=]+)=(.*)$') { $seedDbProperties[$matches[1]] = $matches[2].Trim() }
}
if ($seedDbProperties['spring.datasource.url'] -notmatch '^jdbc:postgresql://([^:/]+):(\d+)/([^?]+)') {
    throw 'Configuration PostgreSQL non reconnue.'
}
$seedDbHost = $matches[1]; $seedDbPort = $matches[2]; $seedDbName = $matches[3]
$seedTestSchema = 'seed_verification_' + [Guid]::NewGuid().ToString('N')
$seedSchemaText = [IO.File]::ReadAllText((Join-Path $seedRoot 'backend/bdd/base_centralisation.sql'))
$seedSchemaText = $seedSchemaText.Substring($seedSchemaText.IndexOf('CREATE TABLE agence('))
$seedSqlText = [IO.File]::ReadAllText((Join-Path $seedRoot 'backend/bdd/donne_deepseek.sql'))
$seedChecksText = [IO.File]::ReadAllText((Join-Path $seedRoot 'backend/bdd/tests/verification_donne_deepseek.sql'))
$seedRunPath = Join-Path $seedRoot 'backend/target/verification_seed_run.sql'
$seedLogPath = Join-Path $seedRoot 'backend/target/verification_seed.log'
$seedScriptText = "\set ON_ERROR_STOP on`nCREATE SCHEMA $seedTestSchema;`nSET search_path TO $seedTestSchema;`n" +
    $seedSchemaText + "`n" + $seedSqlText + "`n" + $seedChecksText
[IO.File]::WriteAllText($seedRunPath, $seedScriptText, (New-Object Text.UTF8Encoding($false)))
$seedOldPassword = $env:PGPASSWORD
$seedOldEncoding = $env:PGCLIENTENCODING
$seedResult = 1
try {
    $env:PGPASSWORD = $seedDbProperties['spring.datasource.password']
    $env:PGCLIENTENCODING = 'UTF8'
    $ErrorActionPreference = 'Continue'
    & 'C:/Program Files/PostgreSQL/17/bin/psql.exe' -w -h $seedDbHost -p $seedDbPort -U $seedDbProperties['spring.datasource.username'] -d $seedDbName -v ON_ERROR_STOP=1 -f $seedRunPath *> $seedLogPath
    $seedResult = $LASTEXITCODE
} finally {
    if ($seedTestSchema -match '^seed_verification_[a-f0-9]{32}$') {
        & 'C:/Program Files/PostgreSQL/17/bin/psql.exe' -w -h $seedDbHost -p $seedDbPort -U $seedDbProperties['spring.datasource.username'] -d $seedDbName -c "SET client_min_messages TO warning; DROP SCHEMA IF EXISTS $seedTestSchema CASCADE;" *> (Join-Path $seedRoot 'backend/target/verification_seed_cleanup.log')
    }
    $env:PGPASSWORD = $seedOldPassword
    $env:PGCLIENTENCODING = $seedOldEncoding
    Get-Content -Encoding UTF8 $seedLogPath -Tail 90
}
exit $seedResult
