\set ON_ERROR_STOP on
CREATE SCHEMA seed_verification_82d41962e7da4eb18c5ae8b415fe331e;
SET search_path TO seed_verification_82d41962e7da4eb18c5ae8b415fe331e;
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
    nom VARCHAR(100), -- Un meme nom peut couvrir plusieurs codes (IM8 : 8900 et 8000).
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

-- Initialisation pour les tests manuels : configuration et comptes uniquement.
-- A executer sur les tables vides, apres reinitialiser_donnees.sql si necessaire.
-- Ne supprime aucune table et aucune donnee. Ne pas cumuler avec donne_deepseek.sql.
-- Conserve les referentiels et les 30 comptes du jeu de demonstration.
-- Aucun client, OT, dossier, mouvement, document remis ou demande interagence.
BEGIN;
-- ============================================================
-- 0. STATUTS DOSSIER DANS L'ORDRE DU DEROULEMENT
-- ============================================================
-- Après une réinitialisation, les identifiants suivent cet ordre.
-- Pour l'affichage SQL : SELECT * FROM statut_dossier ORDER BY id;
INSERT INTO statut_dossier (nom, description) VALUES
('Ouvert', 'Dossier créé, aucune opération effectuée'),
('Client avisé', 'Dernière opération effectuée : Avis d arrivée client'),
('Réservation confirmée', 'Dernière opération effectuée : Réservation TC/Ligne'),
('Documents reçus', 'Dernière opération effectuée : Réception Doc'),
('Conteneur positionné', 'Dernière opération effectuée : Position container'),
('Expertise réalisée', 'Dernière opération effectuée : Expertise'),
('Déclaration établie', 'Dernière opération effectuée : Déclaration'),
('Visite/scan effectué', 'Dernière opération effectuée : Visite/Scan'),
('Droits et taxes payés', 'Dernière opération effectuée : Paiement Droit & Taxes'),
('PG payé', 'Dernière opération effectuée : Paiement PG'),
('Bon à enlever obtenu', 'Dernière opération effectuée : Bon à enlever'),
('Provision enregistrée', 'Dernière opération effectuée : Provision (pour fret et débours)'),
('Facturé', 'Dernière opération effectuée : Facturation'),
('Payé', 'Dernière opération effectuée : Paiement'),
('Clôturé', 'Dossier finalisé après paiement et clôture'),
('En litige', 'Dossier suspendu pour un litige'),
('Annulé', 'Dossier annulé');

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
('IM4', '4000', 'definitif'),
('IM5', '5000', 'economique'),
('IM7', '7000', 'economique'),
('IM8', '8900', 'transitoire'),
('IM8', '8000', 'ordinaire'),
('EX1', '1000', 'definitif'),
('EX2', '2000', 'temporaire');

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
-- Une opération sans statut associé conserve le statut précédent du dossier.
INSERT INTO operation_dossier
    (description, id_phase, id_statut, id_type_production,
     niveau_autorisation, niveau_validation, niveau_operation, est_obligatoire, est_actif)
SELECT t.description, ph.id, st.id, tp.id,
       CASE t.phase WHEN 'PREPARATION' THEN 1 WHEN 'TRAITEMENT' THEN 2 ELSE 4 END,
       CASE WHEN t.description = 'Clôture' THEN 5
            WHEN t.phase = 'COMPTABILITE' THEN 4 WHEN t.phase = 'TRAITEMENT' THEN 2 ELSE 1 END,
       t.rang, TRUE, TRUE
FROM (VALUES
    ('import', 'PREPARATION', 'Avis d arrivée client', 'Client avisé', 1),
    ('import', 'PREPARATION', 'Envoi proforma', NULL, 2),
    ('import', 'PREPARATION', 'Premier envoi document', NULL, 3),
    ('import', 'PREPARATION', 'Premier réception document', NULL, 4),
    ('import', 'PREPARATION', 'Expertise', 'Expertise réalisée', 5),
    ('import', 'TRAITEMENT', 'Déclaration', 'Déclaration établie', 6),
    ('import', 'TRAITEMENT', 'Dispatche/Passeur', NULL, 7),
    ('import', 'TRAITEMENT', 'Visite/Scan', 'Visite/scan effectué', 8),
    ('import', 'TRAITEMENT', 'Paiement Droit & Taxes', 'Droits et taxes payés', 9),
    ('import', 'TRAITEMENT', 'Confirmation paiement Droit & Taxes', NULL, 10),
    ('import', 'COMPTABILITE', 'Provision (pour fret et débours)', 'Provision enregistrée', 11),
    ('import', 'COMPTABILITE', 'Facturation', 'Facturé', 12),
    ('import', 'COMPTABILITE', 'Transmission', NULL, 13),
    ('import', 'COMPTABILITE', 'Paiement', 'Payé', 14),
    ('import', 'COMPTABILITE', 'Clôture', 'Clôturé', 15),
    ('export', 'PREPARATION', 'Préalerte', NULL, 1),
    ('export', 'PREPARATION', 'Réservation TC/Ligne', 'Réservation confirmée', 2),
    ('export', 'PREPARATION', 'Réception Doc', 'Documents reçus', 3),
    ('export', 'PREPARATION', 'Envoi proforma', NULL, 4),
    ('export', 'PREPARATION', 'Position container', 'Conteneur positionné', 5),
    ('export', 'PREPARATION', 'Expertise', 'Expertise réalisée', 6),
    ('export', 'TRAITEMENT', 'Déclaration', 'Déclaration établie', 7),
    ('export', 'TRAITEMENT', 'Dispatche/Passeur', NULL, 8),
    ('export', 'TRAITEMENT', 'Dépôt Déclaration', NULL, 9),
    ('export', 'TRAITEMENT', 'Paiement PG', 'PG payé', 10),
    ('export', 'TRAITEMENT', 'Bon à enlever', 'Bon à enlever obtenu', 11),
    ('export', 'TRAITEMENT', 'Récupération documents', NULL, 12),
    ('export', 'COMPTABILITE', 'Provision', 'Provision enregistrée', 13),
    ('export', 'COMPTABILITE', 'Facturation', 'Facturé', 14),
    ('export', 'COMPTABILITE', 'Transmission', NULL, 15),
    ('export', 'COMPTABILITE', 'Paiement', 'Payé', 16),
    ('export', 'COMPTABILITE', 'Clôture', 'Clôturé', 17)
) AS t(sens, phase, description, statut, rang)
JOIN type_production tp ON tp.nom = t.sens
JOIN phase_dossier ph ON ph.nom = t.phase
LEFT JOIN statut_dossier st ON st.nom = t.statut
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
('Régime économique (IM5)', 'Déclaration liée au régime économique IM5'),
('Régime économique (IM7)', 'Déclaration liée au régime économique IM7'),
('Régime transitoire ou ordinaire (IM8)', 'Déclaration liée aux régimes IM8 8900 ou 8000'),
('Exportation définitive (EX1)', 'Déclaration d''exportation définitive'),
('Exportation temporaire (EX2)', 'Déclaration liée au régime temporaire EX2');

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

-- Comptes et autorisations identiques au jeu de demonstration.
-- Le mot de passe chiffre est conserve ; aucune activite metier n'est generee.
DO $$
DECLARE
    v_agences INT[] := ARRAY(SELECT id FROM agence ORDER BY id);
    v_roles INT[] := ARRAY(SELECT id FROM role ORDER BY id);
    v_agence INT;
    v_role INT;
    v_user INT;
    v_creation TIMESTAMPTZ := CURRENT_TIMESTAMP;
    i INT;
BEGIN
    FOR i IN 1..30 LOOP
        v_agence := v_agences[1 + (i - 1) % array_length(v_agences, 1)];
        v_role := v_roles[1 + (i - 1) % array_length(v_roles, 1)];
        v_creation := CURRENT_TIMESTAMP;
        INSERT INTO utilisateur (nom_utilisateur, nom, prenom, email, mot_de_passe,
                                 id_role, id_agence, date_creation, date_modification)
        VALUES ('user' || i, 'NomUser' || i, 'PrenomUser' || i, 'user' || i || '@transit.mg',
                '$2a$12$KovwAzSOJPTaWqYJ2UgE/.sztG.CGSEgGa/QfoV.qlNK8qBtXzMna',
                v_role, v_agence, v_creation, v_creation) RETURNING id INTO v_user;
        -- Même accès administrateur que le jeu de démonstration précédent,
        -- limité aux comptes insérés ici, sans modifier d'autres utilisateurs.
        INSERT INTO autorisation (id_utilisateur, niveau_autorisation) VALUES (v_user, 10000);
    END LOOP;
END;
$$;

COMMIT;

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
