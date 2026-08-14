\c postgres;

DROP DATABASE IF EXISTS dossier_auximad;

CREATE DATABASE dossier_auximad;

\c dossier_auximad;

CREATE TABLE agence(
    id SERIAL PRIMARY KEY,
    code INTEGER NOT NULL UNIQUE,
    nom VARCHAR(100) NOT NULL,
    adresse VARCHAR(200) NOT NULL,
    ville VARCHAR(100) NOT NULL,
    code_postal VARCHAR(10) NOT NULL,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE role(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE utilisateur(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    mot_de_passe VARCHAR(255) NOT NULL,

    id_role INTEGER NOT NULL REFERENCES role(id),
    id_agence INTEGER NOT NULL REFERENCES agence(id),

    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_modification TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE autorisation(
    id SERIAL PRIMARY KEY,
    id_utilisateur INTEGER NOT NULL REFERENCES utilisateur(id),
    niveau_autorisation INTEGER NOT NULL
);

CREATE TABLE type_produit(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    description TEXT
);

CREATE TABLE mode_transport(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50) NOT NULL UNIQUE,
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

    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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
    nom VARCHAR(100) NOT NULL UNIQUE,
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
    est_obligatoire BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE traffic(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50) NOT NULL UNIQUE,
    code_traffic VARCHAR(10) NOT NULL UNIQUE,
    description TEXT
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
    nom VARCHAR(100) NOT NULL UNIQUE,
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

    id_type_produit INTEGER REFERENCES type_produit(id),
    id_mode_transport INTEGER REFERENCES mode_transport(id),
    id_categorie_marchandise INTEGER REFERENCES categorie_marchandise(id),

    est_obligatoire BOOLEAN NOT NULL DEFAULT FALSE,
    description TEXT
);

CREATE TABLE ordre_transit(
    id SERIAL PRIMARY KEY,
    numero_ordre_transit INTEGER NOT NULL UNIQUE,
    id_agence INTEGER NOT NULL REFERENCES agence(id),
    id_client INTEGER NOT NULL REFERENCES clients(id),

    id_type_produit INTEGER REFERENCES type_produit(id),
    id_mode_transport INTEGER REFERENCES mode_transport(id),

    numero_commande VARCHAR(100) NOT NULL,

    id_utilisateur INTEGER NOT NULL REFERENCES utilisateur(id),
    id_type_regime INTEGER REFERENCES type_regime(id),

    destination_finale VARCHAR(200) NOT NULL, -- destination geographique: Zone Franche Tanjombato, Antananarivo

    id_expediteur INTEGER REFERENCES clients(id),
    id_destinataire INTEGER REFERENCES clients(id),

    id_bureau_dedou INTEGER REFERENCES bureau(id),

    est_assure BOOLEAN NOT NULL DEFAULT FALSE,

    id_destination_facture INTEGER REFERENCES clients(id),

    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE instruction(
    id SERIAL PRIMARY KEY,
    id_ordre_transit INTEGER NOT NULL REFERENCES ordre_transit(id),
    instruction TEXT NOT NULL,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE dossier(
    id SERIAL PRIMARY KEY,
    id_agence INTEGER NOT NULL REFERENCES agence(id),
    id_utilisateur INTEGER NOT NULL REFERENCES utilisateur(id),
    id_client INTEGER NOT NULL REFERENCES clients(id),

    numero_dossier VARCHAR(100) NOT NULL UNIQUE,
    id_ordre_transit INTEGER REFERENCES ordre_transit(id),
    id_traffic INTEGER REFERENCES traffic(id),

    est_groupage BOOLEAN NOT NULL DEFAULT FALSE,
    id_phase INTEGER NOT NULL REFERENCES phase_dossier(id),

    echeance TIMESTAMP NOT NULL,
    pour_compte VARCHAR(100) NOT NULL,

    id_pays_origine INTEGER REFERENCES pays(id),
    id_pays_destination INTEGER REFERENCES pays(id),

    est_prepaye BOOLEAN NOT NULL DEFAULT FALSE,
    est_provisionne BOOLEAN NOT NULL DEFAULT FALSE,

    provision_demandee NUMERIC(10, 2) DEFAULT 0,
    lieu_paiement_fret VARCHAR(100) NOT NULL,

    -- date de livraison attendue
    depart VARCHAR(100) NOT NULL,
    date_livraison TIMESTAMP NOT NULL,
    adresse_livraison VARCHAR(200),

    id_inconterme INTEGER REFERENCES inconterme(id),

    est_sous_dossier BOOLEAN NOT NULL DEFAULT FALSE,

    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE constat(
    id SERIAL PRIMARY KEY,
    id_dossier INTEGER NOT NULL REFERENCES dossier(id),
    id_expert_constat INTEGER NOT NULL REFERENCES expert_constat(id),
    id_type_constat INTEGER NOT NULL REFERENCES type_constat(id),
    description TEXT NOT NULL,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE document_dossier(
    id SERIAL PRIMARY KEY,
    id_dossier INTEGER NOT NULL REFERENCES dossier(id),
    id_document INTEGER NOT NULL REFERENCES type_document(id),
    chemin_fichier VARCHAR(200) NOT NULL,
    est_remis BOOLEAN NOT NULL DEFAULT FALSE,
    date_ajout TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE remise(
    id SERIAL PRIMARY KEY,
    id_dossier INTEGER NOT NULL REFERENCES dossier(id),
    id_type_document INTEGER NOT NULL REFERENCES type_document(id),
    date_remise TIMESTAMP
);

CREATE TABLE type_container(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    volume NUMERIC(10, 2) NOT NULL,
    poids_max NUMERIC(10, 2) NOT NULL,
    longueur NUMERIC(10, 2) NOT NULL,
    largeur NUMERIC(10, 2) NOT NULL,
    hauteur NUMERIC(10, 2) NOT NULL
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
    date_ajout TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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
    date_bl TIMESTAMP,
    lieu_bl VARCHAR(100),
    id_port_embarquement INTEGER REFERENCES station(id),
    id_port_debarquement INTEGER REFERENCES station(id),

    id_compagnie_aerienne INTEGER REFERENCES compagnie_aerienne(id),
    numero_vol VARCHAR(100),
    numero_lta VARCHAR(100),
    date_lta TIMESTAMP,
    lieu_lta VARCHAR(100),
    id_aeroport_embarquement INTEGER REFERENCES station(id),
    id_aeroport_debarquement INTEGER REFERENCES station(id), -- Aéroport d'Ivato à Antananarivo (Code IATA : TNR), Aéroport de Fascene à Nosy Be (NOS), etc.

    lib_scan VARCHAR(200),
    date_depart TIMESTAMP,
    date_prevu TIMESTAMP,
    date_arrivee TIMESTAMP
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
    poids NUMERIC(10, 2) NOT NULL,
    volume NUMERIC(10, 2) NOT NULL,
    quantite INTEGER NOT NULL
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

    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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
    date_mvt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE declaration(
    id SERIAL PRIMARY KEY,
    id_dossier INTEGER NOT NULL REFERENCES dossier(id),
    numero_declaration VARCHAR(100) NOT NULL UNIQUE,
    date_declaration TIMESTAMP NOT NULL,
    id_bureau INTEGER NOT NULL REFERENCES bureau(id),
    description TEXT,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);