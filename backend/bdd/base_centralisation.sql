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

    id_role REFERENCES role(id),
    id_agence REFERENCES agence(id),

    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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
    ville VARCHAR(100),
    id_type_client REFERENCES type_client(id),

    nif VARCHAR(20) UNIQUE NOT NULL,
    stat VARCHAR(20) UNIQUE NOT NULL,
    fax VARCHAR(20),

    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE type_regime(
    id_type_regime SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE bureau_dedou(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE compagnie_maritime(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE mode_acheminement(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50) NOT NULL UNIQUE,
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
    
    id_phase REFERENCES phase_dossier(id),
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
    adresse VARCHAR(200),
);

CREATE TABLE ordre_transit(
    id SERIAL PRIMARY KEY,
    numero_ordre_transit INTEGER NOT NULL UNIQUE,
    id_agence REFERENCES agence(id),
    id_client REFERENCES clients(id),

    id_type_produit REFERENCES type_produit(id),
    id_mode_transport REFERENCES mode_transport(id),

    numero_commande VARCHAR(100) NOT NULL,

    id_utilisateur REFERENCES utilisateur(id),
    id_type_regime REFERENCES type_regime(id_type_regime),

    destination_finale VARCHAR(200) NOT NULL,

    id_expediteur REFERENCES clients(id),
    id_destinataire REFERENCES clients(id),

    id_bureau_dedou REFERENCES bureau_dedou(id),

    bl VARCHAR(100),
    date_bl TIMESTAMP,
    nom_navire VARCHAR(100) NOT NULL,
    date_prevu TIMESTAMP NOT NULL,
    date_arrivee TIMESTAMP NOT NULL,
    id_compagnie_maritime REFERENCES compagnie_maritime(id),

    numero_vol VARCHAR(100),
    date_vol TIMESTAMP,
    numero_lta VARCHAR(100),
    date_lta TIMESTAMP,
    aeroport_depart VARCHAR(100),
    destination VARCHAR(100),

    est_achemine BOOLEAN NOT NULL DEFAULT FALSE,
    id_mode_acheminement REFERENCES mode_acheminement(id),

    est_assure BOOLEAN NOT NULL DEFAULT FALSE,

    destination_facture REFERENCES clients(id),

    adresse_livraison VARCHAR(200),

    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE constat(
    id SERIAL PRIMARY KEY,
    id_dossier REFERENCES dossier(id),
    id_expert_constat REFERENCES expert_constat(id),
    id_type_constat REFERENCES type_constat(id),
    description TEXT NOT NULL,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE instruction(
    id SERIAL PRIMARY KEY,
    id_ordre_transit REFERENCES ordre_transit(id),
    instruction TEXT NOT NULL,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE dossier(
    id SERIAL PRIMARY KEY,
    id_agence REFERENCES agence(id),
    id_utilisateur REFERENCES utilisateur(id),
    id_client REFERENCES clients(id),

    numero_dossier VARCHAR(100) NOT NULL UNIQUE,
    id_ordre_transit REFERENCES ordre_transit(id),
    id_traffic REFERENCES traffic(id),

    est_groupage BOOLEAN NOT NULL DEFAULT FALSE,
    id_phase REFERENCES phase_dossier(id),

    echeance TIMESTAMP NOT NULL,
    pour_compte VARCHAR(100) NOT NULL,

    est_prepaye BOOLEAN NOT NULL DEFAULT FALSE,
    est_provisionne BOOLEAN NOT NULL DEFAULT FALSE,

    provision_demandee NUMERIC(10, 2)DEFAULT 0,
    lieu_paiement_fret VARCHAR(100) NOT NULL,
    lieu_emission VARCHAR(100) NOT NULL,

-- date de livraison attendue
    date_livraison TIMESTAMP NOT NULL,

    id_inconterme REFERENCES inconterme(id),

    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pays(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL UNIQUE,
    code_pays VARCHAR(10) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE compagnie_maritime(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL UNIQUE,
    id_pays REFERENCES pays(id)
);

CREATE TABLE compagnie_aerienne(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL UNIQUE,
    id_pays REFERENCES pays(id)
);

CREATE TABLE expedition(
    id SERIAL PRIMARY KEY,
    id_dossier REFERENCES dossier(id),

    id_compagnie_maritime REFERENCES compagnie_maritime(id),
    BL VARCHAR(100) NOT NULL,
    date_BL TIMESTAMP NOT NULL,
    lieu_paiment_fret VARCHAR(100) NOT NULL,
    lieu_emission VARCHAR(100) NOT NULL,

    id_compagnie_aerienne REFERENCES compagnie_aerienne(id),
);

CREATE TABLE dossier_interagence(
    id SERIAL PRIMARY KEY,
    id_dossier_mere REFERENCES dossier(id),
    id_sous_dossier REFERENCES dossier(id),
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- mila manamboatra table historique phase sy statut dossier