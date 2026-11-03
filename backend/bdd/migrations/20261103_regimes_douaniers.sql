-- Mise a jour du referentiel sans reinitialiser les dossiers ni les OT.
-- Autoriser deux noms IM8 et conserver l'unicite du code.
BEGIN;

LOCK TABLE type_regime IN SHARE ROW EXCLUSIVE MODE;

DO $regimes$
DECLARE
    contrainte RECORD;
    regime RECORD;
    ancien_id INTEGER;
    cible_id INTEGER;
BEGIN
    FOR contrainte IN
        SELECT c.conname FROM pg_constraint c
        WHERE c.conrelid = 'type_regime'::regclass AND c.contype = 'u'
          AND c.conkey = ARRAY[(SELECT attnum FROM pg_attribute
              WHERE attrelid = 'type_regime'::regclass AND attname = 'nom')]::smallint[]
    LOOP
        EXECUTE format('ALTER TABLE type_regime DROP CONSTRAINT %I', contrainte.conname);
    END LOOP;

    FOR regime IN SELECT * FROM (VALUES
        ('IM4', '4000', 'definitif', 'MCI'),
        ('IM5', '5000', 'economique', 'ADT'),
        ('IM7', '7000', 'economique', 'ENT'),
        ('IM8', '8900', 'transitoire', 'TRS'),
        ('IM8', '8000', 'ordinaire', NULL),
        ('EX1', '1000', 'definitif', 'EXP'),
        ('EX2', '2000', 'temporaire', NULL)
    ) AS t(nom, code, description, ancien_code)
    LOOP
        ancien_id := NULL;
        cible_id := NULL;
        SELECT id INTO cible_id FROM type_regime WHERE code = regime.code;
        SELECT id INTO ancien_id FROM type_regime WHERE code = regime.ancien_code;
        IF cible_id IS NULL AND ancien_id IS NOT NULL THEN
            -- Reutiliser l'identifiant historique pour conserver tous ses liens.
            UPDATE type_regime SET nom = regime.nom, code = regime.code, description = regime.description
            WHERE id = ancien_id RETURNING id INTO cible_id;
        ELSE
            INSERT INTO type_regime (nom, code, description)
            VALUES (regime.nom, regime.code, regime.description)
            ON CONFLICT (code) DO UPDATE SET nom = EXCLUDED.nom, description = EXCLUDED.description
            RETURNING id INTO cible_id;
            -- Si anciennes et nouvelles valeurs coexistent, fusionner leurs liens.
            IF ancien_id IS NOT NULL AND ancien_id <> cible_id THEN
                UPDATE ordre_transit SET id_type_regime = cible_id WHERE id_type_regime = ancien_id;
                DELETE FROM type_regime WHERE id = ancien_id;
            END IF;
        END IF;
    END LOOP;
END;
$regimes$;

COMMIT;

SELECT nom, description, code FROM type_regime
ORDER BY CASE WHEN nom LIKE 'IM%' THEN 0 WHEN nom LIKE 'EX%' THEN 1 ELSE 2 END, nom, code DESC;
