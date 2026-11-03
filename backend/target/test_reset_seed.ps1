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
$seedTestSchema = 'seed_reset_verification_' + [Guid]::NewGuid().ToString('N')
$seedSchemaText = [IO.File]::ReadAllText((Join-Path $seedRoot 'backend/bdd/base_centralisation.sql'))
$seedSchemaText = $seedSchemaText.Substring($seedSchemaText.IndexOf('CREATE TABLE agence('))
$seedSqlText = [IO.File]::ReadAllText((Join-Path $seedRoot 'backend/bdd/donne_deepseek.sql'))
$seedChecksText = [IO.File]::ReadAllText((Join-Path $seedRoot 'backend/bdd/tests/verification_donne_deepseek.sql'))
$seedResetText = [IO.File]::ReadAllText((Join-Path $seedRoot 'backend/bdd/reinitialiser_donnees.sql'))
$seedExpectedTables = [regex]::Matches($seedSchemaText, '(?im)^CREATE TABLE ').Count
$seedResetChecks = @'
DO $verification_reset$
DECLARE
    t RECORD;
    v_lignes BIGINT;
BEGIN
    IF current_schema() NOT LIKE 'seed_reset_verification_%' THEN
        RAISE EXCEPTION 'Schéma isolé obligatoire pour ce test.';
    END IF;
    IF (SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname = current_schema() AND c.relkind IN ('r', 'p')) <> TABLES_ATTENDUES THEN
        RAISE EXCEPTION 'La réinitialisation ne doit supprimer aucune table.';
    END IF;
    FOR t IN SELECT tablename FROM pg_tables WHERE schemaname = current_schema() LOOP
        EXECUTE format('SELECT count(*) FROM %I.%I', current_schema(), t.tablename) INTO v_lignes;
        IF v_lignes <> 0 THEN RAISE EXCEPTION 'La table % contient encore des données.', t.tablename; END IF;
    END LOOP;
    IF EXISTS (SELECT 1 FROM statut_dossier_id_seq WHERE last_value <> 1 OR is_called) THEN
        RAISE EXCEPTION 'La séquence des statuts doit être revenue à sa valeur initiale.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgrelid = 'dossier'::regclass
                   AND tgname = 'dossier_actualiser_fts') THEN
        RAISE EXCEPTION 'Le déclencheur du dossier doit être conservé.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = current_schema()
                   AND indexname = 'idx_dossier_recherche_fts') THEN
        RAISE EXCEPTION 'L''index de recherche doit être conservé.';
    END IF;
    RAISE NOTICE 'Réinitialisation vérifiée : tables vides, séquences remises au début, structure conservée.';
END;
$verification_reset$;
'@
$seedResetChecks = $seedResetChecks.Replace('TABLES_ATTENDUES', $seedExpectedTables.ToString())
$seedRunPath = Join-Path $seedRoot 'backend/target/verification_reset_run.sql'
$seedLogPath = Join-Path $seedRoot 'backend/target/verification_reset.log'
$seedScriptText = "\set ON_ERROR_STOP on`nCREATE SCHEMA $seedTestSchema;`nSET search_path TO $seedTestSchema;`n" +
    $seedSchemaText + "`n" + $seedSqlText + "`n" + $seedChecksText + "`n" +
    $seedResetText + "`n" + $seedResetChecks + "`n" + $seedSqlText + "`n" + $seedChecksText
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
    if ($seedTestSchema -match '^seed_reset_verification_[a-f0-9]{32}$') {
        & 'C:/Program Files/PostgreSQL/17/bin/psql.exe' -w -h $seedDbHost -p $seedDbPort -U $seedDbProperties['spring.datasource.username'] -d $seedDbName -c "SET client_min_messages TO warning; DROP SCHEMA IF EXISTS $seedTestSchema CASCADE;" *> (Join-Path $seedRoot 'backend/target/verification_reset_cleanup.log')
    }
    $env:PGPASSWORD = $seedOldPassword
    $env:PGCLIENTENCODING = $seedOldEncoding
    Get-Content -Encoding UTF8 $seedLogPath -Tail 55
}
exit $seedResult
