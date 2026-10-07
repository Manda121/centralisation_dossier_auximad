\c postgres;

DROP DATABASE IF EXISTS dossier_auximad;

CREATE DATABASE dossier_auximad;

\c dossier_auximad;

-- =========================================================================
-- 1. TABLES DE REFERENCE SIMPLES (sans dependances)
-- =========================================================================

CREATE TABLE agence(
    id SERIAL PRIMARY KEY,
    code INTEGER  UNIQUE CHECK (code BETWEEN 0 AND 99), -- 2 chiffres, cf. nomenclature numero_dossier
    nom VARCHAR(100) ,
    adresse VARCHAR(200) ,
    ville VARCHAR(100) ,
    cp_client VARCHAR(10) ,
    date_creation TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE role(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50)  UNIQUE,
    description TEXT
);

CREATE TABLE mode_transport(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50)  UNIQUE,
    description TEXT
);

CREATE TABLE type_client(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50)  UNIQUE,
    description TEXT
);

CREATE TABLE type_regime(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100)  UNIQUE,
    code VARCHAR(10)  UNIQUE,
    description TEXT
);

CREATE TABLE type_declaration(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100)  UNIQUE,
    description TEXT
);

CREATE TABLE type_bureau(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100)  UNIQUE,
    id_type_declaration INTEGER REFERENCES type_declaration(id),
    id_mode_transport INTEGER REFERENCES mode_transport(id),

    description TEXT
);

CREATE TABLE mode_acheminement(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50)  UNIQUE,
    description TEXT
);

CREATE TABLE nature_marchandise(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100)  UNIQUE,
    description TEXT
);

CREATE TABLE phase_dossier(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50)  UNIQUE,
    id_mode_transport INTEGER REFERENCES mode_transport(id),
    description TEXT
);

-- NOUVEAU : statut du dossier (ouvert / cloture / annule / en litige, etc.)
-- Repond au commentaire "mila manamboatra table historique phase sy statut dossier"
CREATE TABLE statut_dossier(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50)  UNIQUE,
    description TEXT
);

CREATE TABLE inconterme(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50)  UNIQUE,
    abreviation VARCHAR(10)  UNIQUE,
    description TEXT
);

CREATE TABLE type_constat(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100)  UNIQUE,
    description TEXT
);

CREATE TABLE expert_constat(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) ,
    prenom VARCHAR(100) ,
    email VARCHAR(100) UNIQUE ,
    telephone VARCHAR(15),
    adresse VARCHAR(200)
);

CREATE TABLE pays(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100)  UNIQUE,
    code VARCHAR(3)  UNIQUE
);

CREATE TABLE unite_mesure(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50)  UNIQUE,
    rapport_m_cube NUMERIC(10, 2) ,
    description TEXT
);

CREATE TABLE type_container(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50)  UNIQUE,
    description TEXT,
    volume NUMERIC(10, 2)  CHECK (volume > 0),
    poids_max NUMERIC(10, 2)  CHECK (poids_max > 0),
    longueur NUMERIC(10, 2)  CHECK (longueur > 0),
    largeur NUMERIC(10, 2)  CHECK (largeur > 0),
    hauteur NUMERIC(10, 2)  CHECK (hauteur > 0)
);

-- =========================================================================
-- 2. TABLES AVEC DEPENDANCES SIMPLES
-- =========================================================================

CREATE TABLE type_production(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) ,
    id_mode_transport INTEGER  REFERENCES mode_transport(id),
    code INTEGER , -- 1 chiffre, cf. nomenclature numero_dossier
    description TEXT
);

CREATE TABLE utilisateur(
    id SERIAL PRIMARY KEY,
    nom_utilisateur VARCHAR(50) UNIQUE ,
    nom VARCHAR(100) ,
    prenom VARCHAR(100) ,
    email VARCHAR(100) UNIQUE ,
    mot_de_passe VARCHAR(255) ,

    id_role INTEGER  REFERENCES role(id),
    id_agence INTEGER  REFERENCES agence(id),

    est_actif BOOLEAN  DEFAULT TRUE,

    date_creation TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP,
    date_modification TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP
);

-- Maintient automatiquement date_modification a chaque UPDATE
-- (un DEFAULT seul ne se declenche qu'a l'INSERT, jamais a l'UPDATE)
CREATE OR REPLACE FUNCTION trg_set_date_modification() RETURNS TRIGGER AS $$
BEGIN
    NEW.date_modification := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_utilisateur_date_modification
    BEFORE UPDATE ON utilisateur
    FOR EACH ROW
    EXECUTE FUNCTION trg_set_date_modification();

CREATE TABLE autorisation(
    id SERIAL PRIMARY KEY,
    id_utilisateur INTEGER  UNIQUE REFERENCES utilisateur(id),
    niveau_autorisation INTEGER 
);

CREATE TABLE bureau(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) ,
    adresse VARCHAR(200) ,
    cp_client VARCHAR(10) ,
    ville VARCHAR(100) ,
    description TEXT,
    code_bureau VARCHAR(10)  UNIQUE,

    id_type_bureau INTEGER  REFERENCES type_bureau(id)
);

CREATE TABLE clients(
    id SERIAL PRIMARY KEY,

    nom VARCHAR(100) ,
    prenom VARCHAR(100), -- nullable : une personne morale (societe) n'a pas de prenom
    email VARCHAR(100) UNIQUE ,
    telephone VARCHAR(15),
    adresse VARCHAR(200),
    ville VARCHAR(100),
    cp_client VARCHAR(10),
    id_pays INTEGER REFERENCES pays(id),

    id_type_client INTEGER  REFERENCES type_client(id),

    nif VARCHAR(20) UNIQUE ,
    stat VARCHAR(20) UNIQUE ,
    fax VARCHAR(20),

    date_creation TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE compagnie(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100)  UNIQUE,
    code VARCHAR(5) ,
    id_pays INTEGER  REFERENCES pays(id),
    id_mode_transport INTEGER  REFERENCES mode_transport(id), -- corrige : etait "mode_transport" (incoherent avec le reste du schema)
    description TEXT
);

CREATE TABLE station(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100)  UNIQUE,
    code VARCHAR(10)  UNIQUE,
    lieu VARCHAR(100) ,

    id_mode_transport INTEGER  REFERENCES mode_transport(id),
    description TEXT
);

CREATE TABLE type_document(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100)  UNIQUE,

    id_type_production INTEGER REFERENCES type_production(id),
    id_nature_marchandise INTEGER REFERENCES nature_marchandise(id),

    est_obligatoire BOOLEAN  DEFAULT FALSE,
    description TEXT
);

CREATE TABLE operation_dossier(
    id SERIAL PRIMARY KEY,
    description TEXT ,

    id_phase INTEGER  REFERENCES phase_dossier(id),
    id_statut INTEGER  REFERENCES statut_dossier(id),
    id_type_production INTEGER REFERENCES type_production(id),

    niveau_autorisation INTEGER ,
    niveau_validation INTEGER ,

    niveau_operation INTEGER ,
    est_actif BOOLEAN  DEFAULT TRUE,
    est_obligatoire BOOLEAN  DEFAULT FALSE
);

CREATE TABLE navire(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100)  UNIQUE,
    imo VARCHAR(20)  UNIQUE,
    id_compagnie INTEGER  REFERENCES compagnie(id),
    date_ajout TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP
);

-- =========================================================================
-- 3. ORDRE DE TRANSIT
-- =========================================================================

CREATE TABLE statut_ot(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50)  UNIQUE,
    description TEXT
);

CREATE TABLE ordre_transit(
    id SERIAL PRIMARY KEY,
    numero_ordre_transit INTEGER  UNIQUE,
    id_agence INTEGER  REFERENCES agence(id),
    id_client INTEGER  REFERENCES clients(id),

    id_type_production INTEGER REFERENCES type_production(id),
    id_nature_marchandise INTEGER REFERENCES nature_marchandise(id),

    numero_commande VARCHAR(100) ,

    id_utilisateur INTEGER  REFERENCES utilisateur(id),
    id_type_regime INTEGER REFERENCES type_regime(id),

    destination_finale VARCHAR(200) , -- destination geographique: Zone Franche Tanjombato, Antananarivo

    id_expediteur INTEGER REFERENCES clients(id),
    id_destinataire INTEGER REFERENCES clients(id),

    id_bureau_dedou INTEGER REFERENCES bureau(id),

    est_assure BOOLEAN  DEFAULT FALSE,

    id_destination_facture INTEGER REFERENCES clients(id),
    id_statut_ot INTEGER REFERENCES statut_ot(id),

    date_creation TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE contrat_transport(
    id SERIAL PRIMARY KEY,
    id_ordre_transit INTEGER  REFERENCES ordre_transit(id),
    numero VARCHAR(100)  UNIQUE,
    date_creation TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP,
    date_prevue TIMESTAMPTZ,      -- corrige : etait "date prevue" (2 identifiants -> erreur de syntaxe)
    date_arrivee TIMESTAMPTZ,     -- corrige : etait "date arrivee"
    id_compagnie INTEGER REFERENCES compagnie(id),
    id_navire INTEGER REFERENCES navire(id),
    n_vol VARCHAR(100),
    lieu_emission VARCHAR(100),
    notify VARCHAR(200)
);

CREATE TABLE instruction(
    id SERIAL PRIMARY KEY,
    id_ordre_transit INTEGER  REFERENCES ordre_transit(id),
    instruction TEXT ,
    date_creation TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP
);

-- =========================================================================
-- 4. DOSSIER
-- =========================================================================

CREATE TABLE dossier(
    id SERIAL PRIMARY KEY,
    id_agence INTEGER  REFERENCES agence(id),
    id_utilisateur INTEGER  REFERENCES utilisateur(id),
    id_client INTEGER  REFERENCES clients(id),
    id_type_production INTEGER  REFERENCES type_production(id),

    numero_dossier VARCHAR(100)  UNIQUE,
    id_ordre_transit INTEGER REFERENCES ordre_transit(id),

    est_groupage BOOLEAN  DEFAULT FALSE,
    id_phase INTEGER  REFERENCES phase_dossier(id),
    id_statut INTEGER  REFERENCES statut_dossier(id),

    echeance TIMESTAMPTZ ,
    pour_compte VARCHAR(100) ,

    id_pays_origine INTEGER REFERENCES pays(id),
    id_pays_destination INTEGER REFERENCES pays(id),

    est_prepaye BOOLEAN  DEFAULT FALSE,
    est_provisionne BOOLEAN  DEFAULT FALSE,

    provision_demandee NUMERIC(10, 2)  DEFAULT 0 CHECK (provision_demandee >= 0),
    lieu_paiement_fret VARCHAR(100) ,

    depart VARCHAR(100) ,
    date_livraison TIMESTAMPTZ ,
    adresse_livraison VARCHAR(200),

    id_inconterme INTEGER REFERENCES inconterme(id),

    date_limite_remise TIMESTAMPTZ,    -- date limite de remise des documents obligatoires

    date_creation TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE constat(
    id SERIAL PRIMARY KEY,
    id_dossier INTEGER  REFERENCES dossier(id),
    id_expert_constat INTEGER  REFERENCES expert_constat(id),
    id_type_constat INTEGER  REFERENCES type_constat(id),
    description TEXT ,
    date_creation TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE document(
    id SERIAL PRIMARY KEY,
    id_dossier INTEGER  REFERENCES dossier(id),
    id_type_document INTEGER  REFERENCES type_document(id),
    nombre INTEGER,
    chemin_fichier VARCHAR(200),
    nom_fichier VARCHAR(200),          -- nom original du fichier uploadé
    est_actif BOOLEAN  DEFAULT TRUE,   -- indique si le document est actif (non retiré)
    date_ajout TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE remise(
    id SERIAL PRIMARY KEY,
    id_dossier INTEGER  REFERENCES dossier(id),
    id_type_document INTEGER  REFERENCES type_document(id),
    date_remise TIMESTAMPTZ
);

CREATE TABLE expedition(
    id SERIAL PRIMARY KEY,
    -- Une expedition par ordre, sans obligation de creer un dossier.
    id_ordre_transit INTEGER UNIQUE REFERENCES ordre_transit(id),
    id_dossier INTEGER  REFERENCES dossier(id),

    est_achemine BOOLEAN  DEFAULT FALSE,
    id_mode_transport INTEGER REFERENCES mode_transport(id),
    id_mode_acheminement INTEGER REFERENCES mode_acheminement(id),

    id_compagnie INTEGER REFERENCES compagnie(id),
    numero_bl VARCHAR(100),
    id_navire INTEGER REFERENCES navire(id),
    date_bl TIMESTAMPTZ,
    lieu_bl VARCHAR(100),
    id_port_embarquement INTEGER REFERENCES station(id),
    id_port_debarquement INTEGER REFERENCES station(id),

    numero_vol VARCHAR(100),
    numero_lta VARCHAR(100),
    date_lta TIMESTAMPTZ,
    lieu_lta VARCHAR(100),
    id_aeroport_embarquement INTEGER REFERENCES station(id),
    id_aeroport_debarquement INTEGER REFERENCES station(id),

    lib_scan VARCHAR(200),
    date_depart TIMESTAMPTZ,
    date_prevu TIMESTAMPTZ,
    date_arrivee TIMESTAMPTZ

);

CREATE TABLE marchandise(
    id SERIAL PRIMARY KEY,
    id_ordre_transit INTEGER  REFERENCES ordre_transit(id),
    id_unite_mesure INTEGER  REFERENCES unite_mesure(id),


    marque VARCHAR(100),
    modele VARCHAR(100),
    description TEXT ,
    poids NUMERIC(10, 2)  CHECK (poids >= 0),
    volume NUMERIC(10, 2)  CHECK (volume >= 0),
    quantite INTEGER  CHECK (quantite > 0)
);

CREATE TABLE conteneur(
    id SERIAL PRIMARY KEY,
    id_dossier INTEGER REFERENCES dossier(id),
    id_type_container INTEGER REFERENCES type_container(id),

    numero_container VARCHAR(100) UNIQUE,
    description TEXT
);

CREATE TABLE dossier_conteneur(
    id_dossier INTEGER NOT NULL REFERENCES dossier(id) ON DELETE CASCADE,
    id_conteneur INTEGER NOT NULL REFERENCES conteneur(id) ON DELETE CASCADE,
    PRIMARY KEY (id_dossier, id_conteneur)
);

-- table pour les hbl et hawb
CREATE TABLE groupage(
    id SERIAL PRIMARY KEY,
    id_dossier INTEGER  REFERENCES dossier(id),
    id_expedition INTEGER REFERENCES expedition(id),

    numero_groupage VARCHAR(100)  UNIQUE,
    partie_notifier TEXT,

    date_creation TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE dossier_interagence(
    id SERIAL PRIMARY KEY,
    id_dossier_mere INTEGER  REFERENCES dossier(id),
    id_sous_dossier INTEGER  REFERENCES dossier(id),
    motif TEXT,
    date_creation TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE demande_interagence(
    id SERIAL PRIMARY KEY,
    id_dossier INTEGER NOT NULL REFERENCES dossier(id),
    id_agence_demandeuse INTEGER NOT NULL REFERENCES agence(id),
    id_agence_destinataire INTEGER NOT NULL REFERENCES agence(id),
    id_utilisateur_demande INTEGER NOT NULL REFERENCES utilisateur(id),
    id_utilisateur_reponse INTEGER REFERENCES utilisateur(id),
    motif TEXT,
    reponse_motif TEXT,
    statut VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE',
    est_vue BOOLEAN NOT NULL DEFAULT FALSE,
    date_demande TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_reponse TIMESTAMPTZ
);


CREATE TABLE mvt_dossier(
    id SERIAL PRIMARY KEY,
    id_dossier INTEGER  REFERENCES dossier(id),
    id_utilisateur INTEGER NOT NULL REFERENCES utilisateur(id),
    id_operation INTEGER  REFERENCES operation_dossier(id),

    id_ancienne_phase INTEGER REFERENCES phase_dossier(id),
    id_nouvelle_phase INTEGER REFERENCES phase_dossier(id),
    id_ancien_statut INTEGER REFERENCES statut_dossier(id),
    id_nouveau_statut INTEGER REFERENCES statut_dossier(id),

    commentaire TEXT,
    date_mvt TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE declaration(
    id SERIAL PRIMARY KEY,
    id_dossier INTEGER  REFERENCES dossier(id),
    numero_declaration VARCHAR(100) UNIQUE,
    date_declaration TIMESTAMPTZ ,
    id_bureau INTEGER  REFERENCES bureau(id),
    description TEXT,
    id_type_declaration INTEGER REFERENCES type_declaration(id),
    id_document INTEGER REFERENCES document(id),
    date_ajout TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE document_mvt(
    id SERIAL PRIMARY KEY,
    id_mvt_dossier INTEGER  REFERENCES mvt_dossier(id),
    id_document INTEGER  REFERENCES document(id)
);

-- Documents obligatoires a remettre avant de pouvoir effectuer le mouvement
-- d'une operation donnee (mvt_dossier). Si un type de document requis pour
-- l'operation n'a pas de document actif remis sur le dossier (table document),
-- la creation du mouvement de CETTE operation est refusee.
-- Les operations sans document requis restent realizables normalement.
CREATE TABLE document_obligatoire_mvt(
    id SERIAL PRIMARY KEY,
    id_operation INTEGER NOT NULL REFERENCES operation_dossier(id),
    id_type_document INTEGER NOT NULL REFERENCES type_document(id),
    description TEXT,
    UNIQUE (id_operation, id_type_document)
);

CREATE TABLE suspension_connexion(
    id SERIAL PRIMARY KEY,
    id_utilisateur INTEGER  UNIQUE REFERENCES utilisateur(id),
    tentatives_echouees INTEGER  DEFAULT 0,
    date_suspension TIMESTAMPTZ,
    duree_suspension_minutes INTEGER DEFAULT 15,
    date_creation TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE action(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100)  UNIQUE,
    description TEXT
);

CREATE TABLE historique(
    id SERIAL PRIMARY KEY,
    id_utilisateur INTEGER  REFERENCES utilisateur(id),
    hostname VARCHAR(100) ,
    url VARCHAR(200) ,
    logiciel VARCHAR(200),
    id_action INTEGER  REFERENCES action(id),
    adresse_ip VARCHAR(45),
    methode_http VARCHAR(10),
    statut_http INTEGER NOT NULL DEFAULT 200,
    entite VARCHAR(255),
    identifiant_entite VARCHAR(100),
    details TEXT ,
    date_action TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP
);

-- =========================================================================
-- 5. INDEX
-- =========================================================================

CREATE INDEX idx_utilisateur_agence ON utilisateur(id_agence);
CREATE INDEX idx_utilisateur_role ON utilisateur(id_role);

CREATE INDEX idx_clients_type_client ON clients(id_type_client);
CREATE INDEX idx_clients_pays ON clients(id_pays);

CREATE INDEX idx_type_production_mode_transport ON type_production(id_mode_transport);

CREATE INDEX idx_bureau_type_bureau ON bureau(id_type_bureau);

CREATE INDEX idx_compagnie_pays ON compagnie(id_pays);
CREATE INDEX idx_compagnie_mode_transport ON compagnie(id_mode_transport);

CREATE INDEX idx_navire_compagnie ON navire(id_compagnie);

CREATE INDEX idx_station_mode_transport ON station(id_mode_transport);

CREATE INDEX idx_type_document_type_production ON type_document(id_type_production);
CREATE INDEX idx_type_document_nature_marchandise ON type_document(id_nature_marchandise);

CREATE INDEX idx_ordre_transit_client ON ordre_transit(id_client);
CREATE INDEX idx_ordre_transit_agence ON ordre_transit(id_agence);
CREATE INDEX idx_ordre_transit_utilisateur ON ordre_transit(id_utilisateur);
CREATE INDEX idx_ordre_transit_type_production ON ordre_transit(id_type_production);
CREATE INDEX idx_ordre_transit_bureau_dedou ON ordre_transit(id_bureau_dedou);

CREATE INDEX idx_contrat_transport_ordre_transit ON contrat_transport(id_ordre_transit);
CREATE INDEX idx_contrat_transport_compagnie ON contrat_transport(id_compagnie);
CREATE INDEX idx_contrat_transport_navire ON contrat_transport(id_navire);

CREATE INDEX idx_instruction_ordre_transit ON instruction(id_ordre_transit);

CREATE INDEX idx_dossier_client ON dossier(id_client);
CREATE INDEX idx_dossier_agence ON dossier(id_agence);
CREATE INDEX idx_dossier_utilisateur ON dossier(id_utilisateur);
CREATE INDEX idx_dossier_ordre_transit ON dossier(id_ordre_transit);
CREATE INDEX idx_dossier_phase ON dossier(id_phase);
CREATE INDEX idx_dossier_statut ON dossier(id_statut);
CREATE INDEX idx_dossier_type_production ON dossier(id_type_production);
CREATE INDEX idx_dossier_inconterme ON dossier(id_inconterme);

CREATE INDEX idx_constat_dossier ON constat(id_dossier);
CREATE INDEX idx_document_dossier ON document(id_dossier);
CREATE INDEX idx_document_type_document ON document(id_type_document);
CREATE INDEX idx_remise_dossier ON remise(id_dossier);
CREATE INDEX idx_remise_type_document ON remise(id_type_document);

CREATE INDEX idx_expedition_dossier ON expedition(id_dossier);
CREATE INDEX idx_expedition_compagnie ON expedition(id_compagnie);
CREATE INDEX idx_expedition_navire ON expedition(id_navire);
CREATE INDEX idx_expedition_mode_acheminement ON expedition(id_mode_acheminement);

CREATE INDEX idx_marchandise_ordre_transit ON marchandise(id_ordre_transit);
CREATE INDEX idx_marchandise_unite_mesure ON marchandise(id_unite_mesure);
CREATE INDEX idx_ordre_transit_nature_marchandise ON ordre_transit(id_nature_marchandise);

CREATE INDEX idx_conteneur_dossier ON conteneur(id_dossier);
CREATE INDEX idx_conteneur_type_container ON conteneur(id_type_container);

CREATE INDEX idx_groupage_dossier ON groupage(id_dossier);
CREATE INDEX idx_groupage_expedition ON groupage(id_expedition);

CREATE INDEX idx_dossier_interagence_mere ON dossier_interagence(id_dossier_mere);
CREATE INDEX idx_dossier_interagence_sous ON dossier_interagence(id_sous_dossier);

CREATE INDEX idx_mvt_dossier_dossier ON mvt_dossier(id_dossier);
CREATE INDEX idx_mvt_dossier_utilisateur ON mvt_dossier(id_utilisateur);
CREATE INDEX idx_mvt_dossier_operation ON mvt_dossier(id_operation);

CREATE INDEX idx_declaration_dossier ON declaration(id_dossier);
CREATE INDEX idx_declaration_bureau ON declaration(id_bureau);
CREATE INDEX idx_declaration_type_declaration ON declaration(id_type_declaration);
CREATE INDEX idx_declaration_document ON declaration(id_document);

CREATE INDEX idx_document_mvt_mvt ON document_mvt(id_mvt_dossier);
CREATE INDEX idx_document_mvt_doc ON document_mvt(id_document);

-- Index pour 
CREATE INDEX idx_historique_utilisateur ON historique(id_utilisateur);
CREATE INDEX idx_historique_action      ON historique(id_action);
CREATE INDEX idx_historique_date_action ON historique(date_action);

CREATE INDEX idx_historique_utilisateur_date ON historique(id_utilisateur, date_action);
CREATE INDEX idx_historique_adresse_ip ON historique(adresse_ip);
CREATE INDEX idx_historique_entite ON historique(entite);
-- Recherche des dossiers : fonctions, vecteurs, declencheurs et index.
-- Keep identifiers separated by hyphens searchable as individual tokens.
CREATE OR REPLACE FUNCTION auximad_vecteur(texte text)
RETURNS tsvector LANGUAGE sql IMMUTABLE PARALLEL SAFE AS $$
    SELECT to_tsvector('simple', regexp_replace(COALESCE(texte, ''), '[^[:alnum:]]+', ' ', 'g'));
$$;

-- Indexed full-text searches for dossier filters and the global search.
-- Safe to run again: only missing vectors are backfilled.
CREATE OR REPLACE FUNCTION auximad_recherche_prefixe(texte text)
RETURNS tsquery LANGUAGE sql IMMUTABLE PARALLEL SAFE AS $$
    SELECT to_tsquery('simple', COALESCE(string_agg(quote_literal(lexeme) || ':*', ' & '), ''))
    FROM unnest(tsvector_to_array(auximad_vecteur(COALESCE(texte, '')))) AS mots(lexeme);
$$;

ALTER TABLE dossier ADD COLUMN IF NOT EXISTS recherche_fts tsvector;

CREATE OR REPLACE FUNCTION auximad_dossier_vecteur(
    numero text, compte text, lieu_depart text, livraison text, paiement text,
    client_nom text, client_prenom text, client_cp text)
RETURNS tsvector LANGUAGE sql IMMUTABLE PARALLEL SAFE AS $$
    SELECT auximad_vecteur(COALESCE(numero, '') || ' ' || COALESCE(compte, '') || ' ' ||
        COALESCE(lieu_depart, '') || ' ' || COALESCE(livraison, '') || ' ' ||
        COALESCE(paiement, '') || ' ' || COALESCE(client_nom, '') || ' ' ||
        COALESCE(client_prenom, '') || ' ' || COALESCE(client_cp, ''));
$$;

CREATE OR REPLACE FUNCTION auximad_dossier_actualiser_fts()
RETURNS trigger LANGUAGE plpgsql SET search_path FROM CURRENT AS $$
DECLARE client_ligne record;
BEGIN
    SELECT nom, prenom, cp_client INTO client_ligne FROM clients WHERE id = NEW.id_client;
    NEW.recherche_fts := auximad_dossier_vecteur(NEW.numero_dossier, NEW.pour_compte,
        NEW.depart, NEW.adresse_livraison, NEW.lieu_paiement_fret,
        client_ligne.nom, client_ligne.prenom, client_ligne.cp_client);
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS dossier_actualiser_fts ON dossier;
CREATE TRIGGER dossier_actualiser_fts
BEFORE INSERT OR UPDATE OF numero_dossier, pour_compte, depart, adresse_livraison,
    lieu_paiement_fret, id_client ON dossier
FOR EACH ROW EXECUTE FUNCTION auximad_dossier_actualiser_fts();

CREATE OR REPLACE FUNCTION auximad_client_actualiser_dossiers_fts()
RETURNS trigger LANGUAGE plpgsql SET search_path FROM CURRENT AS $$
BEGIN
    IF (NEW.nom, NEW.prenom, NEW.cp_client) IS DISTINCT FROM (OLD.nom, OLD.prenom, OLD.cp_client) THEN
        UPDATE dossier SET recherche_fts = auximad_dossier_vecteur(numero_dossier, pour_compte,
            depart, adresse_livraison, lieu_paiement_fret, NEW.nom, NEW.prenom, NEW.cp_client)
        WHERE id_client = NEW.id;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS client_actualiser_dossiers_fts ON clients;
CREATE TRIGGER client_actualiser_dossiers_fts
AFTER UPDATE OF nom, prenom, cp_client ON clients
FOR EACH ROW EXECUTE FUNCTION auximad_client_actualiser_dossiers_fts();

UPDATE dossier d SET recherche_fts = auximad_dossier_vecteur(d.numero_dossier, d.pour_compte,
    d.depart, d.adresse_livraison, d.lieu_paiement_fret, c.nom, c.prenom, c.cp_client)
FROM clients c WHERE c.id = d.id_client AND d.recherche_fts IS NULL;

CREATE INDEX IF NOT EXISTS idx_dossier_recherche_fts ON dossier USING GIN (recherche_fts);
CREATE INDEX IF NOT EXISTS idx_dossier_numerodossier_fts ON dossier USING GIN (auximad_vecteur(COALESCE(numero_dossier, '')));
CREATE INDEX IF NOT EXISTS idx_clients_clientnom_fts ON clients USING GIN (auximad_vecteur(COALESCE(nom, '') || ' ' || COALESCE(prenom, '')));
CREATE INDEX IF NOT EXISTS idx_clients_cpclient_fts ON clients USING GIN (auximad_vecteur(COALESCE(cp_client, '')));
CREATE INDEX IF NOT EXISTS idx_dossier_pourcompte_fts ON dossier USING GIN (auximad_vecteur(COALESCE(pour_compte, '')));
CREATE INDEX IF NOT EXISTS idx_dossier_depart_fts ON dossier USING GIN (auximad_vecteur(COALESCE(depart, '')));
CREATE INDEX IF NOT EXISTS idx_dossier_adresselivraison_fts ON dossier USING GIN (auximad_vecteur(COALESCE(adresse_livraison, '')));
CREATE INDEX IF NOT EXISTS idx_dossier_lieupaiementfret_fts ON dossier USING GIN (auximad_vecteur(COALESCE(lieu_paiement_fret, '')));
