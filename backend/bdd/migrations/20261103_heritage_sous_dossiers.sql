-- Complete les sous-dossiers deja existants avec les ressources du dossier mere.
-- Ajouts uniquement : ne remplace ni ne retire leurs propres documents/remises.
-- Rejouable ; traite aussi plusieurs niveaux de sous-dossiers.
BEGIN;
DO $$
DECLARE
    ajouts BIGINT;
    nombre BIGINT;
BEGIN
    LOOP
        ajouts := 0;
        INSERT INTO dossier_conteneur (id_dossier, id_conteneur)
        SELECT DISTINCT lien.id_sous_dossier, dc.id_conteneur
        FROM dossier_interagence lien
        JOIN dossier_conteneur dc ON dc.id_dossier = lien.id_dossier_mere
        JOIN dossier enfant ON enfant.id = lien.id_sous_dossier
        LEFT JOIN type_production tp ON tp.id = enfant.id_type_production
        LEFT JOIN mode_transport mt ON mt.id = tp.id_mode_transport
        LEFT JOIN ordre_transit ot ON ot.id = enfant.id_ordre_transit
        LEFT JOIN type_production otp ON otp.id = ot.id_type_production
        LEFT JOIN mode_transport otmt ON otmt.id = otp.id_mode_transport
        WHERE lower(COALESCE(NULLIF(trim(mt.nom), ''), trim(otmt.nom))) = 'maritime'
        ON CONFLICT (id_dossier, id_conteneur) DO NOTHING;
        GET DIAGNOSTICS nombre = ROW_COUNT;
        ajouts := ajouts + nombre;

        INSERT INTO document (id_dossier, id_type_document, nombre, chemin_fichier,
                              nom_fichier, est_actif, date_ajout)
        SELECT DISTINCT lien.id_sous_dossier, source.id_type_document, source.nombre,
               source.chemin_fichier, source.nom_fichier, TRUE, source.date_ajout
        FROM dossier_interagence lien
        JOIN document source ON source.id_dossier = lien.id_dossier_mere AND source.est_actif = TRUE
        WHERE NOT EXISTS (
            SELECT 1 FROM document cible
            WHERE cible.id_dossier = lien.id_sous_dossier AND cible.est_actif = TRUE
              AND cible.id_type_document = source.id_type_document
              AND cible.nombre IS NOT DISTINCT FROM source.nombre
              AND cible.chemin_fichier IS NOT DISTINCT FROM source.chemin_fichier
              AND cible.nom_fichier IS NOT DISTINCT FROM source.nom_fichier
        );
        GET DIAGNOSTICS nombre = ROW_COUNT;
        ajouts := ajouts + nombre;

        INSERT INTO remise (id_dossier, id_type_document, date_remise)
        SELECT DISTINCT ON (lien.id_sous_dossier, source.id_type_document)
               lien.id_sous_dossier, source.id_type_document, source.date_remise
        FROM dossier_interagence lien
        JOIN remise source ON source.id_dossier = lien.id_dossier_mere
        WHERE NOT EXISTS (
            SELECT 1 FROM remise cible
            WHERE cible.id_dossier = lien.id_sous_dossier
              AND cible.id_type_document = source.id_type_document
        )
        ORDER BY lien.id_sous_dossier, source.id_type_document, source.id;
        GET DIAGNOSTICS nombre = ROW_COUNT;
        ajouts := ajouts + nombre;
        EXIT WHEN ajouts = 0;
    END LOOP;
END;
$$;
COMMIT;
