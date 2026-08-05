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

    bl VARCHAR(100) NOT NULL,
    date_bl TIMESTAMP NOT NULL,
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

    est_constate BOOLEAN NOT NULL DEFAULT FALSE,
    nom_expert VARCHAR(100),

    destination_facture REFERENCES clients(id),

    adresse_livrainson VARCHAR(200),

    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE instruction(
    id SERIAL PRIMARY KEY,
    id_ordre_transit REFERENCES ordre_transit(id),
    instruction TEXT NOT NULL,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE statut_dossier(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE dossier(
    id SERIAL PRIMARY KEY,
    id_ordre_transit REFERENCES ordre_transit(id),
    id_utilisateur REFERENCES utilisateur(id),

    id_agence REFERENCES agence(id),

    statut VARCHAR(50) NOT NULL,
    id_statut REFERENCES statut_dossier(id), 

    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);