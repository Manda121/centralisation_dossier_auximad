-- Charger dans un schema regime_verification_* vide, jamais dans la base applicative.
\set ON_ERROR_STOP on
DO $$ BEGIN
    IF current_schema() NOT LIKE 'regime_verification_%' THEN
        RAISE EXCEPTION 'Un schema de test isole regime_verification_* est obligatoire.';
    END IF;
END $$;

CREATE TABLE type_regime (id SERIAL PRIMARY KEY, nom VARCHAR(100) UNIQUE, code VARCHAR(10) UNIQUE, description TEXT);
CREATE TABLE ordre_transit (id SERIAL PRIMARY KEY, id_type_regime INTEGER REFERENCES type_regime(id));
INSERT INTO type_regime (nom, code, description) VALUES
    ('Mise a la consommation', 'MCI', 'Importation definitive'),
    ('Exportation', 'EXP', 'Exportation simple'),
    ('Transit', 'TRS', 'Transit international'),
    ('Entrepot', 'ENT', 'Entrepot douanier'),
    ('Admission temporaire', 'ADT', 'Admission temporaire');
INSERT INTO ordre_transit (id_type_regime) SELECT id FROM type_regime ORDER BY id;

\ir ../migrations/20261103_regimes_douaniers.sql

DO $$ BEGIN
    IF (SELECT count(*) FROM type_regime) <> 7
       OR (SELECT count(*) FROM type_regime WHERE nom = 'IM8') <> 2
       OR EXISTS (
            SELECT 1 FROM (VALUES (1, '4000'), (2, '1000'), (3, '8900'), (4, '7000'), (5, '5000')) AS attendu(id, code)
            JOIN ordre_transit ot ON ot.id = attendu.id
            JOIN type_regime r ON r.id = ot.id_type_regime
            WHERE r.id <> attendu.id OR r.code <> attendu.code
       ) THEN
        RAISE EXCEPTION 'Les sept regimes et les identifiants historiques des OT doivent etre conserves.';
    END IF;
END $$;

-- Une ancienne valeur et sa nouvelle valeur peuvent aussi deja coexister.
INSERT INTO type_regime (nom, code, description) VALUES ('Transit historique', 'TRS', 'Ancien transit');
UPDATE ordre_transit SET id_type_regime = (SELECT id FROM type_regime WHERE code = 'TRS') WHERE id = 3;
\ir ../migrations/20261103_regimes_douaniers.sql

DO $$ BEGIN
    IF (SELECT count(*) FROM type_regime) <> 7
       OR (SELECT count(*) FROM ordre_transit) <> 5
       OR EXISTS (SELECT 1 FROM type_regime WHERE code IN ('MCI', 'EXP', 'TRS', 'ENT', 'ADT'))
       OR NOT EXISTS (SELECT 1 FROM ordre_transit ot JOIN type_regime r ON r.id = ot.id_type_regime
                      WHERE ot.id = 3 AND r.code = '8900') THEN
        RAISE EXCEPTION 'Le rejeu doit fusionner les anciennes valeurs sans perdre les OT.';
    END IF;
    RAISE NOTICE 'Migration regimes verifiee : deux IM8, liens OT conserves et rejeu valide.';
END $$;
