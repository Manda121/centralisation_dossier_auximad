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
    id_type_client REFERENCES type_client(id),

    nif VARCHAR(20) UNIQUE NOT NULL,
    stat VARCHAR(20) UNIQUE NOT NULL,
    fax VARCHAR(20),

    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE type_regime(
    id_type_regime SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(10) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE bureau_dedou(
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

CREATE TABLE pays(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(3) NOT NULL UNIQUE
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

    destination_finale VARCHAR(200) NOT NULL, -- destination geographique: Zone Franche Tanjombato, Antananarivo

    id_expediteur REFERENCES clients(id),
    id_destinataire REFERENCES clients(id),

    id_bureau_dedou REFERENCES bureau_dedou(id),

    est_assure BOOLEAN NOT NULL DEFAULT FALSE,

    destination_facture REFERENCES clients(id),

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

    id_pays_origine REFERENCES pays(id),
    id_pays_destination REFERENCES pays(id),

    est_prepaye BOOLEAN NOT NULL DEFAULT FALSE,
    est_provisionne BOOLEAN NOT NULL DEFAULT FALSE,

    provision_demandee NUMERIC(10, 2)DEFAULT 0,
    lieu_paiement_fret VARCHAR(100) NOT NULL,

-- date de livraison attendue
    depart VARCHAR(100) NOT NULL,
    date_livraison TIMESTAMP NOT NULL,
    adresse_livraison VARCHAR(200),

    id_inconterme REFERENCES inconterme(id),

    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE type_container(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50) NOT NULL UNIQUE,
    description TEXT
    volume NUMERIC(10, 2) NOT NULL,
    poids_max NUMERIC(10, 2) NOT NULL
    longueur NUMERIC(10, 2) NOT NULL,
    largeur NUMERIC(10, 2) NOT NULL,
    hauteur NUMERIC(10, 2) NOT NULL
);

CREATE TABLE nombre_container(
    id SERIAL PRIMARY KEY,
    id_dossier REFERENCES dossier(id),
    nombre INTEGER NOT NULL,
    type_container VARCHAR(50) NOT NULL,
    description TEXT
);

CREATE TABLE compagnie_maritime(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(5) NOT NULL,
    id_pays REFERENCES pays(id)
);

CREATE TABLE compagnie_aerienne(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(5) NOT NULL,
    id_pays REFERENCES pays(id)
);

CREATE TABLE navire(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL UNIQUE,
    imo VARCHAR(20) NOT NULL UNIQUE,
    id_compagnie_maritime REFERENCES compagnie_maritime(id),
    date_ajout TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE station(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(10) NOT NULL UNIQUE,
    lieu VARCHAR(100) NOT NULL,
    
    id_mode_transport REFERENCES mode_transport(id),
    description TEXT
);

CREATE TABLE expedition(
    id SERIAL PRIMARY KEY,
    id_dossier REFERENCES dossier(id),

    est_achemine BOOLEAN NOT NULL DEFAULT FALSE,
    id_mode_acheminement REFERENCES mode_acheminement(id),
    
    id_compagnie_maritime REFERENCES compagnie_maritime(id),
    numero_bl VARCHAR(100),
    id_navire REFERENCES navire(id),
    date_bl TIMESTAMP,
    lieu_bl VARCHAR(100) NOT NULL,
    port_embarquement REFERENCES station(id),
    port_debarquement REFERENCES station(id),

    id_compagnie_aerienne REFERENCES compagnie_aerienne(id),
    numero_vol VARCHAR(100),
    numero_lta VARCHAR(100),
    date_lta TIMESTAMP,
    lieu_lta VARCHAR(100),
    aeroport_embarquement REFERENCES station(id),
    aeroport_debarquement REFERENCES station(id), -- Aéroport d'Ivato à Antananarivo (Code IATA : TNR), Aéroport de Fascene à Nosy Be (NOS), etc.

    lib_scan VARCHAR(200),
    date_depart TIMESTAMP,
    date_prevu TIMESTAMP NOT NULL,
    date_arrivee TIMESTAMP NOT NULL,
);

CREATE TABLE unite_mesure(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50) NOT NULL UNIQUE,
    rapport_m_cube NUMERIC(10, 2) NOT NULL,
    description TEXT
);

CREATE TABLE marchandise(
    id SERIAL PRIMARY KEY,
    id_dossier REFERENCES dossier(id),
    id_unite_mesure REFERENCES unite_mesure(id),

    marque VARCHAR(100) NOT NULL,
    modele VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    poids NUMERIC(10, 2) NOT NULL,
    volume NUMERIC(10, 2) NOT NULL,
    quantite INTEGER NOT NULL
);

CREATE TABLE conteneur(
    id SERIAL PRIMARY KEY,
    id_dossier REFERENCES dossier(id),
    id_type_container REFERENCES type_container(id),

    numero_container VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE groupage (
    id SERIAL PRIMARY KEY,
    id_dossier REFERENCES dossier(id),
    id_expedition REFERENCES expedition(id),

    id_hbl reference,
    
    id_hawb reference,
    date_hbl TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE dossier_interagence(
    id SERIAL PRIMARY KEY,
    id_dossier_mere REFERENCES dossier(id),
    id_sous_dossier REFERENCES dossier(id),
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- mila manamboatra table historique phase sy statut dossier