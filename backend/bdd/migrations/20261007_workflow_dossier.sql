BEGIN;

ALTER TABLE mvt_dossier ADD COLUMN IF NOT EXISTS origine VARCHAR(30) DEFAULT 'SAISIE';
ALTER TABLE mvt_dossier ADD COLUMN IF NOT EXISTS groupe_ajustement VARCHAR(36);

CREATE TABLE IF NOT EXISTS mvt_annulee (
    id SERIAL PRIMARY KEY,
    id_mvt_dossier INTEGER NOT NULL UNIQUE REFERENCES mvt_dossier(id) ON DELETE CASCADE,
    date_annulation TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    motif_annulation TEXT,
    id_utilisateur_annulation INTEGER REFERENCES utilisateur(id)
);

-- Conserver les annulations inscrites par une version precedente du schema.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'mvt_dossier' AND column_name = 'est_annule'
    ) THEN
        EXECUTE $migration$
            INSERT INTO mvt_annulee
                (id_mvt_dossier, date_annulation, motif_annulation, id_utilisateur_annulation)
            SELECT id, COALESCE(date_annulation, CURRENT_TIMESTAMP),
                   motif_annulation, id_utilisateur_annulation
            FROM mvt_dossier WHERE est_annule = TRUE
            ON CONFLICT (id_mvt_dossier) DO NOTHING
        $migration$;
    END IF;
END $$;

ALTER TABLE mvt_dossier DROP COLUMN IF EXISTS est_annule;
ALTER TABLE mvt_dossier DROP COLUMN IF EXISTS date_annulation;
ALTER TABLE mvt_dossier DROP COLUMN IF EXISTS motif_annulation;
ALTER TABLE mvt_dossier DROP COLUMN IF EXISTS id_utilisateur_annulation;

COMMIT;
