-- Réinitialise les données du schéma courant en conservant la base, les tables,
-- les contraintes, les index, les fonctions et les déclencheurs.
-- Toutes les lignes sont effacées, y compris les comptes utilisateurs,
-- les référentiels, les adresses emails configurées et les historiques.
-- Les séquences appartenant aux tables reprennent leur valeur initiale
-- (habituellement 1), grâce à RESTART IDENTITY.
--
-- Dans psql, connecté à dossier_auximad :
--   ROLLBACK;  -- à faire d'abord si une requête précédente a échoué dans BEGIN
--   \i 'D:/Stage Auximad/centralisation_app/backend/bdd/reinitialiser_donnees.sql'
--   \i 'D:/Stage Auximad/centralisation_app/backend/bdd/donne_deepseek.sql'
--
-- Le schéma visé est current_schema(), normalement public. Pour un test isolé,
-- définir explicitement search_path avant de charger ce fichier.
-- Une référence depuis un autre schéma bloque la réinitialisation : aucune
-- suppression de données ne sera étendue à ce schéma par CASCADE.
\set ON_ERROR_STOP on

BEGIN;

DO $reinitialisation$
DECLARE
    v_schema NAME := current_schema();
    v_tables TEXT;
BEGIN
    IF v_schema IS NULL OR v_schema = 'information_schema' OR v_schema LIKE 'pg_%' THEN
        RAISE EXCEPTION 'Schéma de réinitialisation invalide : %.', v_schema;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM (VALUES ('agence'), ('dossier'), ('statut_dossier')) AS attendue(nom)
        WHERE NOT EXISTS (
            SELECT 1 FROM pg_class c
            JOIN pg_namespace n ON n.oid = c.relnamespace
            WHERE n.nspname = v_schema AND c.relname = attendue.nom
              AND c.relkind IN ('r', 'p')
        )
    ) THEN
        RAISE EXCEPTION 'Le schéma % doit contenir les tables agence, dossier et statut_dossier.', v_schema;
    END IF;

    -- TRUNCATE inclut aussi les descendants d'une table : interdire ceux qui
    -- appartiennent à un autre schéma, même sans option CASCADE.
    IF EXISTS (
        SELECT 1 FROM pg_inherits heritage
        JOIN pg_class parent ON parent.oid = heritage.inhparent
        JOIN pg_namespace schema_parent ON schema_parent.oid = parent.relnamespace
        JOIN pg_class enfant ON enfant.oid = heritage.inhrelid
        JOIN pg_namespace schema_enfant ON schema_enfant.oid = enfant.relnamespace
        WHERE schema_parent.nspname = v_schema AND schema_enfant.nspname <> v_schema
    ) THEN
        RAISE EXCEPTION 'Une table du schéma % possède un descendant dans un autre schéma ; réinitialisation annulée.', v_schema;
    END IF;

    SELECT string_agg(format('%I.%I', n.nspname, c.relname), ', ' ORDER BY c.relname)
    INTO v_tables
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = v_schema AND c.relkind IN ('r', 'p');

    IF v_tables IS NULL THEN
        RAISE EXCEPTION 'Aucune table à réinitialiser dans le schéma %.', v_schema;
    END IF;

    -- Toutes les tables sont vidées ensemble pour respecter leurs clés étrangères.
    -- RESTRICT conserve les données des autres schémas en cas de référence externe.
    EXECUTE 'TRUNCATE TABLE ' || v_tables || ' RESTART IDENTITY RESTRICT';
    RAISE NOTICE 'Données du schéma % réinitialisées ; tables conservées et identifiants remis à leur valeur initiale.', v_schema;
END;
$reinitialisation$;

COMMIT;
