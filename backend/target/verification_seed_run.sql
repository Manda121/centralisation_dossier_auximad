\set ON_ERROR_STOP on
CREATE SCHEMA seed_verification_a3149010fe0f4ae9abe6e03357a6a209;
SET search_path TO seed_verification_a3149010fe0f4ae9abe6e03357a6a209;
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


-- Configuration et historique des emails interagences
CREATE TABLE adresse_email_agence (
    id SERIAL PRIMARY KEY,
    id_agence INTEGER NOT NULL REFERENCES agence(id),
    email VARCHAR(254) NOT NULL,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (id_agence, email),
    CHECK (email = lower(trim(email)))
);

CREATE TABLE historique_mail (
    id BIGSERIAL PRIMARY KEY,
    id_demande_interagence INTEGER NOT NULL REFERENCES demande_interagence(id),
    id_agence INTEGER NOT NULL REFERENCES agence(id),
    nom_agence VARCHAR(100) NOT NULL,
    numero_dossier VARCHAR(100) NOT NULL,
    destinataire VARCHAR(254),
    expediteur VARCHAR(254),
    sujet VARCHAR(255) NOT NULL,
    corps TEXT NOT NULL,
    statut VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE'
        CHECK (statut IN ('EN_ATTENTE', 'ENVOYE', 'ECHEC', 'NON_CONFIGURE', 'ANNULE')),
    erreur TEXT,
    date_creation TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_tentative TIMESTAMPTZ,
    date_envoi TIMESTAMPTZ,
    id_mail_origine BIGINT REFERENCES historique_mail(id)
);

CREATE INDEX idx_historique_mail_file ON historique_mail(statut, id);
CREATE INDEX idx_historique_mail_agence ON historique_mail(id_agence, id DESC);
CREATE INDEX idx_historique_mail_demande ON historique_mail(id_demande_interagence);

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
    date_mvt TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP,
    origine VARCHAR(30) DEFAULT 'SAISIE',
    groupe_ajustement VARCHAR(36),
    est_annule BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE mvt_annulee(
    id SERIAL PRIMARY KEY,
    id_mvt_dossier INTEGER NOT NULL UNIQUE REFERENCES mvt_dossier(id) ON DELETE CASCADE,
    date_annulation TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    motif_annulation TEXT,
    id_utilisateur_annulation INTEGER REFERENCES utilisateur(id)
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

-- Jeu de démonstration : à charger après base_centralisation.sql sur une base vide.
-- Trois phases, workflows import/export, FOB uniquement, neuf agences.
-- Les dates couvrent les douze derniers mois avec des volumes mensuels variables.
BEGIN;

-- ============================================================
-- 0. STATUT DOSSIER (nouvelle table)
-- ============================================================
INSERT INTO statut_dossier (nom, description) VALUES
('Ouvert', 'Dossier créé, aucune opération effectuée'),
('Clôturé', 'Dossier finalisé après paiement et clôture'),
('Annulé', 'Dossier annulé'),
('En litige', 'Dossier suspendu pour un litige'),
('Client avisé', 'Dernière opération effectuée : Avis d’arrivée client'),
('Proforma envoyée', 'Dernière opération effectuée : Envoi proforma'),
('Premier envoi effectué', 'Dernière opération effectuée : Premier envoi document'),
('Premiers documents reçus', 'Dernière opération effectuée : Premier réception document'),
('Expertise réalisée', 'Dernière opération effectuée : Expertise'),
('Déclaration établie', 'Dernière opération effectuée : Déclaration'),
('Dossier transmis au passeur', 'Dernière opération effectuée : Dispatche/Passeur'),
('Visite/scan effectué', 'Dernière opération effectuée : Visite/Scan'),
('Droits et taxes payés', 'Dernière opération effectuée : Paiement Droit & Taxes'),
('Paiement des droits confirmé', 'Dernière opération effectuée : Confirmation paiement Droit & Taxes'),
('Provision enregistrée', 'Dernière opération effectuée : Provision (pour fret et débours)'),
('Facturé', 'Dernière opération effectuée : Facturation'),
('Facture transmise', 'Dernière opération effectuée : Transmission'),
('Payé', 'Dernière opération effectuée : Paiement'),
('Préalerte envoyée', 'Dernière opération effectuée : Préalerte'),
('Réservation confirmée', 'Dernière opération effectuée : Réservation TC/Ligne'),
('Documents reçus', 'Dernière opération effectuée : Réception Doc'),
('Conteneur positionné', 'Dernière opération effectuée : Position container'),
('Déclaration déposée', 'Dernière opération effectuée : Dépôt Déclaration'),
('PG payé', 'Dernière opération effectuée : Paiement PG'),
('Bon à enlever obtenu', 'Dernière opération effectuée : Bon à enlever'),
('Documents récupérés', 'Dernière opération effectuée : Récupération documents');

-- ============================================================
-- 0bis. STATUT ORDRE DE TRANSIT
-- ============================================================
INSERT INTO statut_ot (nom, description) VALUES
('Brouillon', 'Ordre de transit en preparation'),
('Non valide', 'Ordre de transit en attente de validation'),
('Valide', 'Ordre de transit valide');

-- ============================================================
-- 1. TABLES DE REFERENCE
-- ============================================================

INSERT INTO agence (code, nom, adresse, ville, cp_client) VALUES
(20, 'TNR', '18, RUE J.J. Rabearivelo-Antsahavola', 'Antananarivo', '101'),
(90, 'DIR', '18, RUE J.J. Rabearivelo-Antsahavola', 'Antananarivo', '101'),
(10, 'TMA', '21, Bd Ratsimilaho Po Box 93- 501', 'Toamasina', '501'),
(30, 'MJG', 'Avenue de France, Majunga', 'Mahajanga', '401'),
(70, 'DIEGO', 'Place Kabary, Rue Flacourt', 'Antsiranana', '201'),
(50, 'TUL', 'Amorondriaka,Face Zaza club centre ville', 'Toliara', '601'),
(21, 'A/BE', 'Villa Francois Lot 02-B-177', 'Antsirabe', '110'),
(80, 'NOSY BE', 'Rue de Passot, Hell-Ville BP:1', 'Nosy Be', '207'),
(93, 'FORT-Dauphin', 'Rue Marechal Foch', 'Fort-Dauphin', '614');

INSERT INTO role (nom, description) VALUES
('Admin', 'Administrateur systeme'),
('Agent', 'Agent de transit'),
('ChefAgence', 'Chef d''agence'),
('Lecteur', 'Consultation seule'),
('Validateur', 'Validation des operations');

INSERT INTO mode_transport (nom, description) VALUES
('Maritime', 'Transport par mer'),
('Aerien', 'Transport par avion'),
('combine', 'a la fois transport marin et aerien'),
('Terrestre', 'Transport par route');

INSERT INTO type_production (nom, id_mode_transport, code, description) VALUES
('import', (SELECT id FROM mode_transport WHERE nom = 'Maritime'), 1, 'import maritime'),
('import', (SELECT id FROM mode_transport WHERE nom = 'Aerien'), 4, 'import aerien'),
('export', (SELECT id FROM mode_transport WHERE nom = 'Maritime'), 0, 'export maritime'),
('export', (SELECT id FROM mode_transport WHERE nom = 'Aerien'), 0, 'export aerien'),
('divers', (SELECT id FROM mode_transport WHERE nom = 'Terrestre'), 9, 'divers terrestre'),
('colis postaux', (SELECT id FROM mode_transport WHERE nom = 'Terrestre'), 2, 'transport de colis');

INSERT INTO type_client (nom, description) VALUES
('Particulier', 'Client individuel'),
('Entreprise', 'Client entreprise'),
('Institution', 'Client institutionnel');

INSERT INTO type_regime (nom, code, description) VALUES
('Mise a la consommation', 'MCI', 'Importation definitive'),
('Exportation', 'EXP', 'Exportation simple'),
('Transit', 'TRS', 'Transit international'),
('Entrepot', 'ENT', 'Regime d''entrepot douanier'),
('Admission temporaire', 'ADT', 'Admission temporaire');

-- type_bureau
INSERT INTO type_bureau (nom, description) VALUES
('Bureau de transfert', 'Bureau de transfert'),
('Bureau mise en entrepot', 'Bureau de mise en entrepot'),
('Bureau de douane', 'Bureau de douane'),
('Bureau principal', 'Bureau principal'),
('Bureau secondaire', 'Bureau secondaire'),
('Bureau de controle', 'Bureau de controle'),
('Bureau d''enquete', 'Bureau d''enquete');

INSERT INTO bureau (nom, description, id_type_bureau, adresse, cp_client, ville) VALUES
('Bureau des douanes Antananarivo', 'Bureau principal des douanes a Antananarivo', (SELECT id FROM type_bureau WHERE nom = 'Bureau principal'), 'Rue de l''Independance', '101', 'Antananarivo'),
('Bureau des douanes Toamasina', 'Bureau de douane du port', (SELECT id FROM type_bureau WHERE nom = 'Bureau de douane'), 'Boulevard Joffre', '501', 'Toamasina'),
('Bureau secondaire Antsirabe', 'Bureau secondaire', (SELECT id FROM type_bureau WHERE nom = 'Bureau secondaire'), 'Avenue de la Gare', '110', 'Antsirabe'),
('Bureau de controle Ivato', 'Bureau de controle aeroport', (SELECT id FROM type_bureau WHERE nom = 'Bureau de controle'), 'Aeroport Ivato', '105', 'Antananarivo'),
('Bureau d''enquete Mahajanga', 'Bureau d''enquete', (SELECT id FROM type_bureau WHERE nom = 'Bureau d''enquete'), 'Avenue Philibert Tsiranana', '401', 'Mahajanga'),
('Bureau des douanes Antsiranana', 'Bureau de douane', (SELECT id FROM type_bureau WHERE nom = 'Bureau de douane'), 'Rue Colbert', '201', 'Antsiranana'),
('Bureau secondaire Fianarantsoa', 'Bureau secondaire', (SELECT id FROM type_bureau WHERE nom = 'Bureau secondaire'), 'Avenue de l''Independance', '301', 'Fianarantsoa'),
('Bureau de controle Nosy Be', 'Bureau de controle', (SELECT id FROM type_bureau WHERE nom = 'Bureau de controle'), 'Hell-Ville', '207', 'Nosy Be'),
('Bureau des douanes Toliara', 'Bureau de douane', (SELECT id FROM type_bureau WHERE nom = 'Bureau de douane'), 'Boulevard Gallieni', '601', 'Toliara'),
('Bureau secondaire Morondava', 'Bureau secondaire', (SELECT id FROM type_bureau WHERE nom = 'Bureau secondaire'), 'Boulevard de la Plage', '619', 'Morondava'),
('Bureau des douanes Sambava', 'Bureau de douane', (SELECT id FROM type_bureau WHERE nom = 'Bureau de douane'), 'Route de la Vanille', '208', 'Sambava'),
('Bureau principal Tamatave', 'Bureau principal', (SELECT id FROM type_bureau WHERE nom = 'Bureau principal'), 'Boulevard Joffre', '501', 'Toamasina'),
('Bureau de controle Aeroport Ivato', 'Bureau de controle', (SELECT id FROM type_bureau WHERE nom = 'Bureau de controle'), 'Aeroport Ivato', '105', 'Antananarivo'),
('Bureau d''enquete Antananarivo', 'Bureau d''enquete', (SELECT id FROM type_bureau WHERE nom = 'Bureau d''enquete'), 'Rue J.J. Rabearivelo', '101', 'Antananarivo'),
('Bureau secondaire Mahajanga', 'Bureau secondaire', (SELECT id FROM type_bureau WHERE nom = 'Bureau secondaire'), 'Avenue de France', '401', 'Mahajanga'),
('Bureau de douane Port Toamasina', 'Bureau de douane', (SELECT id FROM type_bureau WHERE nom = 'Bureau de douane'), 'Port Toamasina', '501', 'Toamasina'),
('Bureau de controle Port Toamasina', 'Bureau de controle', (SELECT id FROM type_bureau WHERE nom = 'Bureau de controle'), 'Port Toamasina', '501', 'Toamasina'),
('Bureau secondaire Antsiranana', 'Bureau secondaire', (SELECT id FROM type_bureau WHERE nom = 'Bureau secondaire'), 'Rue Colbert', '201', 'Antsiranana'),
('Bureau des douanes Ivato', 'Bureau de douane', (SELECT id FROM type_bureau WHERE nom = 'Bureau de douane'), 'Aeroport Ivato', '105', 'Antananarivo'),
('Bureau secondaire Toliara', 'Bureau secondaire', (SELECT id FROM type_bureau WHERE nom = 'Bureau secondaire'), 'Boulevard Gallieni', '601', 'Toliara');

INSERT INTO mode_acheminement (nom, description) VALUES
('pas notre soins', '...'),
('Sous D.15(T,O)', '...'),
('Sous D.SO(T.I)', '...'),
('Par train', '...'),
('Par Avion', '...'),
('Par Camion', '...');

-- NATURE MARCHANDISE (remplace nature_marchandise)
INSERT INTO nature_marchandise (nom, description) VALUES
('Textile', 'Textile et habillement'),
('Electronique', 'Produits electroniques'),
('Alimentaire', 'Produits alimentaires'),
('Pharmacie', 'Produits pharmaceutiques'),
('Materiaux de construction', 'Materiaux de construction'),
('Vehicules', 'Vehicules'),
('Machines industrielles', 'Machines industrielles'),
('Produits chimiques', 'Produits chimiques'),
('Bois', 'Bois et derives'),
('Autres', 'Autres marchandises');

-- Trois phases communes aux flux import et export.
-- PREPARATION et Ouvert conservent les noms attendus par le service Java.
INSERT INTO phase_dossier (nom, id_mode_transport, description) VALUES
('PREPARATION', NULL, 'Préparation documentaire et expertise'),
('TRAITEMENT', NULL, 'Formalités douanières et règlements associés'),
('COMPTABILITE', NULL, 'Provision, facturation, paiement et clôture');

-- Première liste = import, seconde liste = export.
-- Chaque opération est déclinée en maritime et aérien. Le niveau_operation
-- suit strictement l'ordre des étapes, du début à la clôture de chaque flux.
-- Le statut associé décrit le résultat de l'opération qui vient d'être faite.
INSERT INTO operation_dossier
    (description, id_phase, id_statut, id_type_production,
     niveau_autorisation, niveau_validation, niveau_operation, est_obligatoire, est_actif)
SELECT t.description, ph.id, st.id, tp.id,
       CASE t.phase WHEN 'PREPARATION' THEN 1 WHEN 'TRAITEMENT' THEN 2 ELSE 4 END,
       CASE WHEN t.description = 'Clôture' THEN 5
            WHEN t.phase = 'COMPTABILITE' THEN 4 WHEN t.phase = 'TRAITEMENT' THEN 2 ELSE 1 END,
       t.rang, TRUE, TRUE
FROM (VALUES
    ('import', 'PREPARATION', 'Avis d’arrivée client', 'Client avisé', 1),
    ('import', 'PREPARATION', 'Envoi proforma', 'Proforma envoyée', 2),
    ('import', 'PREPARATION', 'Premier envoi document', 'Premier envoi effectué', 3),
    ('import', 'PREPARATION', 'Premier réception document', 'Premiers documents reçus', 4),
    ('import', 'PREPARATION', 'Expertise', 'Expertise réalisée', 5),
    ('import', 'TRAITEMENT', 'Déclaration', 'Déclaration établie', 6),
    ('import', 'TRAITEMENT', 'Dispatche/Passeur', 'Dossier transmis au passeur', 7),
    ('import', 'TRAITEMENT', 'Visite/Scan', 'Visite/scan effectué', 8),
    ('import', 'TRAITEMENT', 'Paiement Droit & Taxes', 'Droits et taxes payés', 9),
    ('import', 'TRAITEMENT', 'Confirmation paiement Droit & Taxes', 'Paiement des droits confirmé', 10),
    ('import', 'COMPTABILITE', 'Provision (pour fret et débours)', 'Provision enregistrée', 11),
    ('import', 'COMPTABILITE', 'Facturation', 'Facturé', 12),
    ('import', 'COMPTABILITE', 'Transmission', 'Facture transmise', 13),
    ('import', 'COMPTABILITE', 'Paiement', 'Payé', 14),
    ('import', 'COMPTABILITE', 'Clôture', 'Clôturé', 15),
    ('export', 'PREPARATION', 'Préalerte', 'Préalerte envoyée', 1),
    ('export', 'PREPARATION', 'Réservation TC/Ligne', 'Réservation confirmée', 2),
    ('export', 'PREPARATION', 'Réception Doc', 'Documents reçus', 3),
    ('export', 'PREPARATION', 'Envoi proforma', 'Proforma envoyée', 4),
    ('export', 'PREPARATION', 'Position container', 'Conteneur positionné', 5),
    ('export', 'PREPARATION', 'Expertise', 'Expertise réalisée', 6),
    ('export', 'TRAITEMENT', 'Déclaration', 'Déclaration établie', 7),
    ('export', 'TRAITEMENT', 'Dispatche/Passeur', 'Dossier transmis au passeur', 8),
    ('export', 'TRAITEMENT', 'Dépôt Déclaration', 'Déclaration déposée', 9),
    ('export', 'TRAITEMENT', 'Paiement PG', 'PG payé', 10),
    ('export', 'TRAITEMENT', 'Bon à enlever', 'Bon à enlever obtenu', 11),
    ('export', 'TRAITEMENT', 'Récupération documents', 'Documents récupérés', 12),
    ('export', 'COMPTABILITE', 'Provision', 'Provision enregistrée', 13),
    ('export', 'COMPTABILITE', 'Facturation', 'Facturé', 14),
    ('export', 'COMPTABILITE', 'Transmission', 'Facture transmise', 15),
    ('export', 'COMPTABILITE', 'Paiement', 'Payé', 16),
    ('export', 'COMPTABILITE', 'Clôture', 'Clôturé', 17)
) AS t(sens, phase, description, statut, rang)
JOIN type_production tp ON tp.nom = t.sens
JOIN phase_dossier ph ON ph.nom = t.phase
JOIN statut_dossier st ON st.nom = t.statut
ORDER BY tp.id, t.rang;

INSERT INTO inconterme (nom, abreviation, description) VALUES
('Free On Board', 'FOB', 'Franco à bord');

INSERT INTO type_constat (nom, description) VALUES
('constat ...', 'Constat systematique a chaque transfert de responsabilite'),
('constat d avarie', 'Constat unique en cas d avarie et/ou portes apparentes'),
('pas de constat', 'Pas de constat');

INSERT INTO expert_constat (nom, prenom, email, telephone, adresse) VALUES
('Rakoto', 'Jean', 'jean.rakoto@expert.mg', '0321111111', 'Antananarivo'),
('Rabe', 'Marie', 'marie.rabe@expert.mg', '0322222222', 'Toamasina'),
('Randria', 'Paul', 'paul.randria@expert.mg', '0323333333', 'Mahajanga'),
('Razafy', 'Lucie', 'lucie.razafy@expert.mg', '0324444444', 'Antsiranana'),
('Andrianina', 'Serge', 'serge.andrianina@expert.mg', '0325555555', 'Toliara'),
('Rasoamanana', 'Hanta', 'hanta.rasoamanana@expert.mg', '0326666666', 'Fianarantsoa'),
('Rakotomalala', 'Miora', 'miora.rakotomalala@expert.mg', '0327777777', 'Nosy Be'),
('Ranaivo', 'Tiana', 'tiana.ranaivo@expert.mg', '0328888888', 'Antsirabe'),
('Rasoarimalala', 'Faly', 'faly.rasoarimalala@expert.mg', '0329999999', 'Morondava'),
('Rakotonirina', 'Voahangy', 'voahangy.rakotonirina@expert.mg', '0321010101', 'Sambava');

INSERT INTO pays (nom, code) VALUES
('France', 'FRA'),
('Chine', 'CHN'),
('Etats-Unis', 'USA'),
('Allemagne', 'DEU'),
('Italie', 'ITA'),
('Espagne', 'ESP'),
('Royaume-Uni', 'GBR'),
('Japon', 'JPN'),
('Inde', 'IND'),
('Bresil', 'BRA'),
('Canada', 'CAN'),
('Australie', 'AUS'),
('Afrique du Sud', 'ZAF'),
('Maroc', 'MAR'),
('Maurice', 'MUS'),
('Madagascar', 'MDG'),
('Comores', 'COM'),
('La Reunion', 'REU'),
('Kenya', 'KEN'),
('Tanzanie', 'TZA'),
('Mozambique', 'MOZ'),
('Coree du Sud', 'KOR'),
('Singapour', 'SGP'),
('Malaisie', 'MYS'),
('Thailande', 'THA'),
('Vietnam', 'VNM'),
('Indonesie', 'IDN'),
('Philippines', 'PHL'),
('Belgique', 'BEL'),
('Pays-Bas', 'NLD'),
('Suisse', 'CHE'),
('Suede', 'SWE'),
('Norvege', 'NOR'),
('Danemark', 'DNK'),
('Finlande', 'FIN'),
('Pologne', 'POL'),
('Turquie', 'TUR'),
('Egypte', 'EGY'),
('Emirats arabes unis', 'ARE'),
('Arabie saoudite', 'SAU'),
('Nigeria', 'NGA'),
('Senegal', 'SEN'),
('Cote d''Ivoire', 'CIV'),
('Cameroun', 'CMR'),
('Taiwan', 'TWN'),
('Qatar', 'QAT'),
('Ethiopie', 'ETH');

-- ============================================================
-- TYPE_DECLARATION
-- ============================================================
INSERT INTO type_declaration (nom, description) VALUES
('Mise à la consommation (IM4)', 'Déclaration pour mise à la consommation directe'),
('Exportation définitive (EX1)', 'Déclaration d''exportation définitive'),
('Transit international (TR)', 'Déclaration de transit international'),
('Admission temporaire (AT)', 'Déclaration pour régime d''admission temporaire'),
('Entrepôt douanier (E2)', 'Déclaration pour mise en entrepôt douanier');

-- ============================================================
-- TYPE_DOCUMENT : id=1 pour attachement mvt, id=2 pour déclaration
-- ============================================================
-- INSERT INTO type_document (id, nom, id_type_production, id_nature_marchandise, est_obligatoire, description) VALUES
-- (1, 'Document d''attachement', NULL, NULL, FALSE, 'Document d''attachement pour les mouvements de dossier'),
-- (2, 'Document de déclaration', NULL, NULL, FALSE, 'Document de déclaration douanière');

INSERT INTO type_document (nom, id_type_production, id_nature_marchandise, est_obligatoire, description) VALUES
('Document d''attachement', NULL, NULL, FALSE, 'Document d''attachement pour les mouvements de dossier'),
('Document de déclaration', NULL, NULL, FALSE, 'Document de déclaration douanière'),
('Facture commerciale', (SELECT id FROM type_production WHERE nom = 'import' AND id_mode_transport = (SELECT id FROM mode_transport WHERE nom = 'Maritime')), NULL, TRUE, 'Facture commerciale'),
('Liste de colisage', (SELECT id FROM type_production WHERE nom = 'import' AND id_mode_transport = (SELECT id FROM mode_transport WHERE nom = 'Maritime')), NULL, TRUE, 'Liste de colisage'),
('Connaissement maritime', (SELECT id FROM type_production WHERE nom = 'import' AND id_mode_transport = (SELECT id FROM mode_transport WHERE nom = 'Maritime')), NULL, TRUE, 'Connaissement maritime'),
('Lettre de transport aerien', (SELECT id FROM type_production WHERE nom = 'import' AND id_mode_transport = (SELECT id FROM mode_transport WHERE nom = 'Aerien')), NULL, TRUE, 'Lettre de transport aerien'),
('Certificat d''origine', (SELECT id FROM type_production WHERE nom = 'export' AND id_mode_transport = (SELECT id FROM mode_transport WHERE nom = 'Maritime')), NULL, TRUE, 'Certificat d''origine'),
('Certificat phytosanitaire', (SELECT id FROM type_production WHERE nom = 'export' AND id_mode_transport = (SELECT id FROM mode_transport WHERE nom = 'Maritime')), (SELECT id FROM nature_marchandise WHERE nom = 'Alimentaire'), FALSE, 'Certificat phytosanitaire'),
('Certificat de qualite', (SELECT id FROM type_production WHERE nom = 'import' AND id_mode_transport = (SELECT id FROM mode_transport WHERE nom = 'Maritime')), NULL, TRUE, 'Certificat de qualite'),
('Certificat d''assurance', (SELECT id FROM type_production WHERE nom = 'import' AND id_mode_transport = (SELECT id FROM mode_transport WHERE nom = 'Maritime')), NULL, FALSE, 'Certificat d''assurance'),
('Licence d''importation', (SELECT id FROM type_production WHERE nom = 'import' AND id_mode_transport = (SELECT id FROM mode_transport WHERE nom = 'Maritime')), NULL, TRUE, 'Licence d''importation'),
('Permis d''exportation', (SELECT id FROM type_production WHERE nom = 'export' AND id_mode_transport = (SELECT id FROM mode_transport WHERE nom = 'Maritime')), NULL, FALSE, 'Permis d''exportation'),
('Attestation de verification', (SELECT id FROM type_production WHERE nom = 'import' AND id_mode_transport = (SELECT id FROM mode_transport WHERE nom = 'Maritime')), NULL, TRUE, 'Attestation de verification'),
('Bordereau de suivi', (SELECT id FROM type_production WHERE nom = 'divers' AND id_mode_transport = (SELECT id FROM mode_transport WHERE nom = 'Terrestre')), NULL, TRUE, 'Bordereau de suivi'),
('Declaration en douane', (SELECT id FROM type_production WHERE nom = 'import' AND id_mode_transport = (SELECT id FROM mode_transport WHERE nom = 'Maritime')), NULL, TRUE, 'Declaration en douane'),
('Facture proforma', (SELECT id FROM type_production WHERE nom = 'import' AND id_mode_transport = (SELECT id FROM mode_transport WHERE nom = 'Maritime')), NULL, FALSE, 'Facture proforma'),
('Bon de commande', (SELECT id FROM type_production WHERE nom = 'import' AND id_mode_transport = (SELECT id FROM mode_transport WHERE nom = 'Maritime')), NULL, FALSE, 'Bon de commande'),
('Certificat de non-manipulation', (SELECT id FROM type_production WHERE nom = 'export' AND id_mode_transport = (SELECT id FROM mode_transport WHERE nom = 'Maritime')), (SELECT id FROM nature_marchandise WHERE nom = 'Alimentaire'), FALSE, 'Certificat de non-manipulation'),
('Certificat veterinaire', (SELECT id FROM type_production WHERE nom = 'export' AND id_mode_transport = (SELECT id FROM mode_transport WHERE nom = 'Maritime')), (SELECT id FROM nature_marchandise WHERE nom = 'Alimentaire'), FALSE, 'Certificat veterinaire'),
('Certificat de salubrite', (SELECT id FROM type_production WHERE nom = 'export' AND id_mode_transport = (SELECT id FROM mode_transport WHERE nom = 'Maritime')), (SELECT id FROM nature_marchandise WHERE nom = 'Alimentaire'), FALSE, 'Certificat de salubrite'),
('Fiche technique', (SELECT id FROM type_production WHERE nom = 'import' AND id_mode_transport = (SELECT id FROM mode_transport WHERE nom = 'Maritime')), NULL, FALSE, 'Fiche technique'),
('Rapport d''inspection', (SELECT id FROM type_production WHERE nom = 'import' AND id_mode_transport = (SELECT id FROM mode_transport WHERE nom = 'Maritime')), NULL, FALSE, 'Rapport d''inspection'),
('Preuve de paiement', (SELECT id FROM type_production WHERE nom = 'import' AND id_mode_transport = (SELECT id FROM mode_transport WHERE nom = 'Maritime')), NULL, FALSE, 'Preuve de paiement'),
('Recu de depot', (SELECT id FROM type_production WHERE nom = 'colis postaux' AND id_mode_transport = (SELECT id FROM mode_transport WHERE nom = 'Terrestre')), NULL, FALSE, 'Recu de depot'),
('Lettre de credit', (SELECT id FROM type_production WHERE nom = 'import' AND id_mode_transport = (SELECT id FROM mode_transport WHERE nom = 'Maritime')), NULL, FALSE, 'Lettre de credit'),
('Contrat de transport', (SELECT id FROM type_production WHERE nom = 'divers' AND id_mode_transport = (SELECT id FROM mode_transport WHERE nom = 'Terrestre')), NULL, FALSE, 'Contrat de transport'),
('Packing list', (SELECT id FROM type_production WHERE nom = 'import' AND id_mode_transport = (SELECT id FROM mode_transport WHERE nom = 'Maritime')), NULL, TRUE, 'Packing list'),
('Certificat de fret', (SELECT id FROM type_production WHERE nom = 'import' AND id_mode_transport = (SELECT id FROM mode_transport WHERE nom = 'Maritime')), NULL, FALSE, 'Certificat de fret'),
('Attestation de non-radioactivite', (SELECT id FROM type_production WHERE nom = 'import' AND id_mode_transport = (SELECT id FROM mode_transport WHERE nom = 'Maritime')), NULL, FALSE, 'Attestation de non-radioactivite'),
('Certificat de conformite', (SELECT id FROM type_production WHERE nom = 'import' AND id_mode_transport = (SELECT id FROM mode_transport WHERE nom = 'Maritime')), (SELECT id FROM nature_marchandise WHERE nom = 'Electronique'), TRUE, 'Certificat de conformite');

-- Les noms de types documentaires sont uniques dans le schéma. Conserver
-- leur production pour les listes documentaires et les indicateurs du tableau
-- de bord ; suffixer les noms ajoutés avec le flux et le mode de transport.
INSERT INTO type_document (nom, id_type_production, est_obligatoire, description)
SELECT t.nom || ' - ' || tp.description, tp.id, t.obligatoire, t.description
FROM type_production tp
CROSS JOIN (VALUES
    ('Declaration en douane', TRUE, 'Déclaration douanière du dossier'),
    ('Facture commerciale', TRUE, 'Facture commerciale du dossier'),
    ('Facture proforma', FALSE, 'Proforma envoyée au client')
) AS t(nom, obligatoire, description)
WHERE tp.nom IN ('import', 'export')
  AND NOT EXISTS (SELECT 1 FROM type_document td WHERE td.nom = t.nom AND td.id_type_production = tp.id);

INSERT INTO type_document (nom, id_type_production, est_obligatoire, description)
SELECT CASE mt.nom WHEN 'Maritime' THEN 'Connaissement maritime export' ELSE 'Lettre de transport aerien export' END,
       tp.id, TRUE, 'Document de transport reçu pour le flux export'
FROM type_production tp JOIN mode_transport mt ON mt.id = tp.id_mode_transport
WHERE tp.nom = 'export';

INSERT INTO document_obligatoire_mvt (id_operation, id_type_document, description)
SELECT op.id, td.id, 'Document requis pour ' || op.description || ' : ' || td.nom
FROM operation_dossier op
JOIN type_production tp ON tp.id = op.id_type_production
JOIN mode_transport mt ON mt.id = tp.id_mode_transport
JOIN type_document td ON td.id_type_production = op.id_type_production
WHERE (op.description IN ('Premier réception document', 'Réception Doc')
       AND ((mt.nom = 'Maritime' AND td.nom LIKE 'Connaissement maritime%')
         OR (mt.nom = 'Aerien' AND td.nom LIKE 'Lettre de transport aerien%')))
   OR (op.description IN ('Déclaration', 'Dépôt Déclaration') AND td.nom LIKE 'Declaration en douane%')
   OR (op.description = 'Envoi proforma' AND td.nom LIKE 'Facture proforma%')
   OR (op.description = 'Facturation' AND td.nom LIKE 'Facture commerciale%');

SELECT setval(pg_get_serial_sequence('type_document', 'id'), coalesce(max(id), 1)) FROM type_document;

INSERT INTO type_container (nom, description, volume, poids_max, longueur, largeur, hauteur) VALUES
('20 pieds', 'Conteneur standard 20 pieds', 33.2, 28000, 6.06, 2.44, 2.59),
('40 pieds', 'Conteneur standard 40 pieds', 67.7, 30000, 12.19, 2.44, 2.59),
('40 pieds High Cube', 'Conteneur 40 pieds high cube', 76.4, 30000, 12.19, 2.44, 2.90),
('45 pieds High Cube', 'Conteneur 45 pieds high cube', 86.0, 30000, 13.72, 2.44, 2.90),
('Open Top', 'Conteneur open top', 65.0, 28000, 12.19, 2.44, 2.59),
('Reefer', 'Conteneur refrigere', 60.0, 28000, 12.19, 2.44, 2.59);

-- ============================================================
-- COMPAGNIES (fusion maritime + aerienne)
-- ============================================================
INSERT INTO compagnie (nom, code, id_pays, id_mode_transport, description) VALUES
('Maersk', 'MAE', (SELECT id FROM pays WHERE code = 'DNK'), (SELECT id FROM mode_transport WHERE nom = 'Maritime'), NULL),
('MSC', 'MSC', (SELECT id FROM pays WHERE code = 'CHE'), (SELECT id FROM mode_transport WHERE nom = 'Maritime'), NULL),
('CMA CGM', 'CMA', (SELECT id FROM pays WHERE code = 'FRA'), (SELECT id FROM mode_transport WHERE nom = 'Maritime'), NULL),
('COSCO', 'COS', (SELECT id FROM pays WHERE code = 'CHN'), (SELECT id FROM mode_transport WHERE nom = 'Maritime'), NULL),
('Hapag-Lloyd', 'HAP', (SELECT id FROM pays WHERE code = 'DEU'), (SELECT id FROM mode_transport WHERE nom = 'Maritime'), NULL),
('Evergreen', 'EVE', (SELECT id FROM pays WHERE code = 'TWN'), (SELECT id FROM mode_transport WHERE nom = 'Maritime'), NULL),
('ONE', 'ONE', (SELECT id FROM pays WHERE code = 'JPN'), (SELECT id FROM mode_transport WHERE nom = 'Maritime'), NULL),
('HMM', 'HMM', (SELECT id FROM pays WHERE code = 'KOR'), (SELECT id FROM mode_transport WHERE nom = 'Maritime'), NULL),
('Yang Ming', 'YML', (SELECT id FROM pays WHERE code = 'TWN'), (SELECT id FROM mode_transport WHERE nom = 'Maritime'), NULL),
('PIL', 'PIL', (SELECT id FROM pays WHERE code = 'SGP'), (SELECT id FROM mode_transport WHERE nom = 'Maritime'), NULL),
('Air France', 'AFR', (SELECT id FROM pays WHERE code = 'FRA'), (SELECT id FROM mode_transport WHERE nom = 'Aerien'), NULL),
('Air Madagascar', 'MDG', (SELECT id FROM pays WHERE code = 'MDG'), (SELECT id FROM mode_transport WHERE nom = 'Aerien'), NULL),
('Kenya Airways', 'KQA', (SELECT id FROM pays WHERE code = 'KEN'), (SELECT id FROM mode_transport WHERE nom = 'Aerien'), NULL),
('Ethiopian Airlines', 'ETH', (SELECT id FROM pays WHERE code = 'ETH'), (SELECT id FROM mode_transport WHERE nom = 'Aerien'), NULL),
('Emirates', 'UAE', (SELECT id FROM pays WHERE code = 'ARE'), (SELECT id FROM mode_transport WHERE nom = 'Aerien'), NULL),
('Qatar Airways', 'QTR', (SELECT id FROM pays WHERE code = 'QAT'), (SELECT id FROM mode_transport WHERE nom = 'Aerien'), NULL),
('Turkish Airlines', 'THY', (SELECT id FROM pays WHERE code = 'TUR'), (SELECT id FROM mode_transport WHERE nom = 'Aerien'), NULL),
('Corsair', 'CRL', (SELECT id FROM pays WHERE code = 'FRA'), (SELECT id FROM mode_transport WHERE nom = 'Aerien'), NULL),
('Air Mauritius', 'MAU', (SELECT id FROM pays WHERE code = 'MUS'), (SELECT id FROM mode_transport WHERE nom = 'Aerien'), NULL),
('Air Austral', 'REU', (SELECT id FROM pays WHERE code = 'REU'), (SELECT id FROM mode_transport WHERE nom = 'Aerien'), NULL);

INSERT INTO navire (nom, imo, id_compagnie) VALUES
('CMA CGM Marco Polo', 'IMO9454436', (SELECT id FROM compagnie WHERE code = 'CMA')),
('MSC Oscar', 'IMO9703291', (SELECT id FROM compagnie WHERE code = 'MSC')),
('Maersk Triple E', 'IMO9632064', (SELECT id FROM compagnie WHERE code = 'MAE')),
('Ever Given', 'IMO9811000', (SELECT id FROM compagnie WHERE code = 'EVE')),
('HMM Algeciras', 'IMO9863296', (SELECT id FROM compagnie WHERE code = 'HMM')),
('COSCO Shipping Universe', 'IMO9795610', (SELECT id FROM compagnie WHERE code = 'COS')),
('ONE Trust', 'IMO9863578', (SELECT id FROM compagnie WHERE code = 'ONE')),
('Yang Ming Wellbeing', 'IMO9861611', (SELECT id FROM compagnie WHERE code = 'YML')),
('PIL Kota Pahlawan', 'IMO9628236', (SELECT id FROM compagnie WHERE code = 'PIL')),
('Hapag-Lloyd Al Nefud', 'IMO9708776', (SELECT id FROM compagnie WHERE code = 'HAP')),
('MSC Gulsun', 'IMO9839430', (SELECT id FROM compagnie WHERE code = 'MSC')),
('Maersk Edinburgh', 'IMO9458078', (SELECT id FROM compagnie WHERE code = 'MAE')),
('CMA CGM Antoine de Saint Exupery', 'IMO9776418', (SELECT id FROM compagnie WHERE code = 'CMA')),
('Ever Ace', 'IMO9893890', (SELECT id FROM compagnie WHERE code = 'EVE')),
('HMM Copenhagen', 'IMO9863301', (SELECT id FROM compagnie WHERE code = 'HMM')),
('COSCO Shipping Pisces', 'IMO9783481', (SELECT id FROM compagnie WHERE code = 'COS')),
('ONE Continuity', 'IMO9863592', (SELECT id FROM compagnie WHERE code = 'ONE')),
('Yang Ming Warranty', 'IMO9861623', (SELECT id FROM compagnie WHERE code = 'YML')),
('PIL Kota Cempaka', 'IMO9628248', (SELECT id FROM compagnie WHERE code = 'PIL')),
('Hapag-Lloyd Berlin Express', 'IMO9851185', (SELECT id FROM compagnie WHERE code = 'HAP'));

INSERT INTO station (nom, code, lieu, id_mode_transport, description) VALUES
('Port de Toamasina', 'PTOM', 'Toamasina', (SELECT id FROM mode_transport WHERE nom = 'Maritime'), 'Port principal de Madagascar'),
('Port d''Antsiranana', 'PDAN', 'Antsiranana', (SELECT id FROM mode_transport WHERE nom = 'Maritime'), 'Port du nord'),
('Port de Mahajanga', 'PMAH', 'Mahajanga', (SELECT id FROM mode_transport WHERE nom = 'Maritime'), 'Port de l''ouest'),
('Port de Toliara', 'PTOL', 'Toliara', (SELECT id FROM mode_transport WHERE nom = 'Maritime'), 'Port du sud'),
('Port de Marseille', 'PMRS', 'Marseille', (SELECT id FROM mode_transport WHERE nom = 'Maritime'), 'Port francais'),
('Port du Havre', 'PHAV', 'Le Havre', (SELECT id FROM mode_transport WHERE nom = 'Maritime'), 'Port francais'),
('Port d''Anvers', 'PANV', 'Anvers', (SELECT id FROM mode_transport WHERE nom = 'Maritime'), 'Port belge'),
('Port de Rotterdam', 'PRTM', 'Rotterdam', (SELECT id FROM mode_transport WHERE nom = 'Maritime'), 'Port neerlandais'),
('Port de Hambourg', 'PHAM', 'Hambourg', (SELECT id FROM mode_transport WHERE nom = 'Maritime'), 'Port allemand'),
('Port de Shanghai', 'PSHA', 'Shanghai', (SELECT id FROM mode_transport WHERE nom = 'Maritime'), 'Port chinois'),
('Port de Singapour', 'PSIN', 'Singapour', (SELECT id FROM mode_transport WHERE nom = 'Maritime'), 'Port de Singapour'),
('Port de Busan', 'PBUS', 'Busan', (SELECT id FROM mode_transport WHERE nom = 'Maritime'), 'Port coreen'),
('Port de Jebel Ali', 'PJEA', 'Dubai', (SELECT id FROM mode_transport WHERE nom = 'Maritime'), 'Port emirati'),
('Port de Durban', 'PDUR', 'Durban', (SELECT id FROM mode_transport WHERE nom = 'Maritime'), 'Port sud-africain'),
('Port de Mombasa', 'PMOM', 'Mombasa', (SELECT id FROM mode_transport WHERE nom = 'Maritime'), 'Port kenyan'),
('Aeroport Ivato', 'TNR', 'Antananarivo', (SELECT id FROM mode_transport WHERE nom = 'Aerien'), 'Aeroport international d''Antananarivo'),
('Aeroport de Nosy Be', 'NOS', 'Nosy Be', (SELECT id FROM mode_transport WHERE nom = 'Aerien'), 'Aeroport de Nosy Be'),
('Aeroport de Toamasina', 'TMM', 'Toamasina', (SELECT id FROM mode_transport WHERE nom = 'Aerien'), 'Aeroport de Toamasina'),
('Aeroport Roissy Charles de Gaulle', 'CDG', 'Paris', (SELECT id FROM mode_transport WHERE nom = 'Aerien'), 'Aeroport parisien'),
('Aeroport d''Orly', 'ORY', 'Paris', (SELECT id FROM mode_transport WHERE nom = 'Aerien'), 'Aeroport parisien'),
('Aeroport de Francfort', 'FRA', 'Francfort', (SELECT id FROM mode_transport WHERE nom = 'Aerien'), 'Aeroport allemand'),
('Aeroport d''Amsterdam Schiphol', 'AMS', 'Amsterdam', (SELECT id FROM mode_transport WHERE nom = 'Aerien'), 'Aeroport neerlandais'),
('Aeroport de Londres Heathrow', 'LHR', 'Londres', (SELECT id FROM mode_transport WHERE nom = 'Aerien'), 'Aeroport britannique'),
('Aeroport de Bruxelles', 'BRU', 'Bruxelles', (SELECT id FROM mode_transport WHERE nom = 'Aerien'), 'Aeroport belge'),
('Aeroport de Geneve', 'GVA', 'Geneve', (SELECT id FROM mode_transport WHERE nom = 'Aerien'), 'Aeroport suisse'),
('Aeroport de Dubai', 'DXB', 'Dubai', (SELECT id FROM mode_transport WHERE nom = 'Aerien'), 'Aeroport emirati'),
('Aeroport de Doha', 'DOH', 'Doha', (SELECT id FROM mode_transport WHERE nom = 'Aerien'), 'Aeroport qatari'),
('Aeroport d''Istanbul', 'IST', 'Istanbul', (SELECT id FROM mode_transport WHERE nom = 'Aerien'), 'Aeroport turc'),
('Aeroport d''Addis-Abeba', 'ADD', 'Addis-Abeba', (SELECT id FROM mode_transport WHERE nom = 'Aerien'), 'Aeroport ethiopien'),
('Aeroport de Nairobi', 'NBO', 'Nairobi', (SELECT id FROM mode_transport WHERE nom = 'Aerien'), 'Aeroport kenyan');

INSERT INTO unite_mesure (nom, rapport_m_cube, description) VALUES
('Unite', 0.00, 'Unite'),
('Kilogramme', 0.001, 'Kilogramme'),
('Tonne', 1.00, 'Tonne metrique'),
('Metre cube', 1.00, 'Metre cube'),
('Litre', 0.001, 'Litre');

-- Insertion des actions de base
INSERT INTO action (nom, description) VALUES
    ('visualisation', 'Visualisation d''un dossier ou d''une ressource'),
    ('creation', 'Création d''un dossier ou d''une ressource'),
    ('modification', 'Modification d''un dossier ou d''une ressource'),
    ('suppression', 'Suppression d''un dossier ou d''une ressource'),
    ('validation', 'Validation d''un dossier ou d''une ressource')
ON CONFLICT (nom) DO NOTHING;

-- ============================================================
-- 2. GENERATION DE DONNEES DE DEMONSTRATION
-- ============================================================
-- À charger sur une base vide après le schéma. Ce script ne supprime aucune
-- donnée existante et n'est pas destiné à être rejoué sur une base déjà remplie.
-- Dates relatives à l'exécution : les douze derniers mois calendaires, avec
-- des volumes mensuels différents et plusieurs jours dans la période récente.
-- Toutes les dates réelles restent dans le passé ; seules les dates prévues,
-- les échéances et les limites de remise peuvent se situer dans le futur.

DO $$
DECLARE
    v_debut TIMESTAMPTZ := date_trunc('month', CURRENT_TIMESTAMP) - INTERVAL '11 months';
    v_volumes INT[] := ARRAY[10, 13, 17, 12, 22, 18, 28, 24, 34, 30, 40, 52];
    v_agences INT[] := ARRAY(SELECT id FROM agence ORDER BY id);
    v_roles INT[] := ARRAY(SELECT id FROM role ORDER BY id);
    v_productions INT[] := ARRAY(SELECT id FROM type_production ORDER BY id);
    v_clients INT[];
    v_utilisateurs INT[];
    v_ots INT[] := '{}';
    v_dossiers INT[] := '{}';
    v_operations INT[];
    v_pays INT[] := ARRAY(SELECT id FROM pays ORDER BY id);
    v_types_clients INT[] := ARRAY(SELECT id FROM type_client ORDER BY id);
    v_regimes INT[] := ARRAY(SELECT id FROM type_regime ORDER BY id);
    v_bureaux INT[] := ARRAY(SELECT id FROM bureau ORDER BY id);
    v_unites INT[] := ARRAY(SELECT id FROM unite_mesure ORDER BY id);
    v_natures INT[] := ARRAY(SELECT id FROM nature_marchandise ORDER BY id);
    v_containers INT[] := ARRAY(SELECT id FROM type_container ORDER BY id);
    v_acheminements INT[] := ARRAY(SELECT id FROM mode_acheminement ORDER BY id);
    v_fob INT := (SELECT id FROM inconterme WHERE abreviation = 'FOB');
    v_preparation INT := (SELECT id FROM phase_dossier WHERE nom = 'PREPARATION');
    v_ouvert INT := (SELECT id FROM statut_dossier WHERE nom = 'Ouvert');
    v_user INT;
    v_ot INT;
    v_dossier INT;
    v_type INT;
    v_agence INT;
    v_role INT;
    v_statut_ot INT;
    v_statut INT;
    v_phase INT;
    v_phase_suivante INT;
    v_doc INT;
    v_expedition INT;
    v_compagnie INT;
    v_navire INT;
    v_station_depart INT;
    v_station_arrivee INT;
    v_acheminement INT;
    v_nb_operations INT;
    v_nb_effectuees INT;
    v_creation TIMESTAMPTZ;
    v_mois TIMESTAMPTZ;
    v_fin_mois TIMESTAMPTZ;
    v_fin_flux TIMESTAMPTZ;
    v_date_mvt TIMESTAMPTZ;
    v_date_doc TIMESTAMPTZ;
    v_date_depart TIMESTAMPTZ;
    v_date_prevue TIMESTAMPTZ;
    v_date_arrivee TIMESTAMPTZ;
    v_numero TEXT;
    v_compteur INT := 0;
    i INT;
    j INT;
    mois INT;
    ligne RECORD;
    op RECORD;
    type_doc RECORD;
    parent RECORD;
    enfant RECORD;
BEGIN
    -- Tirage reproductible ; les dates suivent la date d'exécution du script.
    PERFORM setseed(0.2718);
    UPDATE agence SET date_creation = v_debut - INTERVAL '4 months' + id * INTERVAL '1 day';

    -- 100 clients et 30 comptes de démonstration, créés avant leur activité.
    FOR i IN 1..100 LOOP
        INSERT INTO clients (nom, prenom, email, telephone, adresse, id_type_client,
                             nif, stat, fax, id_pays, cp_client, date_creation)
        VALUES ('ClientNom' || i, 'Prenom' || i, 'client' || i || '@mail.com',
                '032' || lpad(i::text, 7, '0'), 'Adresse ' || i,
                v_types_clients[1 + (i - 1) % array_length(v_types_clients, 1)],
                'NIF' || lpad(i::text, 10, '0'), 'STAT' || lpad(i::text, 10, '0'), 'Fax' || i,
                v_pays[1 + (i - 1) % array_length(v_pays, 1)], 'CLI' || lpad(i::text, 3, '0'),
                v_debut - INTERVAL '3 months' + random() * INTERVAL '20 days');
    END LOOP;

    FOR i IN 1..30 LOOP
        v_agence := v_agences[1 + (i - 1) % array_length(v_agences, 1)];
        v_role := v_roles[1 + (i - 1) % array_length(v_roles, 1)];
        v_creation := v_debut - INTERVAL '2 months' + i * INTERVAL '1 day';
        INSERT INTO utilisateur (nom_utilisateur, nom, prenom, email, mot_de_passe,
                                 id_role, id_agence, date_creation, date_modification)
        VALUES ('user' || i, 'NomUser' || i, 'PrenomUser' || i, 'user' || i || '@transit.mg',
                '$2a$12$KovwAzSOJPTaWqYJ2UgE/.sztG.CGSEgGa/QfoV.qlNK8qBtXzMna',
                v_role, v_agence, v_creation, v_creation) RETURNING id INTO v_user;
        -- Même accès administrateur que le jeu de démonstration précédent,
        -- limité aux comptes insérés ici, sans modifier d'autres utilisateurs.
        INSERT INTO autorisation (id_utilisateur, niveau_autorisation) VALUES (v_user, 10000);
    END LOOP;
    v_clients := ARRAY(SELECT id FROM clients ORDER BY id);
    v_utilisateurs := ARRAY(SELECT id FROM utilisateur ORDER BY id);

    -- 320 OT : suffisamment pour 300 dossiers et au moins 20 OT autonomes.
    FOR i IN 1..320 LOOP
        v_agence := v_agences[1 + (i - 1) % array_length(v_agences, 1)];
        v_type := v_productions[1 + (i - 1) % array_length(v_productions, 1)];
        SELECT id INTO v_user FROM utilisateur WHERE id_agence = v_agence ORDER BY id LIMIT 1;
        SELECT id INTO v_statut_ot FROM statut_ot
        WHERE nom = CASE WHEN i <= 300 THEN 'Valide'
                         WHEN i % 2 = 0 THEN 'Brouillon' ELSE 'Non valide' END;
        INSERT INTO ordre_transit (numero_ordre_transit, id_agence, id_client, id_type_production,
            id_nature_marchandise, numero_commande, id_utilisateur, id_type_regime, destination_finale,
            id_expediteur, id_destinataire, id_bureau_dedou, est_assure, id_destination_facture,
            id_statut_ot, date_creation)
        VALUES (10000 + i, v_agence, v_clients[1 + (i - 1) % 100], v_type,
                v_natures[1 + (i - 1) % array_length(v_natures, 1)],
                'CMD-' || lpad(i::text, 6, '0'), v_user,
                v_regimes[1 + (i - 1) % array_length(v_regimes, 1)], 'Destination finale ' || i,
                v_clients[1 + i % 100], v_clients[1 + (i + 1) % 100],
                v_bureaux[1 + (i - 1) % array_length(v_bureaux, 1)], random() < 0.3,
                v_clients[1 + (i - 1) % 100], v_statut_ot,
                v_debut + random() * (CURRENT_TIMESTAMP - INTERVAL '3 days' - v_debut))
        RETURNING id INTO v_ot;
        v_ots := array_append(v_ots, v_ot);
    END LOOP;

    FOR i IN 1..400 LOOP
        INSERT INTO instruction (id_ordre_transit, instruction)
        VALUES (v_ots[1 + (i - 1) % 320], 'Instruction ' || i || ' : suivi du dossier.');
    END LOOP;

    -- 300 créations mensuelles : 10,13,17,12,22,18,28,24,34,30,40,52.
    FOR mois IN 1..12 LOOP
        v_mois := v_debut + (mois - 1) * INTERVAL '1 month';
        v_fin_mois := LEAST(v_mois + INTERVAL '1 month', CURRENT_TIMESTAMP);
        FOR j IN 1..v_volumes[mois] LOOP
            v_compteur := v_compteur + 1;
            -- Répartition stratifiée : plusieurs jours par mois, sans création future.
            v_creation := v_mois + (v_fin_mois - v_mois) * ((j - 1 + random()) / v_volumes[mois]);
            SELECT ot.*, tp.code AS code_production, a.code AS code_agence INTO ligne
            FROM ordre_transit ot JOIN type_production tp ON tp.id = ot.id_type_production
            JOIN agence a ON a.id = ot.id_agence WHERE ot.id = v_ots[v_compteur];
            v_numero := ligne.code_production::text || lpad(ligne.code_agence::text, 2, '0')
                     || to_char(v_creation, 'MM') || (EXTRACT(YEAR FROM v_creation)::int % 10)::text
                     || lpad(v_compteur::text, 4, '0');
            INSERT INTO dossier (id_agence, id_utilisateur, id_client, id_type_production,
                numero_dossier, id_ordre_transit, est_groupage, id_phase, id_statut, echeance,
                pour_compte, id_pays_origine, id_pays_destination, est_prepaye, est_provisionne,
                provision_demandee, lieu_paiement_fret, depart, adresse_livraison, id_inconterme,
                date_limite_remise, date_creation)
            VALUES (ligne.id_agence, ligne.id_utilisateur, ligne.id_client, ligne.id_type_production,
                v_numero, ligne.id, FALSE, v_preparation, v_ouvert,
                v_creation + (20 + v_compteur % 35) * INTERVAL '1 day',
                'Pour compte de ' || v_compteur,
                v_pays[1 + (v_compteur - 1) % array_length(v_pays, 1)],
                v_pays[1 + v_compteur % array_length(v_pays, 1)], random() < 0.5, FALSE,
                (50000 + random() * 950000)::numeric(10, 2), 'Antananarivo',
                'Port/Aeroport ' || v_compteur, 'Adresse livraison ' || v_compteur, v_fob,
                v_creation + (5 + v_compteur % 8) * INTERVAL '1 day', v_creation)
            RETURNING id INTO v_dossier;
            v_dossiers := array_append(v_dossiers, v_dossier);
        END LOOP;
    END LOOP;

    -- 40 sous-dossiers plus récents : même OT, même client et même production
    -- que leur dossier mère, avec une agence différente, sans boucle ni doublon.
    FOR i IN 1..40 LOOP
        SELECT * INTO parent FROM dossier WHERE id = v_dossiers[i];
        SELECT d.* INTO enfant FROM dossier d
        WHERE d.id = ANY(v_dossiers[181:300]) AND d.id_agence <> parent.id_agence
          AND d.id_type_production = parent.id_type_production
          AND NOT EXISTS (SELECT 1 FROM dossier_interagence di WHERE di.id_sous_dossier = d.id)
        ORDER BY d.id LIMIT 1;
        IF enfant.id IS NULL THEN RAISE EXCEPTION 'Sous-dossier compatible introuvable'; END IF;
        UPDATE dossier SET id_ordre_transit = parent.id_ordre_transit, id_client = parent.id_client
        WHERE id = enfant.id;
        INSERT INTO dossier_interagence (id_dossier_mere, id_sous_dossier, motif, date_creation)
        VALUES (parent.id, enfant.id, 'Prise en charge par une autre agence', enfant.date_creation);
    END LOOP;

    -- L'OT précède toujours le premier dossier qui lui est rattaché.
    UPDATE ordre_transit ot SET date_creation = LEAST(ot.date_creation,
        (SELECT min(d.date_creation) - INTERVAL '1 day' FROM dossier d WHERE d.id_ordre_transit = ot.id))
    WHERE EXISTS (SELECT 1 FROM dossier d WHERE d.id_ordre_transit = ot.id);

    -- Parcours réels : préfixe ordonné du workflow du type de production.
    -- Les types divers/colis, sans workflow défini, restent ouverts en préparation.
    FOR ligne IN SELECT * FROM dossier ORDER BY id LOOP
        v_operations := ARRAY(SELECT id FROM operation_dossier
            WHERE id_type_production = ligne.id_type_production ORDER BY niveau_operation, id);
        v_nb_operations := COALESCE(array_length(v_operations, 1), 0);
        IF v_nb_operations = 0 THEN CONTINUE; END IF;
        IF ligne.date_creation > CURRENT_TIMESTAMP - INTERVAL '3 days' THEN
            v_nb_effectuees := ligne.id % 5;
        ELSIF ligne.id % 17 = 0 THEN
            v_nb_effectuees := 0;
        ELSIF ligne.id % 5 IN (0, 1) THEN
            v_nb_effectuees := v_nb_operations;
        ELSE
            v_nb_effectuees := 1 + (ligne.id * 7) % v_nb_operations;
        END IF;
        v_phase := v_preparation;
        v_statut := v_ouvert;
        v_fin_flux := LEAST(ligne.date_creation + INTERVAL '45 days', CURRENT_TIMESTAMP);
        v_date_mvt := ligne.date_creation;
        FOR j IN 1..v_nb_effectuees LOOP
            SELECT * INTO op FROM operation_dossier WHERE id = v_operations[j];
            -- Après la dernière opération obligatoire d'une phase, avancer
            -- à la phase de l'opération suivante, comme le service Java.
            SELECT id_phase INTO v_phase_suivante FROM operation_dossier
            WHERE id_type_production = ligne.id_type_production AND niveau_operation > op.niveau_operation
            ORDER BY niveau_operation, id LIMIT 1;
            v_phase_suivante := COALESCE(v_phase_suivante, op.id_phase);
            v_date_mvt := ligne.date_creation + (v_fin_flux - ligne.date_creation) * (j::numeric / (v_nb_effectuees + 1));
            INSERT INTO mvt_dossier (id_dossier, id_utilisateur, id_operation,
                id_ancienne_phase, id_nouvelle_phase, id_ancien_statut, id_nouveau_statut,
                commentaire, date_mvt, origine)
            VALUES (ligne.id, ligne.id_utilisateur, op.id, v_phase, v_phase_suivante,
                v_statut, op.id_statut, 'Opération effectuée : ' || op.description, v_date_mvt, 'SAISIE');
            v_phase := v_phase_suivante;
            v_statut := op.id_statut;
        END LOOP;
        UPDATE dossier SET id_phase = v_phase, id_statut = v_statut,
            est_provisionne = EXISTS (SELECT 1 FROM mvt_dossier m JOIN operation_dossier o ON o.id = m.id_operation
                WHERE m.id_dossier = ligne.id AND o.description IN ('Provision', 'Provision (pour fret et débours)')),
            date_livraison = CASE WHEN v_phase = (SELECT id FROM phase_dossier WHERE nom = 'COMPTABILITE')
                THEN LEAST(ligne.date_creation + INTERVAL '25 days', v_date_mvt) ELSE NULL END
        WHERE id = ligne.id;
    END LOOP;

    -- Documents actifs du même flux et de la même nature que le dossier.
    -- Les documents requis par une opération réalisée sont toujours présents
    -- et remis AVANT cette opération. Quelques dossiers non clôturés restent
    -- volontairement incomplets pour les indicateurs de pièces manquantes.
    FOR ligne IN SELECT d.*, ot.id_nature_marchandise FROM dossier d
                 JOIN ordre_transit ot ON ot.id = d.id_ordre_transit ORDER BY d.id LOOP
        INSERT INTO document (id_dossier, id_type_document, nombre, chemin_fichier, nom_fichier, date_ajout)
        VALUES (ligne.id, (SELECT id FROM type_document WHERE nom = 'Document d''attachement'),
                1, '/documents/dossier_' || ligne.id || '.pdf', 'dossier_' || ligne.id || '.pdf', ligne.date_creation);
        FOR type_doc IN
            SELECT td.*, (SELECT min(m.date_mvt) FROM mvt_dossier m
                JOIN document_obligatoire_mvt dom ON dom.id_operation = m.id_operation
                WHERE m.id_dossier = ligne.id AND dom.id_type_document = td.id) AS premiere_utilisation
            FROM type_document td
            WHERE (td.id_type_production = ligne.id_type_production OR td.id_type_production IS NULL)
              AND td.nom <> 'Document d''attachement'
              AND (td.id_nature_marchandise IS NULL OR td.id_nature_marchandise = ligne.id_nature_marchandise)
        LOOP
            IF type_doc.premiere_utilisation IS NULL AND ligne.id % 5 = 0 THEN CONTINUE; END IF;
            IF NOT type_doc.est_obligatoire AND type_doc.premiere_utilisation IS NULL AND ligne.id % 3 <> 0 THEN CONTINUE; END IF;
            v_date_doc := ligne.date_creation +
                (COALESCE(type_doc.premiere_utilisation, LEAST(ligne.date_creation + INTERVAL '2 days', CURRENT_TIMESTAMP))
                    - ligne.date_creation) * 0.1;
            INSERT INTO document (id_dossier, id_type_document, nombre, chemin_fichier, nom_fichier, date_ajout)
            VALUES (ligne.id, type_doc.id, 1, '/documents/doc_' || ligne.id || '_' || type_doc.id || '.pdf',
                    type_doc.nom || '.pdf', v_date_doc);
            INSERT INTO remise (id_dossier, id_type_document, date_remise)
            VALUES (ligne.id, type_doc.id,
                LEAST(v_date_doc + INTERVAL '1 second', COALESCE(type_doc.premiere_utilisation, CURRENT_TIMESTAMP)));
        END LOOP;
    END LOOP;
    INSERT INTO document_mvt (id_mvt_dossier, id_document)
    SELECT m.id, (SELECT doc.id FROM document doc WHERE doc.id_dossier = m.id_dossier ORDER BY doc.id LIMIT 1)
    FROM mvt_dossier m WHERE m.id % 3 = 0;

    -- Expéditions maritimes/aériennes : dates déduites de l'OT et stations distinctes.
    FOR ligne IN SELECT ot.*, tp.id_mode_transport, mt.nom AS transport
        FROM ordre_transit ot JOIN type_production tp ON tp.id = ot.id_type_production
        JOIN mode_transport mt ON mt.id = tp.id_mode_transport
        WHERE mt.nom IN ('Maritime', 'Aerien') ORDER BY ot.id LOOP
        SELECT id INTO v_dossier FROM dossier WHERE id_ordre_transit = ligne.id ORDER BY id LIMIT 1;
        SELECT id INTO v_compagnie FROM compagnie WHERE id_mode_transport = ligne.id_mode_transport ORDER BY random() LIMIT 1;
        SELECT id INTO v_navire FROM navire WHERE id_compagnie = v_compagnie ORDER BY random() LIMIT 1;
        SELECT id INTO v_station_depart FROM station WHERE id_mode_transport = ligne.id_mode_transport ORDER BY random() LIMIT 1;
        SELECT id INTO v_station_arrivee FROM station
            WHERE id_mode_transport = ligne.id_mode_transport AND id <> v_station_depart ORDER BY random() LIMIT 1;
        v_acheminement := v_acheminements[1 + ligne.id % array_length(v_acheminements, 1)];
        v_date_depart := ligne.date_creation + INTERVAL '1 day';
        v_date_prevue := v_date_depart + CASE WHEN ligne.transport = 'Maritime' THEN INTERVAL '18 days' ELSE INTERVAL '2 days' END;
        v_date_arrivee := v_date_prevue + (ligne.id % 4) * INTERVAL '1 day';
        IF v_date_arrivee > CURRENT_TIMESTAMP THEN v_date_arrivee := NULL; END IF;
        IF v_date_depart > CURRENT_TIMESTAMP THEN v_date_depart := NULL; END IF;
        INSERT INTO expedition (id_ordre_transit, id_dossier, est_achemine, id_mode_transport,
            id_mode_acheminement, id_compagnie, id_navire, numero_bl, date_bl, lieu_bl,
            id_port_embarquement, id_port_debarquement, numero_vol, numero_lta, date_lta, lieu_lta,
            id_aeroport_embarquement, id_aeroport_debarquement, lib_scan, date_depart, date_prevu, date_arrivee)
        VALUES (ligne.id, v_dossier, v_date_arrivee IS NOT NULL, ligne.id_mode_transport,
            v_acheminement, v_compagnie, CASE WHEN ligne.transport = 'Maritime' THEN v_navire END,
            CASE WHEN ligne.transport = 'Maritime' THEN 'BL-' || lpad(ligne.id::text, 6, '0') END,
            CASE WHEN ligne.transport = 'Maritime' THEN v_date_depart END,
            CASE WHEN ligne.transport = 'Maritime' THEN 'Port de Toamasina' END,
            CASE WHEN ligne.transport = 'Maritime' THEN v_station_depart END,
            CASE WHEN ligne.transport = 'Maritime' THEN v_station_arrivee END,
            CASE WHEN ligne.transport = 'Aerien' THEN 'VOL-' || lpad(ligne.id::text, 6, '0') END,
            CASE WHEN ligne.transport = 'Aerien' THEN 'LTA-' || lpad(ligne.id::text, 6, '0') END,
            CASE WHEN ligne.transport = 'Aerien' THEN v_date_depart END,
            CASE WHEN ligne.transport = 'Aerien' THEN 'Aeroport Ivato' END,
            CASE WHEN ligne.transport = 'Aerien' THEN v_station_depart END,
            CASE WHEN ligne.transport = 'Aerien' THEN v_station_arrivee END,
            'Scan OT ' || ligne.id, v_date_depart, v_date_prevue, v_date_arrivee);
    END LOOP;

    FOR i IN 1..50 LOOP
        SELECT * INTO ligne FROM dossier WHERE id = v_dossiers[1 + (i * 7) % 300];
        INSERT INTO constat (id_dossier, id_expert_constat, id_type_constat, description, date_creation)
        VALUES (ligne.id, (SELECT id FROM expert_constat ORDER BY id OFFSET (i % 10) LIMIT 1),
            (SELECT id FROM type_constat ORDER BY id OFFSET (i % 3) LIMIT 1), 'Constat ' || i,
            LEAST(ligne.date_creation + INTERVAL '1 hour', CURRENT_TIMESTAMP));
    END LOOP;

    FOR i IN 1..600 LOOP
        INSERT INTO marchandise (id_ordre_transit, id_unite_mesure, marque, modele, description, poids, volume, quantite)
        VALUES (v_ots[1 + (i - 1) % 320], v_unites[1 + (i - 1) % array_length(v_unites, 1)],
            'Marque' || i % 20, 'Modele' || i % 30, 'Description marchandise ' || i,
            (random() * 5000)::numeric(10, 2), (random() * 100)::numeric(10, 2), 1 + (i % 100));
    END LOOP;

    FOR i IN 1..300 LOOP
        v_dossier := v_dossiers[i];
        INSERT INTO conteneur (id_dossier, id_type_container, numero_container, description)
        VALUES (v_dossier, v_containers[1 + (i - 1) % array_length(v_containers, 1)],
            'CONT-' || lpad(i::text, 7, '0'), 'Conteneur ' || i) RETURNING id INTO v_doc;
        INSERT INTO dossier_conteneur (id_dossier, id_conteneur) VALUES (v_dossier, v_doc);
    END LOOP;

    FOR ligne IN SELECT d.id, d.date_creation, e.id AS expedition FROM dossier d
        JOIN expedition e ON e.id_ordre_transit = d.id_ordre_transit ORDER BY d.id LIMIT 100 LOOP
        INSERT INTO groupage (id_dossier, id_expedition, numero_groupage, partie_notifier, date_creation)
        VALUES (ligne.id, ligne.expedition, 'GRP-' || lpad(ligne.id::text, 7, '0'),
            'Partie notifier ' || ligne.id, LEAST(ligne.date_creation + INTERVAL '1 hour', CURRENT_TIMESTAMP));
        UPDATE dossier SET est_groupage = TRUE WHERE id = ligne.id;
    END LOOP;

    -- Une déclaration par dossier ayant réellement effectué cette opération.
    FOR ligne IN SELECT d.id, d.id_type_production, d.id_ordre_transit, m.date_mvt, tp.nom AS sens,
                        ot.id_bureau_dedou
        FROM dossier d JOIN mvt_dossier m ON m.id_dossier = d.id
        JOIN operation_dossier o ON o.id = m.id_operation AND o.description = 'Déclaration'
        JOIN type_production tp ON tp.id = d.id_type_production
        JOIN ordre_transit ot ON ot.id = d.id_ordre_transit ORDER BY d.id LOOP
        SELECT doc.id INTO v_doc FROM document doc JOIN type_document td ON td.id = doc.id_type_document
        WHERE doc.id_dossier = ligne.id AND td.nom LIKE 'Declaration en douane%' ORDER BY doc.id LIMIT 1;
        INSERT INTO declaration (id_dossier, numero_declaration, date_declaration, id_bureau,
            id_type_declaration, id_document, description, date_ajout)
        VALUES (ligne.id, 'DEC-' || lpad(ligne.id::text, 8, '0'), ligne.date_mvt, ligne.id_bureau_dedou,
            (SELECT id FROM type_declaration WHERE nom = CASE WHEN ligne.sens = 'import'
                THEN 'Mise à la consommation (IM4)' ELSE 'Exportation définitive (EX1)' END),
            v_doc, 'Déclaration du dossier ' || ligne.id, ligne.date_mvt);
    END LOOP;

    FOR i IN 1..5 LOOP
        INSERT INTO suspension_connexion (id_utilisateur, tentatives_echouees, date_suspension,
            duree_suspension_minutes, date_creation)
        VALUES (v_utilisateurs[i], i, CURRENT_TIMESTAMP - i * INTERVAL '1 day',
                15, CURRENT_TIMESTAMP - i * INTERVAL '1 day');
    END LOOP;
END $$;

COMMIT;

-- À lancer après le schéma et donne_deepseek.sql dans un schéma de test isolé.
-- Ce fichier ne modifie aucune donnée et s'arrête à la première incohérence.
\set ON_ERROR_STOP on

DO $verification$
DECLARE
    v_table TEXT;
    v_preparation INTEGER;
    v_ouvert INTEGER;
BEGIN
    IF current_schema() IN ('public', 'pg_catalog', 'information_schema') THEN
        RAISE EXCEPTION 'La vérification doit utiliser un schéma de test isolé (search_path actuel : %).', current_schema();
    END IF;

    FOREACH v_table IN ARRAY ARRAY[
        'agence', 'inconterme', 'phase_dossier', 'statut_dossier',
        'type_production', 'mode_transport', 'operation_dossier', 'ordre_transit',
        'dossier', 'mvt_dossier', 'document', 'type_document', 'document_mvt',
        'document_obligatoire_mvt', 'remise', 'utilisateur', 'clients',
        'declaration', 'dossier_interagence'
    ] LOOP
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.tables
            WHERE table_schema = current_schema() AND table_name = v_table
        ) THEN
            RAISE EXCEPTION 'Table % absente du schéma de test %.', v_table, current_schema();
        END IF;
    END LOOP;

    IF (SELECT count(*) FROM agence) <> 9
       OR EXISTS (SELECT 1 FROM agence WHERE lower(nom) LIKE '%manakara%' OR lower(ville) LIKE '%manakara%') THEN
        RAISE EXCEPTION 'Le jeu doit contenir neuf agences et aucune agence de Manakara.';
    END IF;

    IF (SELECT count(*) FROM inconterme) <> 1
       OR NOT EXISTS (SELECT 1 FROM inconterme WHERE abreviation = 'FOB') THEN
        RAISE EXCEPTION 'FOB doit être le seul incoterm du jeu.';
    END IF;

    IF (SELECT count(*) FROM phase_dossier) <> 3
       OR (SELECT count(*) FROM phase_dossier WHERE nom IN ('PREPARATION', 'TRAITEMENT', 'COMPTABILITE')) <> 3 THEN
        RAISE EXCEPTION 'Les seules phases attendues sont PREPARATION, TRAITEMENT et COMPTABILITE.';
    END IF;

    SELECT id INTO STRICT v_preparation FROM phase_dossier WHERE nom = 'PREPARATION';
    SELECT id INTO STRICT v_ouvert FROM statut_dossier WHERE nom = 'Ouvert';

    IF (SELECT count(*) FROM operation_dossier) <> 64 THEN
        RAISE EXCEPTION '64 opérations attendues : import 15 x 2 modes et export 17 x 2 modes.';
    END IF;

    IF (SELECT count(*) FROM type_production tp JOIN mode_transport mt ON mt.id = tp.id_mode_transport
        WHERE tp.nom IN ('import', 'export') AND mt.nom IN ('Maritime', 'Aerien')) <> 4 THEN
        RAISE EXCEPTION 'Les quatre productions import/export maritime/aérien sont nécessaires.';
    END IF;

    IF EXISTS (
        SELECT tp.id FROM type_production tp
        JOIN mode_transport mt ON mt.id = tp.id_mode_transport
        LEFT JOIN operation_dossier op ON op.id_type_production = tp.id
        WHERE tp.nom IN ('import', 'export') AND mt.nom IN ('Maritime', 'Aerien')
        GROUP BY tp.id, tp.nom
        HAVING count(op.id) <> CASE WHEN tp.nom = 'import' THEN 15 ELSE 17 END
           OR count(DISTINCT op.niveau_operation) <> count(op.id)
           OR min(op.niveau_operation) <> 1
           OR max(op.niveau_operation) <> count(op.id)
    ) THEN
        RAISE EXCEPTION 'Les opérations doivent former les rangs uniques 1..15 (import) ou 1..17 (export), par production.';
    END IF;

    IF EXISTS (
        SELECT 1 FROM operation_dossier
        WHERE id_phase IS NULL OR id_type_production IS NULL OR id_statut IS NULL
           OR id_statut = v_ouvert OR est_actif IS DISTINCT FROM TRUE
    ) THEN
        RAISE EXCEPTION 'Chaque opération active doit avoir une phase, une production et un statut précis ; Ouvert est réservé à la création.';
    END IF;

    -- Les listes de documents et les KPI applicatifs filtrent par production :
    -- un type documentaire générique (production NULL) serait invisible ici.
    IF EXISTS (
        SELECT 1 FROM document_obligatoire_mvt obligation
        JOIN operation_dossier op ON op.id = obligation.id_operation
        JOIN type_document td ON td.id = obligation.id_type_document
        WHERE td.id_type_production IS DISTINCT FROM op.id_type_production
    ) THEN
        RAISE EXCEPTION 'Les documents obligatoires d''une opération doivent avoir la même production que cette opération pour apparaître dans les listes et indicateurs.';
    END IF;

    IF EXISTS (
        SELECT 1 FROM type_production tp
        JOIN mode_transport mt ON mt.id = tp.id_mode_transport
        CROSS JOIN (VALUES ('Facture commerciale'), ('Declaration en douane'), ('Facture proforma')) AS famille(nom)
        WHERE tp.nom IN ('import', 'export') AND mt.nom IN ('Maritime', 'Aerien')
          AND NOT EXISTS (
              SELECT 1 FROM type_document td
              WHERE td.id_type_production = tp.id AND td.nom LIKE famille.nom || '%'
          )
    ) THEN
        RAISE EXCEPTION 'Chaque production import/export maritime/aérien doit proposer sa facture commerciale, sa déclaration en douane et sa facture proforma.';
    END IF;

    IF (SELECT count(*) FROM dossier) <> 300 OR (SELECT count(*) FROM ordre_transit) <> 320 THEN
        RAISE EXCEPTION 'Le jeu doit contenir 300 dossiers et 320 ordres de transit.';
    END IF;

    IF (SELECT count(*) FROM ordre_transit ot WHERE NOT EXISTS (
        SELECT 1 FROM dossier d WHERE d.id_ordre_transit = ot.id
    )) < 20 THEN
        RAISE EXCEPTION 'Au moins vingt ordres de transit doivent rester sans dossier.';
    END IF;

    IF (SELECT count(DISTINCT date_trunc('month', date_creation)) FROM dossier) <> 12
       OR (SELECT count(DISTINCT date_creation::date) FROM dossier) < 30
       OR (SELECT count(DISTINCT date_creation::date) FROM dossier
           WHERE date_creation >= CURRENT_DATE - INTERVAL '29 days') < 2 THEN
        RAISE EXCEPTION 'Les créations doivent couvrir douze mois, trente jours distincts et plusieurs jours sur les trente derniers jours.';
    END IF;

    IF EXISTS (
        SELECT 1 FROM dossier d
        LEFT JOIN ordre_transit ot ON ot.id = d.id_ordre_transit
        WHERE d.date_creation IS NULL OR d.date_creation > NOW()
           OR ot.id IS NULL OR ot.date_creation IS NULL OR ot.date_creation > d.date_creation
           OR d.id_type_production IS DISTINCT FROM ot.id_type_production
           OR d.id_client IS DISTINCT FROM ot.id_client
    ) THEN
        RAISE EXCEPTION 'Chaque dossier doit conserver le client et la production de son OT, créé au plus tard à la création du dossier.';
    END IF;

    IF (SELECT count(*) FROM dossier_interagence) <> 40
       OR (SELECT count(DISTINCT id_sous_dossier) FROM dossier_interagence) <> 40
       OR EXISTS (
           SELECT 1 FROM dossier_interagence di
           JOIN dossier mere ON mere.id = di.id_dossier_mere
           JOIN dossier sous ON sous.id = di.id_sous_dossier
           WHERE mere.id = sous.id
              OR mere.id_agence IS NOT DISTINCT FROM sous.id_agence
              OR mere.id_ordre_transit IS DISTINCT FROM sous.id_ordre_transit
              OR mere.id_client IS DISTINCT FROM sous.id_client
              OR mere.id_type_production IS DISTINCT FROM sous.id_type_production
              OR sous.date_creation < mere.date_creation
       ) THEN
        RAISE EXCEPTION 'Les quarante sous-dossiers doivent partager OT, client et production de leur dossier mère, dans une autre agence, sans antériorité.';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM mvt_dossier) OR EXISTS (
        SELECT 1 FROM mvt_dossier m
        JOIN dossier d ON d.id = m.id_dossier
        JOIN operation_dossier op ON op.id = m.id_operation
        WHERE op.id_type_production IS DISTINCT FROM d.id_type_production
           OR m.date_mvt IS NULL OR m.date_mvt < d.date_creation OR m.date_mvt > NOW()
           OR m.id_nouveau_statut IS DISTINCT FROM op.id_statut
           OR m.id_ancienne_phase IS NULL OR m.id_nouvelle_phase IS NULL
           OR m.id_ancien_statut IS NULL OR m.id_nouveau_statut IS NULL
    ) THEN
        RAISE EXCEPTION 'Les mouvements doivent suivre le type du dossier, le statut de leur opération et une chronologie dossier <= mouvement <= maintenant.';
    END IF;

    IF EXISTS (
        SELECT 1 FROM (
            SELECT m.*,
                lag(m.id_nouvelle_phase, 1, v_preparation) OVER w AS phase_attendue,
                lag(m.id_nouveau_statut, 1, v_ouvert) OVER w AS statut_attendu,
                lag(m.date_mvt) OVER w AS date_precedente,
                lag(op.niveau_operation) OVER w AS rang_precedent,
                op.niveau_operation AS rang
            FROM mvt_dossier m JOIN operation_dossier op ON op.id = m.id_operation
            WHERE NOT coalesce(m.est_annule, FALSE)
            WINDOW w AS (PARTITION BY m.id_dossier ORDER BY m.id)
        ) chaine
        WHERE id_ancienne_phase IS DISTINCT FROM phase_attendue
           OR id_ancien_statut IS DISTINCT FROM statut_attendu
           OR date_mvt < date_precedente OR rang <= rang_precedent
    ) THEN
        RAISE EXCEPTION 'La chaîne des mouvements doit conserver les anciennes phases/statuts et progresser dans l''ordre des opérations et des dates.';
    END IF;

    IF EXISTS (
        SELECT 1 FROM dossier d
        LEFT JOIN LATERAL (
            SELECT m.id_nouvelle_phase, m.id_nouveau_statut FROM mvt_dossier m
            WHERE m.id_dossier = d.id AND NOT coalesce(m.est_annule, FALSE)
            ORDER BY m.id DESC LIMIT 1
        ) dernier ON TRUE
        WHERE d.id_phase IS DISTINCT FROM coalesce(dernier.id_nouvelle_phase, v_preparation)
           OR d.id_statut IS DISTINCT FROM coalesce(dernier.id_nouveau_statut, v_ouvert)
    ) THEN
        RAISE EXCEPTION 'La phase et le statut de chaque dossier doivent correspondre à son dernier mouvement, ou à PREPARATION/Ouvert sans mouvement.';
    END IF;

    IF EXISTS (
        SELECT 1 FROM document_mvt dm
        JOIN document doc ON doc.id = dm.id_document
        JOIN mvt_dossier m ON m.id = dm.id_mvt_dossier
        WHERE doc.id_dossier IS DISTINCT FROM m.id_dossier OR doc.date_ajout > m.date_mvt
    ) THEN
        RAISE EXCEPTION 'Les documents joints à un mouvement doivent appartenir au même dossier et exister à la date du mouvement.';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM document_obligatoire_mvt) OR EXISTS (
        SELECT 1 FROM mvt_dossier m
        JOIN document_obligatoire_mvt obligation ON obligation.id_operation = m.id_operation
        WHERE NOT coalesce(m.est_annule, FALSE) AND NOT EXISTS (
            SELECT 1 FROM document doc
            WHERE doc.id_dossier = m.id_dossier
              AND doc.id_type_document = obligation.id_type_document
              AND doc.est_actif = TRUE AND doc.date_ajout <= m.date_mvt
              AND EXISTS (
                  SELECT 1 FROM remise r
                  WHERE r.id_dossier = m.id_dossier AND r.id_type_document = doc.id_type_document
                    AND r.date_remise <= m.date_mvt
              )
        )
    ) THEN
        RAISE EXCEPTION 'Les documents obligatoires des opérations effectuées doivent être présents, actifs et remis avant le mouvement.';
    END IF;

    IF EXISTS (SELECT 1 FROM utilisateur WHERE date_creation > NOW() OR date_modification > NOW())
       OR EXISTS (SELECT 1 FROM clients WHERE date_creation > NOW())
       OR EXISTS (SELECT 1 FROM ordre_transit WHERE date_creation > NOW())
       OR EXISTS (SELECT 1 FROM document WHERE date_ajout > NOW())
       OR EXISTS (SELECT 1 FROM remise WHERE date_remise > NOW())
       OR EXISTS (SELECT 1 FROM declaration WHERE date_ajout > NOW() OR date_declaration > NOW()) THEN
        RAISE EXCEPTION 'Les utilisateurs, clients, OT, ajouts de documents, remises et déclarations ne doivent pas être datés dans le futur.';
    END IF;

    RAISE NOTICE 'Vérifications du jeu donne_deepseek réussies dans le schéma %.', current_schema();
END;
$verification$;

-- Répartition visible dans le graphique annuel des créations.
SELECT to_char(date_creation, 'YYYY-MM') AS mois, count(*) AS dossiers
FROM dossier GROUP BY 1 ORDER BY 1;

-- Détail du graphique affiché par défaut sur les trente derniers jours.
SELECT date_creation::date AS jour, count(*) AS dossiers
FROM dossier WHERE date_creation >= CURRENT_DATE - INTERVAL '29 days'
GROUP BY 1 ORDER BY 1;

SELECT date_trunc('week', date_mvt)::date AS semaine, count(*) AS mouvements
FROM mvt_dossier WHERE date_mvt >= NOW() - INTERVAL '8 weeks'
GROUP BY 1 ORDER BY 1;

SELECT ph.nom AS phase, s.nom AS statut, count(*) AS dossiers
FROM dossier d JOIN phase_dossier ph ON ph.id = d.id_phase
JOIN statut_dossier s ON s.id = d.id_statut
GROUP BY ph.nom, s.nom ORDER BY ph.nom, s.nom;
