-- Cle d'un meme evenement repris dans plusieurs dossiers interagences.
-- Ne modifie pas les donnees d'avancement existantes.
BEGIN;
ALTER TABLE mvt_dossier ADD COLUMN IF NOT EXISTS cle_partage VARCHAR(36);
CREATE UNIQUE INDEX IF NOT EXISTS idx_mvt_partage_actif
    ON mvt_dossier(id_dossier, cle_partage)
    WHERE est_annule = FALSE AND cle_partage IS NOT NULL;
COMMIT;
