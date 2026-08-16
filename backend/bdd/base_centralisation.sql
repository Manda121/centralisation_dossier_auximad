\c postgres;

DROP DATABASE IF EXISTS dossier_auximad;

CREATE DATABASE dossier_auximad;

\c dossier_auximad;

CREATE TABLE agence(
    id SERIAL PRIMARY KEY,
    code INTEGER NOT NULL UNIQUE CHECK (code BETWEEN 0 AND 99), -- 2 chiffres, cf. nomenclature numero_dossier
    nom VARCHAR(100) NOT NULL,
    adresse VARCHAR(200) NOT NULL,
    ville VARCHAR(100) NOT NULL,
    code_postal VARCHAR(10) NOT NULL,
    date_creation TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE role(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE utilisateur(
    id SERIAL PRIMARY KEY,
    nom_utilisateur VARCHAR(50) UNIQUE NOT NULL,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    mot_de_passe VARCHAR(255) NOT NULL,

    id_role INTEGER NOT NULL REFERENCES role(id),
    id_agence INTEGER NOT NULL REFERENCES agence(id),

    date_creation TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    date_modification TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE autorisation(
    id SERIAL PRIMARY KEY,
    id_utilisateur INTEGER NOT NULL REFERENCES utilisateur(id),
    niveau_autorisation INTEGER NOT NULL
);

CREATE TABLE mode_transport(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50) NOT NULL,
    description TEXT
);

CREATE TABLE type_production(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    id_mode_transport INTEGER NOT NULL REFERENCES mode_transport(id),
    code INTEGER NOT NULL,
    description TEXT
);

CREATE TABLE type_client(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE clients(
    id SERIAL PRIMARY KEY,
    cp_client VARCHAR(10) NOT NULL,

    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    telephone VARCHAR(15),
    adresse VARCHAR(200),
    id_type_client INTEGER NOT NULL REFERENCES type_client(id),

    nif VARCHAR(20) UNIQUE NOT NULL,
    stat VARCHAR(20) UNIQUE NOT NULL,
    fax VARCHAR(20),

    date_creation TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE type_regime(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(10) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE type_bureau(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE bureau(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    adresse VARCHAR(200) NOT NULL,
    code_postal VARCHAR(10) NOT NULL,
    ville VARCHAR(100) NOT NULL,
    description TEXT,

    id_type_bureau INTEGER NOT NULL REFERENCES type_bureau(id)
);

CREATE TABLE mode_acheminement(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE categorie_marchandise(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE phase_dossier(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE operation_dossier(
    id SERIAL PRIMARY KEY,
    description TEXT NOT NULL,

    id_phase INTEGER NOT NULL REFERENCES phase_dossier(id),

    niveau_autorisation INTEGER NOT NULL,
    niveau_validation INTEGER NOT NULL,

    niveau_operation INTEGER NOT NULL,
    est_actif BOOLEAN NOT NULL DEFAULT TRUE,
    est_obligatoire BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE inconterme(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50) NOT NULL UNIQUE,
    abreviation VARCHAR(10) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE type_constat(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE expert_constat(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    telephone VARCHAR(15),
    adresse VARCHAR(200)
);

CREATE TABLE pays(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(3) NOT NULL UNIQUE
);

CREATE TABLE type_document(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL UNIQUE,

    id_type_production INTEGER REFERENCES type_production(id),
    id_categorie_marchandise INTEGER REFERENCES categorie_marchandise(id),

    est_obligatoire BOOLEAN NOT NULL DEFAULT FALSE,
    description TEXT
);

CREATE TABLE ordre_transit(
    id SERIAL PRIMARY KEY,
    numero_ordre_transit INTEGER NOT NULL UNIQUE,
    id_agence INTEGER NOT NULL REFERENCES agence(id),
    id_client INTEGER NOT NULL REFERENCES clients(id),

    id_type_production INTEGER REFERENCES type_production(id),

    numero_commande VARCHAR(100) NOT NULL,

    id_utilisateur INTEGER NOT NULL REFERENCES utilisateur(id),
    id_type_regime INTEGER REFERENCES type_regime(id),

    destination_finale VARCHAR(200) NOT NULL, -- destination geographique: Zone Franche Tanjombato, Antananarivo

    id_expediteur INTEGER REFERENCES clients(id),
    id_destinataire INTEGER REFERENCES clients(id),

    id_bureau_dedou INTEGER REFERENCES bureau(id),

    est_assure BOOLEAN NOT NULL DEFAULT FALSE,

    id_destination_facture INTEGER REFERENCES clients(id),

    date_creation TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE instruction(
    id SERIAL PRIMARY KEY,
    id_ordre_transit INTEGER NOT NULL REFERENCES ordre_transit(id),
    instruction TEXT NOT NULL,
    date_creation TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE dossier(
    id SERIAL PRIMARY KEY,
    id_agence INTEGER NOT NULL REFERENCES agence(id),
    id_utilisateur INTEGER NOT NULL REFERENCES utilisateur(id),
    id_client INTEGER NOT NULL REFERENCES clients(id),
    id_type_production INTEGER NOT NULL REFERENCES type_production(id),

    numero_dossier VARCHAR(100) NOT NULL UNIQUE,
    -- nomenclature numero dossier: code_production(1) code_agence(2) mois(2) annee(1: dernier chiffre) numero_chonologique(4) : ex: 02007300001
    id_ordre_transit INTEGER REFERENCES ordre_transit(id),

    est_groupage BOOLEAN NOT NULL DEFAULT FALSE,
    id_phase INTEGER NOT NULL REFERENCES phase_dossier(id),

    echeance TIMESTAMPTZ NOT NULL,
    pour_compte VARCHAR(100) NOT NULL,

    id_pays_origine INTEGER REFERENCES pays(id),
    id_pays_destination INTEGER REFERENCES pays(id),

    est_prepaye BOOLEAN NOT NULL DEFAULT FALSE,
    est_provisionne BOOLEAN NOT NULL DEFAULT FALSE,

    provision_demandee NUMERIC(10, 2) DEFAULT 0 CHECK (provision_demandee >= 0),
    lieu_paiement_fret VARCHAR(100) NOT NULL,

    -- date de livraison attendue
    depart VARCHAR(100) NOT NULL,
    date_livraison TIMESTAMPTZ NOT NULL,
    adresse_livraison VARCHAR(200),

    id_inconterme INTEGER REFERENCES inconterme(id),

    est_sous_dossier BOOLEAN NOT NULL DEFAULT FALSE,

    date_creation TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE constat(
    id SERIAL PRIMARY KEY,
    id_dossier INTEGER NOT NULL REFERENCES dossier(id),
    id_expert_constat INTEGER NOT NULL REFERENCES expert_constat(id),
    id_type_constat INTEGER NOT NULL REFERENCES type_constat(id),
    description TEXT NOT NULL,
    date_creation TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE document_dossier(
    id SERIAL PRIMARY KEY,
    id_dossier INTEGER NOT NULL REFERENCES dossier(id),
    id_document INTEGER NOT NULL REFERENCES type_document(id),
    chemin_fichier VARCHAR(200) NOT NULL,
    est_remis BOOLEAN NOT NULL DEFAULT FALSE,
    date_ajout TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE remise(
    id SERIAL PRIMARY KEY,
    id_dossier INTEGER NOT NULL REFERENCES dossier(id),
    id_type_document INTEGER NOT NULL REFERENCES type_document(id),
    date_remise TIMESTAMPTZ
);

CREATE TABLE type_container(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    volume NUMERIC(10, 2) NOT NULL CHECK (volume > 0),
    poids_max NUMERIC(10, 2) NOT NULL CHECK (poids_max > 0),
    longueur NUMERIC(10, 2) NOT NULL CHECK (longueur > 0),
    largeur NUMERIC(10, 2) NOT NULL CHECK (largeur > 0),
    hauteur NUMERIC(10, 2) NOT NULL CHECK (hauteur > 0)
);

CREATE TABLE compagnie_maritime(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(5) NOT NULL,
    id_pays INTEGER NOT NULL REFERENCES pays(id)
);

CREATE TABLE compagnie_aerienne(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(5) NOT NULL,
    id_pays INTEGER NOT NULL REFERENCES pays(id)
);

CREATE TABLE navire(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL UNIQUE,
    imo VARCHAR(20) NOT NULL UNIQUE,
    id_compagnie_maritime INTEGER NOT NULL REFERENCES compagnie_maritime(id),
    date_ajout TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE station(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(10) NOT NULL UNIQUE,
    lieu VARCHAR(100) NOT NULL,

    id_mode_transport INTEGER NOT NULL REFERENCES mode_transport(id),
    description TEXT
);

CREATE TABLE expedition(
    id SERIAL PRIMARY KEY,
    id_dossier INTEGER NOT NULL REFERENCES dossier(id),

    est_achemine BOOLEAN NOT NULL DEFAULT FALSE,
    id_mode_acheminement INTEGER REFERENCES mode_acheminement(id),

    id_compagnie_maritime INTEGER REFERENCES compagnie_maritime(id),
    numero_bl VARCHAR(100),
    id_navire INTEGER REFERENCES navire(id),
    date_bl TIMESTAMPTZ,
    lieu_bl VARCHAR(100),
    id_port_embarquement INTEGER REFERENCES station(id),
    id_port_debarquement INTEGER REFERENCES station(id),

    id_compagnie_aerienne INTEGER REFERENCES compagnie_aerienne(id),
    numero_vol VARCHAR(100),
    numero_lta VARCHAR(100),
    date_lta TIMESTAMPTZ,
    lieu_lta VARCHAR(100),
    id_aeroport_embarquement INTEGER REFERENCES station(id),
    id_aeroport_debarquement INTEGER REFERENCES station(id), -- Aéroport d'Ivato à Antananarivo (Code IATA : TNR), Aéroport de Fascene à Nosy Be (NOS), etc.

    lib_scan VARCHAR(200),
    date_depart TIMESTAMPTZ,
    date_prevu TIMESTAMPTZ,
    date_arrivee TIMESTAMPTZ,

    CHECK (id_compagnie_maritime IS NULL OR id_compagnie_aerienne IS NULL) -- un seul mode par expedition
);

CREATE TABLE unite_mesure(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50) NOT NULL UNIQUE,
    rapport_m_cube NUMERIC(10, 2) NOT NULL,
    description TEXT
);

CREATE TABLE marchandise(
    id SERIAL PRIMARY KEY,
    id_dossier INTEGER NOT NULL REFERENCES dossier(id),
    id_unite_mesure INTEGER NOT NULL REFERENCES unite_mesure(id),

    id_categorie_marchandise INTEGER REFERENCES categorie_marchandise(id),

    marque VARCHAR(100) NOT NULL,
    modele VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    poids NUMERIC(10, 2) NOT NULL CHECK (poids >= 0),
    volume NUMERIC(10, 2) NOT NULL CHECK (volume >= 0),
    quantite INTEGER NOT NULL CHECK (quantite > 0)
);

CREATE TABLE conteneur(
    id SERIAL PRIMARY KEY,
    id_dossier INTEGER NOT NULL REFERENCES dossier(id),
    id_type_container INTEGER NOT NULL REFERENCES type_container(id),

    numero_container VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
);

-- table pour les hbl et hawb
CREATE TABLE groupage(
    id SERIAL PRIMARY KEY,
    id_dossier INTEGER NOT NULL REFERENCES dossier(id),
    id_expedition INTEGER REFERENCES expedition(id),

    numero_groupage VARCHAR(100) NOT NULL UNIQUE,
    partie_notifier TEXT,

    date_creation TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE dossier_interagence(
    id SERIAL PRIMARY KEY,
    id_dossier_mere INTEGER NOT NULL REFERENCES dossier(id),
    id_sous_dossier INTEGER NOT NULL REFERENCES dossier(id),
    id_agence_mere INTEGER NOT NULL REFERENCES agence(id),
    id_agence_sous INTEGER NOT NULL REFERENCES agence(id),
    numero_dossier_mere VARCHAR(100) NOT NULL,
    numero_sous_dossier VARCHAR(100) NOT NULL,
    motif TEXT,
    date_creation TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- mila manamboatra table historique phase sy statut dossier

CREATE TABLE mvt_dossier(
    id SERIAL PRIMARY KEY,
    id_dossier INTEGER NOT NULL REFERENCES dossier(id),
    id_utilisateur INTEGER NOT NULL REFERENCES utilisateur(id),
    ip_utilisateur INET,
    id_operation INTEGER NOT NULL REFERENCES operation_dossier(id),
    id_document_dossier INTEGER REFERENCES document_dossier(id),
    commentaire TEXT,
    date_mvt TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE declaration(
    id SERIAL PRIMARY KEY,
    id_dossier INTEGER NOT NULL REFERENCES dossier(id),
    numero_declaration VARCHAR(100) NOT NULL UNIQUE,
    date_declaration TIMESTAMPTZ NOT NULL,
    id_bureau INTEGER NOT NULL REFERENCES bureau(id),
    description TEXT,
    date_creation TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE suspension_connexion(
    id SERIAL PRIMARY KEY,
    id_utilisateur INTEGER NOT NULL UNIQUE REFERENCES utilisateur(id),
    tentatives_echouees INTEGER NOT NULL DEFAULT 0,
    date_suspension TIMESTAMPTZ,
    duree_suspension_minutes INTEGER DEFAULT 15,
    date_creation TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Index pour accélérer les jointures et recherches les plus fréquentes
CREATE INDEX idx_dossier_client ON dossier(id_client);
CREATE INDEX idx_dossier_agence ON dossier(id_agence);
CREATE INDEX idx_dossier_utilisateur ON dossier(id_utilisateur);
CREATE INDEX idx_dossier_ordre_transit ON dossier(id_ordre_transit);
CREATE INDEX idx_dossier_phase ON dossier(id_phase);
CREATE INDEX idx_ordre_transit_client ON ordre_transit(id_client);
CREATE INDEX idx_ordre_transit_agence ON ordre_transit(id_agence);
CREATE INDEX idx_ordre_transit_utilisateur ON ordre_transit(id_utilisateur);
CREATE INDEX idx_document_dossier_dossier ON document_dossier(id_dossier);
CREATE INDEX idx_remise_dossier ON remise(id_dossier);
CREATE INDEX idx_expedition_dossier ON expedition(id_dossier);
CREATE INDEX idx_marchandise_dossier ON marchandise(id_dossier);
CREATE INDEX idx_conteneur_dossier ON conteneur(id_dossier);
CREATE INDEX idx_groupage_dossier ON groupage(id_dossier);
CREATE INDEX idx_constat_dossier ON constat(id_dossier);
CREATE INDEX idx_declaration_dossier ON declaration(id_dossier);
CREATE INDEX idx_mvt_dossier_dossier ON mvt_dossier(id_dossier);
CREATE INDEX idx_mvt_dossier_utilisateur ON mvt_dossier(id_utilisateur);
CREATE INDEX idx_instruction_ordre_transit ON instruction(id_ordre_transit);
CREATE INDEX idx_utilisateur_agence ON utilisateur(id_agence);