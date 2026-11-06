-- Verification exclusivement dans un schema temporaire de test.
DO $$
DECLARE
    t RECORD;
    n BIGINT;
BEGIN
    IF current_schema() NOT LIKE 'seed_verification_%' THEN
        RAISE EXCEPTION 'Utiliser un schema temporaire seed_verification_*';
    END IF;
    IF (SELECT count(*) FROM agence) <> 9
       OR (SELECT count(*) FROM statut_dossier) <> 17
       OR (SELECT count(*) FROM phase_dossier) <> 3
       OR (SELECT count(*) FROM operation_dossier) <> 64
       OR (SELECT count(*) FROM type_regime) <> 7
       OR (SELECT count(*) FROM utilisateur) <> 30
       OR (SELECT count(*) FROM autorisation WHERE niveau_autorisation = 10000) <> 30 THEN
        RAISE EXCEPTION 'Configuration initiale incomplete';
    END IF;
    -- Toutes les tables non configurees doivent rester vides.
    FOR t IN SELECT tablename FROM pg_tables WHERE schemaname = current_schema()
        AND tablename NOT IN (
            'statut_dossier', 'statut_ot', 'agence', 'role', 'mode_transport',
            'type_production', 'type_client', 'type_regime', 'type_bureau', 'bureau',
            'mode_acheminement', 'nature_marchandise', 'phase_dossier',
            'operation_dossier', 'inconterme', 'type_constat', 'expert_constat',
            'pays', 'type_declaration', 'type_document', 'document_obligatoire_mvt',
            'type_container', 'compagnie', 'navire', 'station', 'unite_mesure',
            'action', 'utilisateur', 'autorisation')
    LOOP
        EXECUTE format('SELECT count(*) FROM %I.%I', current_schema(), t.tablename) INTO n;
        IF n <> 0 THEN
            RAISE EXCEPTION 'La table % contient % donnees metier', t.tablename, n;
        END IF;
    END LOOP;
END;
$$;
SELECT 'Configuration initiale valide, tables metier vides' AS resultat;
