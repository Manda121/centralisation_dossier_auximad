CREATE TABLE agence(
   id COUNTER,
   code INT NOT NULL,
   nom VARCHAR(100) NOT NULL,
   adresse VARCHAR(200) NOT NULL,
   ville VARCHAR(100) NOT NULL,
   code_postal VARCHAR(10) NOT NULL,
   date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
   PRIMARY KEY(id),
   UNIQUE(code)
);

CREATE TABLE role(
   id COUNTER,
   nom VARCHAR(50) NOT NULL,
   description TEXT,
   PRIMARY KEY(id),
   UNIQUE(nom)
);

CREATE TABLE type_produit(
   id COUNTER,
   nom VARCHAR(100) NOT NULL,
   description TEXT,
   PRIMARY KEY(id)
);

CREATE TABLE expert_constat(
   id COUNTER,
   nom VARCHAR(100) NOT NULL,
   prenom VARCHAR(100) NOT NULL,
   email VARCHAR(100) NOT NULL,
   telephone VARCHAR(15),
   adresse VARCHAR(200),
   PRIMARY KEY(id),
   UNIQUE(nom),
   UNIQUE(email)
);

CREATE TABLE mode_transport(
   id COUNTER,
   nom VARCHAR(50) NOT NULL,
   description TEXT,
   PRIMARY KEY(id),
   UNIQUE(nom)
);

CREATE TABLE type_client(
   id COUNTER,
   nom VARCHAR(50) NOT NULL,
   description TEXT,
   PRIMARY KEY(id),
   UNIQUE(nom)
);

CREATE TABLE clients(
   id COUNTER,
   cp_client VARCHAR(10) NOT NULL,
   nom VARCHAR(100) NOT NULL,
   prenom VARCHAR(100) NOT NULL,
   email VARCHAR(100) NOT NULL,
   telephone VARCHAR(15),
   adresse VARCHAR(200),
   nif VARCHAR(20) NOT NULL,
   stat VARCHAR(20) NOT NULL,
   fax VARCHAR(20),
   date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
   id_type_client INT NOT NULL,
   PRIMARY KEY(id),
   UNIQUE(email),
   UNIQUE(nif),
   UNIQUE(stat),
   FOREIGN KEY(id_type_client) REFERENCES type_client(id)
);

CREATE TABLE type_regime(
   id COUNTER,
   nom VARCHAR(100) NOT NULL,
   code VARCHAR(10) NOT NULL,
   description TEXT,
   PRIMARY KEY(id),
   UNIQUE(nom),
   UNIQUE(code)
);

CREATE TABLE type_bureau(
   id COUNTER,
   nom VARCHAR(100) NOT NULL,
   description TEXT,
   PRIMARY KEY(id),
   UNIQUE(nom)
);

CREATE TABLE bureau(
   id COUNTER,
   nom VARCHAR(100) NOT NULL,
   description TEXT,
   id_type_bureau INT NOT NULL,
   PRIMARY KEY(id),
   UNIQUE(nom),
   FOREIGN KEY(id_type_bureau) REFERENCES type_bureau(id)
);

CREATE TABLE mode_acheminement(
   id COUNTER,
   nom VARCHAR(50) NOT NULL,
   description TEXT,
   PRIMARY KEY(id),
   UNIQUE(nom)
);

CREATE TABLE categorie_marchandise(
   id COUNTER,
   nom VARCHAR(100) NOT NULL,
   description TEXT,
   PRIMARY KEY(id),
   UNIQUE(nom)
);

CREATE TABLE unite_mesure(
   id COUNTER,
   nom VARCHAR(50) NOT NULL,
   rapport_m_cube DECIMAL(10,2) NOT NULL,
   description TEXT,
   PRIMARY KEY(id),
   UNIQUE(nom)
);

CREATE TABLE phase_dossier(
   id COUNTER,
   nom VARCHAR(50) NOT NULL,
   description TEXT,
   PRIMARY KEY(id),
   UNIQUE(nom)
);

CREATE TABLE operation_dossier(
   id COUNTER,
   description TEXT NOT NULL,
   niveau_autorisation INT NOT NULL,
   niveau_validation INT NOT NULL,
   niveau_operation INT NOT NULL,
   est_obligatoire LOGICAL NOT NULL DEFAULT FALSE,
   id_phase INT NOT NULL,
   PRIMARY KEY(id),
   FOREIGN KEY(id_phase) REFERENCES phase_dossier(id)
);

CREATE TABLE traffic(
   id COUNTER,
   nom VARCHAR(50) NOT NULL,
   code_traffic VARCHAR(10) NOT NULL,
   description TEXT,
   PRIMARY KEY(id),
   UNIQUE(nom),
   UNIQUE(code_traffic)
);

CREATE TABLE inconterme(
   id COUNTER,
   nom VARCHAR(50) NOT NULL,
   abreviation VARCHAR(10) NOT NULL,
   description TEXT,
   PRIMARY KEY(id),
   UNIQUE(nom),
   UNIQUE(abreviation)
);

CREATE TABLE type_constat(
   id COUNTER,
   nom VARCHAR(100) NOT NULL,
   description TEXT,
   PRIMARY KEY(id),
   UNIQUE(nom)
);

CREATE TABLE pays(
   id COUNTER,
   nom VARCHAR(100) NOT NULL,
   code VARCHAR(3) NOT NULL,
   PRIMARY KEY(id),
   UNIQUE(nom),
   UNIQUE(code)
);

CREATE TABLE compagnie_maritime(
   id COUNTER,
   nom VARCHAR(100) NOT NULL,
   code VARCHAR(5) NOT NULL,
   id_pays INT NOT NULL,
   PRIMARY KEY(id),
   UNIQUE(nom),
   FOREIGN KEY(id_pays) REFERENCES pays(id)
);

CREATE TABLE navire(
   id COUNTER,
   nom VARCHAR(100) NOT NULL,
   imo VARCHAR(20) NOT NULL,
   date_ajout DATETIME DEFAULT CURRENT_TIMESTAMP,
   id_compagnie_maritime INT NOT NULL,
   PRIMARY KEY(id),
   UNIQUE(nom),
   UNIQUE(imo),
   FOREIGN KEY(id_compagnie_maritime) REFERENCES compagnie_maritime(id)
);

CREATE TABLE type_container(
   id COUNTER,
   nom VARCHAR(50) NOT NULL,
   description TEXT,
   volume DECIMAL(10,2) NOT NULL,
   poids_max DECIMAL(10,2) NOT NULL,
   longueur DECIMAL(10,2) NOT NULL,
   largeur DECIMAL(10,2) NOT NULL,
   hauteur DECIMAL(10,2) NOT NULL,
   PRIMARY KEY(id),
   UNIQUE(nom)
);

CREATE TABLE compagnie_aerienne(
   id COUNTER,
   nom VARCHAR(100) NOT NULL,
   code VARCHAR(5) NOT NULL,
   id_pays INT NOT NULL,
   PRIMARY KEY(id),
   UNIQUE(nom),
   FOREIGN KEY(id_pays) REFERENCES pays(id)
);

CREATE TABLE station(
   id COUNTER,
   nom VARCHAR(100) NOT NULL,
   code VARCHAR(10) NOT NULL,
   lieu VARCHAR(100) NOT NULL,
   description TEXT,
   id_mode_transport INT NOT NULL,
   PRIMARY KEY(id),
   UNIQUE(nom),
   UNIQUE(code),
   FOREIGN KEY(id_mode_transport) REFERENCES mode_transport(id)
);

CREATE TABLE utilisateur(
   id COUNTER,
   nom VARCHAR(100) NOT NULL,
   prenom VARCHAR(100) NOT NULL,
   email VARCHAR(100) NOT NULL,
   mot_de_passe VARCHAR(255) NOT NULL,
   date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
   date_modification DATETIME DEFAULT CURRENT_TIMESTAMP,
   id_agence INT NOT NULL,
   id_role INT NOT NULL,
   PRIMARY KEY(id),
   UNIQUE(email),
   FOREIGN KEY(id_agence) REFERENCES agence(id),
   FOREIGN KEY(id_role) REFERENCES role(id)
);

CREATE TABLE autorisation(
   id COUNTER,
   niveau_autorisation INT NOT NULL,
   id_utilisateur INT NOT NULL,
   PRIMARY KEY(id),
   FOREIGN KEY(id_utilisateur) REFERENCES utilisateur(id)
);

CREATE TABLE type_document(
   id COUNTER,
   nom VARCHAR(100) NOT NULL,
   est_obligatoire LOGICAL NOT NULL DEFAULT FALSE,
   description TEXT,
   id_categorie_marchandise INT,
   id_mode_transport INT,
   id_type_produit INT,
   PRIMARY KEY(id),
   UNIQUE(nom),
   FOREIGN KEY(id_categorie_marchandise) REFERENCES categorie_marchandise(id),
   FOREIGN KEY(id_mode_transport) REFERENCES mode_transport(id),
   FOREIGN KEY(id_type_produit) REFERENCES type_produit(id)
);

CREATE TABLE ordre_transit(
   id COUNTER,
   numero_ordre_transit INT NOT NULL,
   numero_commande VARCHAR(100) NOT NULL,
   destination_finale VARCHAR(200) NOT NULL,
   est_assure LOGICAL NOT NULL DEFAULT FALSE,
   date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
   id_destination_facture INT,
   id_bureau_dedou INT,
   id_destinataire INT,
   id_expediteur INT,
   id_type_regime INT,
   id_utilisateur INT NOT NULL,
   id_mode_transport INT,
   id_type_produit INT,
   id_client INT NOT NULL,
   id_agence INT NOT NULL,
   PRIMARY KEY(id),
   UNIQUE(numero_ordre_transit),
   FOREIGN KEY(id_destination_facture) REFERENCES clients(id),
   FOREIGN KEY(id_bureau_dedou) REFERENCES bureau(id),
   FOREIGN KEY(id_destinataire) REFERENCES clients(id),
   FOREIGN KEY(id_expediteur) REFERENCES clients(id),
   FOREIGN KEY(id_type_regime) REFERENCES type_regime(id),
   FOREIGN KEY(id_utilisateur) REFERENCES utilisateur(id),
   FOREIGN KEY(id_mode_transport) REFERENCES mode_transport(id),
   FOREIGN KEY(id_type_produit) REFERENCES type_produit(id),
   FOREIGN KEY(id_client) REFERENCES clients(id),
   FOREIGN KEY(id_agence) REFERENCES agence(id)
);

CREATE TABLE instruction(
   id COUNTER,
   instruction TEXT NOT NULL,
   date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
   id_ordre_transit INT NOT NULL,
   PRIMARY KEY(id),
   FOREIGN KEY(id_ordre_transit) REFERENCES ordre_transit(id)
);

CREATE TABLE dossier(
   id COUNTER,
   numero_dossier VARCHAR(100) NOT NULL,
   est_groupage LOGICAL NOT NULL DEFAULT FALSE,
   echeance DATETIME NOT NULL,
   pour_compte VARCHAR(100) NOT NULL,
   est_prepaye LOGICAL NOT NULL DEFAULT FALSE,
   est_provisionne LOGICAL NOT NULL DEFAULT FALSE,
   provision_demandee DECIMAL(10,2) DEFAULT 0,
   lieu_paiement_fret VARCHAR(100) NOT NULL,
   depart VARCHAR(100) NOT NULL,
   date_livraison DATETIME NOT NULL,
   adresse_livraison VARCHAR(200),
   est_sous_dossier LOGICAL NOT NULL DEFAULT FALSE,
   date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
   id_inconterme INT,
   id_pays_destination INT,
   id_pays_origine INT,
   id_phase INT NOT NULL,
   id_traffic INT,
   id_ordre_transit INT,
   id_utilisateur INT NOT NULL,
   id_agence INT NOT NULL,
   PRIMARY KEY(id),
   UNIQUE(numero_dossier),
   FOREIGN KEY(id_inconterme) REFERENCES inconterme(id),
   FOREIGN KEY(id_pays_destination) REFERENCES pays(id),
   FOREIGN KEY(id_pays_origine) REFERENCES pays(id),
   FOREIGN KEY(id_phase) REFERENCES phase_dossier(id),
   FOREIGN KEY(id_traffic) REFERENCES traffic(id),
   FOREIGN KEY(id_ordre_transit) REFERENCES ordre_transit(id),
   FOREIGN KEY(id_utilisateur) REFERENCES utilisateur(id),
   FOREIGN KEY(id_agence) REFERENCES agence(id)
);

CREATE TABLE constat(
   id COUNTER,
   description TEXT NOT NULL,
   date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
   id_type_constat INT NOT NULL,
   id_expert_constat INT NOT NULL,
   id_dossier INT NOT NULL,
   PRIMARY KEY(id),
   FOREIGN KEY(id_type_constat) REFERENCES type_constat(id),
   FOREIGN KEY(id_expert_constat) REFERENCES expert_constat(id),
   FOREIGN KEY(id_dossier) REFERENCES dossier(id)
);

CREATE TABLE declaration(
   id COUNTER,
   numero_declaration VARCHAR(100) NOT NULL,
   date_declaration DATETIME NOT NULL,
   description TEXT,
   date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
   id_bureau INT NOT NULL,
   id_dossier INT NOT NULL,
   PRIMARY KEY(id),
   UNIQUE(numero_declaration),
   FOREIGN KEY(id_bureau) REFERENCES bureau(id),
   FOREIGN KEY(id_dossier) REFERENCES dossier(id)
);

CREATE TABLE expedition(
   id COUNTER,
   est_achemine LOGICAL NOT NULL DEFAULT FALSE,
   numero_bl VARCHAR(100),
   date_bl DATETIME,
   lieu_bl VARCHAR(100),
   numero_vol VARCHAR(100),
   numero_lta VARCHAR(100),
   date_lta DATETIME,
   lieu_lta VARCHAR(100),
   lib_scan VARCHAR(200),
   date_depart DATETIME,
   date_prevu DATETIME,
   date_arrivee DATETIME,
   id_aeroport_debarquement INT,
   id_aeroport_embarquement INT,
   id_compagnie_aerienne INT,
   id_port_debarquement INT,
   id_port_embarquement INT,
   id_navire INT,
   id_compagnie_maritime INT,
   id_mode_acheminement INT,
   id_dossier INT NOT NULL,
   PRIMARY KEY(id),
   FOREIGN KEY(id_aeroport_debarquement) REFERENCES station(id),
   FOREIGN KEY(id_aeroport_embarquement) REFERENCES station(id),
   FOREIGN KEY(id_compagnie_aerienne) REFERENCES compagnie_aerienne(id),
   FOREIGN KEY(id_port_debarquement) REFERENCES station(id),
   FOREIGN KEY(id_port_embarquement) REFERENCES station(id),
   FOREIGN KEY(id_navire) REFERENCES navire(id),
   FOREIGN KEY(id_compagnie_maritime) REFERENCES compagnie_maritime(id),
   FOREIGN KEY(id_mode_acheminement) REFERENCES mode_acheminement(id),
   FOREIGN KEY(id_dossier) REFERENCES dossier(id)
);

CREATE TABLE groupage(
   id COUNTER,
   numero_groupage VARCHAR(100) NOT NULL,
   partie_notifier TEXT,
   date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
   id_expedition INT,
   id_dossier INT NOT NULL,
   PRIMARY KEY(id),
   UNIQUE(numero_groupage),
   FOREIGN KEY(id_expedition) REFERENCES expedition(id),
   FOREIGN KEY(id_dossier) REFERENCES dossier(id)
);

CREATE TABLE marchandise(
   id COUNTER,
   marque VARCHAR(100) NOT NULL,
   modele VARCHAR(100) NOT NULL,
   description TEXT NOT NULL,
   poids DECIMAL(10,2) NOT NULL,
   volume DECIMAL(10,2) NOT NULL,
   quantite INT NOT NULL,
   id_categorie_marchandise INT,
   id_unite_mesure INT NOT NULL,
   id_dossier INT NOT NULL,
   PRIMARY KEY(id),
   FOREIGN KEY(id_categorie_marchandise) REFERENCES categorie_marchandise(id),
   FOREIGN KEY(id_unite_mesure) REFERENCES unite_mesure(id),
   FOREIGN KEY(id_dossier) REFERENCES dossier(id)
);

CREATE TABLE conteneur(
   id COUNTER,
   numero_container VARCHAR(100) NOT NULL,
   description TEXT,
   id_type_container INT NOT NULL,
   id_dossier INT NOT NULL,
   PRIMARY KEY(id),
   UNIQUE(numero_container),
   FOREIGN KEY(id_type_container) REFERENCES type_container(id),
   FOREIGN KEY(id_dossier) REFERENCES dossier(id)
);

CREATE TABLE remise(
   id COUNTER,
   date_remise DATETIME,
   id_type_document INT NOT NULL,
   id_dossier INT NOT NULL,
   PRIMARY KEY(id),
   FOREIGN KEY(id_type_document) REFERENCES type_document(id),
   FOREIGN KEY(id_dossier) REFERENCES dossier(id)
);

CREATE TABLE dossier_interagence(
   id COUNTER,
   numero_dossier_mere VARCHAR(100) NOT NULL,
   numero_sous_dossier VARCHAR(100) NOT NULL,
   motif TEXT,
   date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
   id_agence_sous INT NOT NULL,
   id_agence_mere INT NOT NULL,
   id_sous_dossier INT NOT NULL,
   id_dossier_mere INT NOT NULL,
   PRIMARY KEY(id),
   FOREIGN KEY(id_agence_sous) REFERENCES agence(id),
   FOREIGN KEY(id_agence_mere) REFERENCES agence(id),
   FOREIGN KEY(id_sous_dossier) REFERENCES dossier(id),
   FOREIGN KEY(id_dossier_mere) REFERENCES dossier(id)
);

CREATE TABLE document_dossier(
   id COUNTER,
   chemin_fichier VARCHAR(200) NOT NULL,
   est_remis LOGICAL NOT NULL DEFAULT FALSE,
   date_ajout DATETIME DEFAULT CURRENT_TIMESTAMP,
   id_document INT NOT NULL,
   id_dossier INT NOT NULL,
   PRIMARY KEY(id),
   FOREIGN KEY(id_document) REFERENCES type_document(id),
   FOREIGN KEY(id_dossier) REFERENCES dossier(id)
);

CREATE TABLE mvt_dossier(
   id COUNTER,
   ip_utilisateur INET,
   commentaire TEXT,
   date_mvt DATETIME DEFAULT CURRENT_TIMESTAMP,
   id_operation INT NOT NULL,
   id_utilisateur INT NOT NULL,
   id_dossier INT NOT NULL,
   PRIMARY KEY(id),
   FOREIGN KEY(id_operation) REFERENCES operation_dossier(id),
   FOREIGN KEY(id_utilisateur) REFERENCES utilisateur(id),
   FOREIGN KEY(id_dossier) REFERENCES dossier(id)
);
