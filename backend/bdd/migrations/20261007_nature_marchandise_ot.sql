-- À exécuter une fois sur une base existante avant de redémarrer l'application.
ALTER TABLE ordre_transit
    ADD COLUMN IF NOT EXISTS id_nature_marchandise INTEGER REFERENCES nature_marchandise(id);

-- Les anciennes marchandises pouvaient avoir plusieurs natures pour un même OT.
-- Conserver celle de la première marchandise enregistrée pour chaque OT.
UPDATE ordre_transit ot
SET id_nature_marchandise = source.id_nature_marchandise
FROM (
    SELECT DISTINCT ON (id_ordre_transit) id_ordre_transit, id_nature_marchandise
    FROM marchandise
    WHERE id_nature_marchandise IS NOT NULL
    ORDER BY id_ordre_transit, id
) source
WHERE ot.id = source.id_ordre_transit AND ot.id_nature_marchandise IS NULL;

CREATE INDEX IF NOT EXISTS idx_ordre_transit_nature_marchandise
    ON ordre_transit(id_nature_marchandise);

-- La colonne historique de marchandise est conservée sur une base existante.
-- Les nouvelles saisies utilisent uniquement la nature enregistrée sur l'OT.
