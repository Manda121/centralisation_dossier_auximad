$ErrorActionPreference='Stop'
$regimeRoot='D:/Stage Auximad/centralisation_app'
$regimeProperties=@{}
Get-Content (Join-Path $regimeRoot 'backend/src/main/resources/application.properties') | ForEach-Object {
    if ($_ -match '^(spring\.datasource\.[^=]+)=(.*)$') { $regimeProperties[$matches[1]]=$matches[2].Trim() }
}
if ($regimeProperties['spring.datasource.url'] -notmatch '^jdbc:postgresql://([^:/]+):(\d+)/([^?]+)') { throw 'URL non reconnue.' }
$regimeDbHost=$matches[1];$regimeDbPort=$matches[2];$regimeDbName=$matches[3]
$regimeSchema='regime_verification_'+[Guid]::NewGuid().ToString('N')
$regimeTest=(Join-Path $regimeRoot 'backend/bdd/tests/verification_regimes_migration.sql').Replace('\','/')
$regimeSql="\set ON_ERROR_STOP on`nCREATE SCHEMA $regimeSchema;`nSET search_path TO $regimeSchema;`n\i '$regimeTest'`n"
$regimePath=Join-Path $regimeRoot 'backend/target/regimes_migration_test.sql'
[IO.File]::WriteAllText($regimePath,$regimeSql,(New-Object Text.UTF8Encoding($false)))
$env:PGPASSWORD=$regimeProperties['spring.datasource.password'];$env:PGCLIENTENCODING='UTF8'
$ErrorActionPreference='Continue'
$regimeResult=1
try {
    & 'C:/Program Files/PostgreSQL/17/bin/psql.exe' -w -h $regimeDbHost -p $regimeDbPort -U $regimeProperties['spring.datasource.username'] -d $regimeDbName -v ON_ERROR_STOP=1 -f $regimePath *> (Join-Path $regimeRoot 'backend/target/regimes_migration_test.log')
    $regimeResult=$LASTEXITCODE
} finally {
    if ($regimeSchema -match '^regime_verification_[a-f0-9]{32}$') {
        & 'C:/Program Files/PostgreSQL/17/bin/psql.exe' -w -h $regimeDbHost -p $regimeDbPort -U $regimeProperties['spring.datasource.username'] -d $regimeDbName -c "SET client_min_messages TO warning; DROP SCHEMA IF EXISTS $regimeSchema CASCADE;" *> (Join-Path $regimeRoot 'backend/target/regimes_migration_cleanup.log')
    }
    Get-Content -Encoding UTF8 (Join-Path $regimeRoot 'backend/target/regimes_migration_test.log') -Tail 23
}
exit $regimeResult
