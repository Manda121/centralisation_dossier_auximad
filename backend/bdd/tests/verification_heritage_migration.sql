DO $$ BEGIN
    IF current_schema() NOT LIKE 'seed_verification_%' THEN
        RAISE EXCEPTION 'Utiliser un schema temporaire seed_verification_*';
    END IF;
END $$;
CREATE TABLE mode_transport(id int PRIMARY KEY, nom text);
CREATE TABLE type_production(id int PRIMARY KEY, id_mode_transport int);
CREATE TABLE ordre_transit(id int PRIMARY KEY, id_type_production int);
CREATE TABLE dossier(id int PRIMARY KEY, id_type_production int, id_ordre_transit int);
CREATE TABLE conteneur(id int PRIMARY KEY);
CREATE TABLE dossier_conteneur(id_dossier int REFERENCES dossier, id_conteneur int REFERENCES conteneur,
                              PRIMARY KEY(id_dossier, id_conteneur));
CREATE TABLE dossier_interagence(id_dossier_mere int REFERENCES dossier, id_sous_dossier int REFERENCES dossier);
CREATE TABLE document(id serial PRIMARY KEY, id_dossier int REFERENCES dossier, id_type_document int,
                      nombre int, chemin_fichier text, nom_fichier text, est_actif boolean, date_ajout timestamp);
CREATE TABLE remise(id serial PRIMARY KEY, id_dossier int REFERENCES dossier, id_type_document int, date_remise timestamp);
INSERT INTO mode_transport VALUES (1, 'Maritime'), (2, 'Aerien');
INSERT INTO type_production VALUES (1,1), (2,2);
INSERT INTO dossier VALUES (1,1,NULL), (2,1,NULL), (3,1,NULL), (4,2,NULL);
INSERT INTO conteneur VALUES (10);
INSERT INTO dossier_conteneur VALUES (1,10);
INSERT INTO dossier_interagence VALUES (1,2), (2,3), (1,4);
INSERT INTO document(id_dossier,id_type_document,nombre,chemin_fichier,nom_fichier,est_actif,date_ajout) VALUES
    (1,1,2,'dossier-1/a.pdf','a.pdf',TRUE,'2026-10-01'),
    (1,2,1,'dossier-1/retire.pdf','retire.pdf',FALSE,'2026-09-01'),
    (2,3,1,'dossier-2/propre.pdf','propre.pdf',TRUE,'2026-10-02');
INSERT INTO remise(id_dossier,id_type_document,date_remise) VALUES (1,1,'2026-10-01'),(2,1,'2026-10-03');
\ir ../migrations/20261103_heritage_sous_dossiers.sql
\ir ../migrations/20261103_heritage_sous_dossiers.sql
DO $$ BEGIN
    IF (SELECT count(*) FROM dossier_conteneur) <> 3
       OR (SELECT count(*) FROM dossier_conteneur WHERE id_dossier=4) <> 0
       OR (SELECT count(*) FROM document) <> 7
       OR (SELECT count(*) FROM document WHERE id_dossier=3 AND est_actif) <> 2
       OR (SELECT count(*) FROM document WHERE id_dossier<>1 AND id_type_document=2) <> 0
       OR (SELECT date_remise FROM remise WHERE id_dossier=2 AND id_type_document=1) <> '2026-10-03'::timestamp
       OR (SELECT date_remise FROM remise WHERE id_dossier=3 AND id_type_document=1) <> '2026-10-03'::timestamp THEN
        RAISE EXCEPTION 'Heritage incomplet, duplique ou incoherent';
    END IF;
END $$;
SELECT 'Heritage existant valide : plusieurs niveaux, idempotence, transport, dates' AS resultat;
