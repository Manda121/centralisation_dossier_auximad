BEGIN;

-- ============================================================
-- 1. TABLES DE REFERENCE (lookup)
-- ============================================================

INSERT INTO agence (code, nom, adresse, ville, code_postal) VALUES
(20, 'TNR', '18, RUE J.J. Rabearivelo-Antsahavola', 'Antananarivo', '101'),
(90, 'DIR', '18, RUE J.J. Rabearivelo-Antsahavola', 'Antananarivo', '101'),
(10, 'TMA', '21, Bd Ratsimilaho Po Box 93- 501', 'Toamasina', '501'),
(30, 'MJG', 'Avenue de France, Majunga', 'Mahajanga', '401'),
(70, 'DIEGO', 'Place Kabary, Rue Flacourt', 'Antsiranana', '201'),
(50, 'TUL', 'Amorondriaka,Face Zaza club centre ville', 'Toliara', '601'),
(40, 'Manakara', 'Avenue de l''Independance', 'Manakara', '316'),
(21, 'A/BE', 'Villa Francois Lot 02-B-177', 'Antsirabe', '110'),
(80, 'NOSY BE', 'Rue de Passot, Hell-Ville BP:1', 'Nosy Be', '207'),
(93, 'FORT-Dauphin', 'Rue Maréchal Foch', 'Fort-Dauphin', '614');

INSERT INTO role (nom, description) VALUES
('Admin', 'Administrateur systeme'),
('Agent', 'Agent de transit'),
('ChefAgence', 'Chef d''agence'),
('Lecteur', 'Consultation seule'),
('Validateur', 'Validation des operations');

INSERT INTO mode_transport (nom, description) VALUES
('Maritime', 'Transport par mer'),
('Aerien', 'Transport par avion'),
('Terrestre', 'Transport par route');

INSERT INTO type_production (nom, id_mode_transport, code, description) VALUES
('import', ('maritime'), 1, 'import'),
('import', ('aerien'), 4, 'import'),
('export', ('maritime'), 0, 'export'),
('export', ('aerien'), 0, 'export'),
('divers', ('terrestre'), 9, ''),
('colis postaux', ('terestre'), 2, 'transport de colis').

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

INSERT INTO type_bureau (nom, description) VALUES
('Bureau de transfert', 'Bureau de transeft'),
('Bureau mise en entrepot', 'Bureau de mise en entrepot'),
('Bureau de douane', 'Bureau de douane');

INSERT INTO bureau (nom, description, id_type_bureau, adresse, code_postal, ville) VALUES
('Bureau des douanes Antananarivo', 'Bureau principal des douanes a Antananarivo', (SELECT id FROM type_bureau WHERE nom='Bureau principal')),
('Bureau des douanes Toamasina', 'Bureau de douane du port', (SELECT id FROM type_bureau WHERE nom='Bureau de douane')),
('Bureau secondaire Antsirabe', 'Bureau secondaire', (SELECT id FROM type_bureau WHERE nom='Bureau secondaire')),
('Bureau de controle Ivato', 'Bureau de controle aeroport', (SELECT id FROM type_bureau WHERE nom='Bureau de controle')),
('Bureau d''enquete Mahajanga', 'Bureau d''enquete', (SELECT id FROM type_bureau WHERE nom='Bureau d''enquete')),
('Bureau des douanes Antsiranana', 'Bureau de douane', (SELECT id FROM type_bureau WHERE nom='Bureau de douane')),
('Bureau secondaire Fianarantsoa', 'Bureau secondaire', (SELECT id FROM type_bureau WHERE nom='Bureau secondaire')),
('Bureau de controle Nosy Be', 'Bureau de controle', (SELECT id FROM type_bureau WHERE nom='Bureau de controle')),
('Bureau des douanes Toliara', 'Bureau de douane', (SELECT id FROM type_bureau WHERE nom='Bureau de douane')),
('Bureau secondaire Morondava', 'Bureau secondaire', (SELECT id FROM type_bureau WHERE nom='Bureau secondaire')),
('Bureau des douanes Sambava', 'Bureau de douane', (SELECT id FROM type_bureau WHERE nom='Bureau de douane')),
('Bureau principal Tamatave', 'Bureau principal', (SELECT id FROM type_bureau WHERE nom='Bureau principal')),
('Bureau de controle Aeroport Ivato', 'Bureau de controle', (SELECT id FROM type_bureau WHERE nom='Bureau de controle')),
('Bureau d''enquete Antananarivo', 'Bureau d''enquete', (SELECT id FROM type_bureau WHERE nom='Bureau d''enquete')),
('Bureau secondaire Mahajanga', 'Bureau secondaire', (SELECT id FROM type_bureau WHERE nom='Bureau secondaire')),
('Bureau de douane Port Toamasina', 'Bureau de douane', (SELECT id FROM type_bureau WHERE nom='Bureau de douane')),
('Bureau de controle Port Toamasina', 'Bureau de controle', (SELECT id FROM type_bureau WHERE nom='Bureau de controle')),
('Bureau secondaire Antsiranana', 'Bureau secondaire', (SELECT id FROM type_bureau WHERE nom='Bureau secondaire')),
('Bureau des douanes Ivato', 'Bureau de douane', (SELECT id FROM type_bureau WHERE nom='Bureau de douane')),
('Bureau secondaire Toliara', 'Bureau secondaire', (SELECT id FROM type_bureau WHERE nom='Bureau secondaire'));

INSERT INTO mode_acheminement (nom, description) VALUES
('pas notre soins', ''),
('Par Camion', 'Conteneur complet');

INSERT INTO categorie_marchandise (nom, description) VALUES
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

INSERT INTO phase_dossier (nom, description) VALUES
('PREPARATION', ''),
('TRAITEMENT', 'En attente de documents'),
('DEBARQUEMENT/LIVRAISON', 'Dossier en cours de traitement'),
('COMPTABILITE', 'Dossier en cours de dedouanement');

INSERT INTO operation_dossier (description, id_phase, niveau_autorisation, niveau_validation, niveau_operation, est_obligatoire, staut) VALUES
('Reservation TC/Ligne', (SELECT id FROM phase_dossier WHERE nom='preparation'), 1, 1, 1, TRUE),
('Reception BL/LTA', (SELECT id FROM phase_dossier WHERE nom='preparation'), 1, 1, 1, TRUE),
('Position Container', (SELECT id FROM phase_dossier WHERE nom='preparation'), 2, 2, 2, TRUE),
('Declaration', (SELECT id FROM phase_dossier WHERE nom='traitement'), 2, 2, 2, TRUE),
('Dispatch/Passeur', (SELECT id FROM phase_dossier WHERE nom='traitement'), 2, 3, 2, TRUE),
('Depot Declaration', (SELECT id FROM phase_dossier WHERE nom='traitement'), 2, 3, 2, TRUE),
('Visite Douane', (SELECT id FROM phase_dossier WHERE nom='traitement'), 3, 4, 3, TRUE),
('Empotage', (SELECT id FROM phase_dossier WHERE nom='traitement'), 3, 4, 3, TRUE),
('Depart Camion', (SELECT id FROM phase_dossier WHERE nom='debarquement/livraison'), 3, 4, 3, TRUE),
('Arrivee Camion', (SELECT id FROM phase_dossier WHERE nom='debarquement/livraison'), 3, 4, 3, TRUE),
('Mise a quai', (SELECT id FROM phase_dossier WHERE nom='debarquement/livraison'), 4, 5, 4, TRUE),
('Taxation BAE', (SELECT id FROM phase_dossier WHERE nom='debarquement/livraison'), 2, 3, 2, TRUE),
('Closing date', (SELECT id FROM phase_dossier WHERE nom='debarquement/livraison'), 3, 4, 3, TRUE),
('Provision', (SELECT id FROM phase_dossier WHERE nom='comptabilite'), 3, 4, 3, FALSE),
('Facturation', (SELECT id FROM phase_dossier WHERE nom='comptabilite'), 4, 5, 4, TRUE),
('Transmission', (SELECT id FROM phase_dossier WHERE nom='comptabilite'), 4, 5, 4, TRUE),
('Paiement', (SELECT id FROM phase_dossier WHERE nom='comptabilite'), 4, 5, 5, TRUE),
('Cloture', (SELECT id FROM phase_dossier WHERE nom='comptabilite'), 4, 5, 5, TRUE);

INSERT INTO traffic (nom, code_traffic, description) VALUES
('Import', 'IMP', 'Importation'),
('Export', 'EXP', 'Exportation'),
('Transit', 'TRS', 'Transit'),
('Reexport', 'REX', 'Reexportation'),
('Admission temporaire', 'ADT', 'Admission temporaire');

INSERT INTO inconterme (nom, abreviation, description) VALUES
('...', 'FOB', '...');

INSERT INTO type_constat (nom, description) VALUES
('Avarie', 'Constat d''avarie'),
('Manquant', 'Constat de manquant'),
('Retard', 'Constat de retard'),
('Dommage', 'Constat de dommage'),
('Autre', 'Autre constat');

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

INSERT INTO type_document (nom, id_type_production, id_mode_transport, id_categorie_marchandise, est_obligatoire, description) VALUES
('Facture commerciale', NULL, NULL, NULL, TRUE, 'Facture commerciale'),
('Liste de colisage', NULL, NULL, NULL, TRUE, 'Liste de colisage'),
('Connaissement maritime', NULL, (SELECT id FROM mode_transport WHERE nom='Maritime'), NULL, TRUE, 'Connaissement maritime'),
('Lettre de transport aerien', NULL, (SELECT id FROM mode_transport WHERE nom='Aerien'), NULL, TRUE, 'Lettre de transport aerien'),
('Certificat d''origine', NULL, NULL, NULL, TRUE, 'Certificat d''origine'),
('Certificat phytosanitaire', (SELECT id FROM type_production WHERE nom='Alimentaire'), NULL, (SELECT id FROM categorie_marchandise WHERE nom='Alimentaire'), FALSE, 'Certificat phytosanitaire'),
('Certificat de qualite', NULL, NULL, NULL, TRUE, 'Certificat de qualite'),
('Certificat d''assurance', NULL, NULL, NULL, FALSE, 'Certificat d''assurance'),
('Licence d''importation', NULL, NULL, NULL, TRUE, 'Licence d''importation'),
('Permis d''exportation', NULL, NULL, NULL, FALSE, 'Permis d''exportation'),
('Attestation de verification', NULL, NULL, NULL, TRUE, 'Attestation de verification'),
('Bordereau de suivi', NULL, NULL, NULL, TRUE, 'Bordereau de suivi'),
('Declaration en douane', NULL, NULL, NULL, TRUE, 'Declaration en douane'),
('Facture proforma', NULL, NULL, NULL, FALSE, 'Facture proforma'),
('Bon de commande', NULL, NULL, NULL, FALSE, 'Bon de commande'),
('Certificat de non-manipulation', (SELECT id FROM type_production WHERE nom='Alimentaire'), NULL, (SELECT id FROM categorie_marchandise WHERE nom='Alimentaire'), FALSE, 'Certificat de non-manipulation'),
('Certificat veterinaire', (SELECT id FROM type_production WHERE nom='Alimentaire'), NULL, (SELECT id FROM categorie_marchandise WHERE nom='Alimentaire'), FALSE, 'Certificat veterinaire'),
('Certificat de salubrite', (SELECT id FROM type_production WHERE nom='Alimentaire'), NULL, (SELECT id FROM categorie_marchandise WHERE nom='Alimentaire'), FALSE, 'Certificat de salubrite'),
('Fiche technique', NULL, NULL, NULL, FALSE, 'Fiche technique'),
('Rapport d''inspection', NULL, NULL, NULL, FALSE, 'Rapport d''inspection'),
('Preuve de paiement', NULL, NULL, NULL, FALSE, 'Preuve de paiement'),
('Recu de depot', NULL, NULL, NULL, FALSE, 'Recu de depot'),
('Lettre de credit', NULL, NULL, NULL, FALSE, 'Lettre de credit'),
('Contrat de transport', NULL, NULL, NULL, FALSE, 'Contrat de transport'),
('Packing list', NULL, NULL, NULL, TRUE, 'Packing list'),
('Certificat d''empotage', NULL, (SELECT id FROM mode_transport WHERE nom='Maritime'), NULL, FALSE, 'Certificat d''empotage'),
('Certificat de depotage', NULL, (SELECT id FROM mode_transport WHERE nom='Maritime'), NULL, FALSE, 'Certificat de depotage'),
('Manifeste cargo', NULL, (SELECT id FROM mode_transport WHERE nom='Maritime'), NULL, TRUE, 'Manifeste cargo'),
('Manifeste aerien', NULL, (SELECT id FROM mode_transport WHERE nom='Aerien'), NULL, TRUE, 'Manifeste aerien'),
('Certificat de fret', NULL, NULL, NULL, FALSE, 'Certificat de fret'),
('Attestation de non-radioactivite', NULL, NULL, NULL, FALSE, 'Attestation de non-radioactivite'),
('Certificat d''analyse', (SELECT id FROM type_production WHERE nom='Pharmacie'), NULL, (SELECT id FROM categorie_marchandise WHERE nom='Pharmacie'), FALSE, 'Certificat d''analyse'),
('Licence pharmaceutique', (SELECT id FROM type_production WHERE nom='Pharmacie'), NULL, (SELECT id FROM categorie_marchandise WHERE nom='Pharmacie'), FALSE, 'Licence pharmaceutique'),
('Certificat de conformite', (SELECT id FROM type_production WHERE nom='Electronique'), NULL, (SELECT id FROM categorie_marchandise WHERE nom='Electronique'), TRUE, 'Certificat de conformite'),
('Declaration de conformite CE', (SELECT id FROM type_production WHERE nom='Electronique'), NULL, (SELECT id FROM categorie_marchandise WHERE nom='Electronique'), FALSE, 'Declaration de conformite CE'),
('Homologation', (SELECT id FROM type_production WHERE nom='Vehicules'), NULL, (SELECT id FROM categorie_marchandise WHERE nom='Vehicules'), FALSE, 'Homologation'),
('Certificat de carrosserie', (SELECT id FROM type_production WHERE nom='Vehicules'), NULL, (SELECT id FROM categorie_marchandise WHERE nom='Vehicules'), FALSE, 'Certificat de carrosserie'),
('Fiche d''homologation', (SELECT id FROM type_production WHERE nom='Vehicules'), NULL, (SELECT id FROM categorie_marchandise WHERE nom='Vehicules'), FALSE, 'Fiche d''homologation'),
('Certificat de traitement du bois', (SELECT id FROM type_production WHERE nom='Bois'), NULL, (SELECT id FROM categorie_marchandise WHERE nom='Bois'), FALSE, 'Certificat de traitement du bois'),
('Permis CITES', (SELECT id FROM type_production WHERE nom='Bois'), NULL, (SELECT id FROM categorie_marchandise WHERE nom='Bois'), FALSE, 'Permis CITES'),
('Certificat de fumigation', (SELECT id FROM type_production WHERE nom='Bois'), NULL, (SELECT id FROM categorie_marchandise WHERE nom='Bois'), FALSE, 'Certificat de fumigation'),
('Fiche de donnees de securite', (SELECT id FROM type_production WHERE nom='Produits chimiques'), NULL, (SELECT id FROM categorie_marchandise WHERE nom='Produits chimiques'), TRUE, 'Fiche de donnees de securite'),
('Certificat de danger', (SELECT id FROM type_production WHERE nom='Produits chimiques'), NULL, (SELECT id FROM categorie_marchandise WHERE nom='Produits chimiques'), TRUE, 'Certificat de danger'),
('Declaration de marchandises dangereuses', (SELECT id FROM type_production WHERE nom='Produits chimiques'), NULL, (SELECT id FROM categorie_marchandise WHERE nom='Produits chimiques'), TRUE, 'Declaration de marchandises dangereuses'),
('Certificat de resistance', (SELECT id FROM type_production WHERE nom='Materiaux de construction'), NULL, (SELECT id FROM categorie_marchandise WHERE nom='Materiaux de construction'), FALSE, 'Certificat de resistance'),
('Rapport d''essai', (SELECT id FROM type_production WHERE nom='Materiaux de construction'), NULL, (SELECT id FROM categorie_marchandise WHERE nom='Materiaux de construction'), FALSE, 'Rapport d''essai'),
('Certificat de garantie', (SELECT id FROM type_production WHERE nom='Machines industrielles'), NULL, (SELECT id FROM categorie_marchandise WHERE nom='Machines industrielles'), FALSE, 'Certificat de garantie'),
('Manuel technique', (SELECT id FROM type_production WHERE nom='Machines industrielles'), NULL, (SELECT id FROM categorie_marchandise WHERE nom='Machines industrielles'), FALSE, 'Manuel technique'),
('Certificat de poids', NULL, NULL, NULL, FALSE, 'Certificat de poids'),
('Attestation de non-manipulation', NULL, NULL, NULL, FALSE, 'Attestation de non-manipulation');

INSERT INTO type_container (nom, description, volume, poids_max, longueur, largeur, hauteur) VALUES
('20 pieds', 'Conteneur standard 20 pieds', 33.2, 28000, 6.06, 2.44, 2.59),
('40 pieds', 'Conteneur standard 40 pieds', 67.7, 30000, 12.19, 2.44, 2.59),
('40 pieds High Cube', 'Conteneur 40 pieds high cube', 76.4, 30000, 12.19, 2.44, 2.90),
('45 pieds High Cube', 'Conteneur 45 pieds high cube', 86.0, 30000, 13.72, 2.44, 2.90),
('Open Top', 'Conteneur open top', 65.0, 28000, 12.19, 2.44, 2.59),
('Reefer', 'Conteneur refrigere', 60.0, 28000, 12.19, 2.44, 2.59);

INSERT INTO compagnie_maritime (nom, code, id_pays) VALUES
('Maersk', 'MAE', (SELECT id FROM pays WHERE code='DNK')),
('MSC', 'MSC', (SELECT id FROM pays WHERE code='CHE')),
('CMA CGM', 'CMA', (SELECT id FROM pays WHERE code='FRA')),
('COSCO', 'COS', (SELECT id FROM pays WHERE code='CHN')),
('Hapag-Lloyd', 'HAP', (SELECT id FROM pays WHERE code='DEU')),
('Evergreen', 'EVE', (SELECT id FROM pays WHERE code='TWN')),
('ONE', 'ONE', (SELECT id FROM pays WHERE code='JPN')),
('HMM', 'HMM', (SELECT id FROM pays WHERE code='KOR')),
('Yang Ming', 'YML', (SELECT id FROM pays WHERE code='TWN')),
('PIL', 'PIL', (SELECT id FROM pays WHERE code='SGP'));

INSERT INTO compagnie_aerienne (nom, code, id_pays) VALUES
('Air France', 'AFR', (SELECT id FROM pays WHERE code='FRA')),
('Air Madagascar', 'MDG', (SELECT id FROM pays WHERE code='MDG')),
('Kenya Airways', 'KQA', (SELECT id FROM pays WHERE code='KEN')),
('Ethiopian Airlines', 'ETH', (SELECT id FROM pays WHERE code='ETH')),
('Emirates', 'UAE', (SELECT id FROM pays WHERE code='ARE')),
('Qatar Airways', 'QTR', (SELECT id FROM pays WHERE code='QAT')),
('Turkish Airlines', 'THY', (SELECT id FROM pays WHERE code='TUR')),
('Corsair', 'CRL', (SELECT id FROM pays WHERE code='FRA')),
('Air Mauritius', 'MAU', (SELECT id FROM pays WHERE code='MUS')),
('Air Austral', 'REU', (SELECT id FROM pays WHERE code='REU'));

INSERT INTO navire (nom, imo, id_compagnie_maritime) VALUES
('CMA CGM Marco Polo', 'IMO9454436', (SELECT id FROM compagnie_maritime WHERE code='CMA')),
('MSC Oscar', 'IMO9703291', (SELECT id FROM compagnie_maritime WHERE code='MSC')),
('Maersk Triple E', 'IMO9632064', (SELECT id FROM compagnie_maritime WHERE code='MAE')),
('Ever Given', 'IMO9811000', (SELECT id FROM compagnie_maritime WHERE code='EVE')),
('HMM Algeciras', 'IMO9863296', (SELECT id FROM compagnie_maritime WHERE code='HMM')),
('COSCO Shipping Universe', 'IMO9795610', (SELECT id FROM compagnie_maritime WHERE code='COS')),
('ONE Trust', 'IMO9863578', (SELECT id FROM compagnie_maritime WHERE code='ONE')),
('Yang Ming Wellbeing', 'IMO9861611', (SELECT id FROM compagnie_maritime WHERE code='YML')),
('PIL Kota Pahlawan', 'IMO9628236', (SELECT id FROM compagnie_maritime WHERE code='PIL')),
('Hapag-Lloyd Al Nefud', 'IMO9708776', (SELECT id FROM compagnie_maritime WHERE code='HAP')),
('MSC Gulsun', 'IMO9839430', (SELECT id FROM compagnie_maritime WHERE code='MSC')),
('Maersk Edinburgh', 'IMO9458078', (SELECT id FROM compagnie_maritime WHERE code='MAE')),
('CMA CGM Antoine de Saint Exupery', 'IMO9776418', (SELECT id FROM compagnie_maritime WHERE code='CMA')),
('Ever Ace', 'IMO9893890', (SELECT id FROM compagnie_maritime WHERE code='EVE')),
('HMM Copenhagen', 'IMO9863301', (SELECT id FROM compagnie_maritime WHERE code='HMM')),
('COSCO Shipping Pisces', 'IMO9783481', (SELECT id FROM compagnie_maritime WHERE code='COS')),
('ONE Continuity', 'IMO9863592', (SELECT id FROM compagnie_maritime WHERE code='ONE')),
('Yang Ming Warranty', 'IMO9861623', (SELECT id FROM compagnie_maritime WHERE code='YML')),
('PIL Kota Cempaka', 'IMO9628248', (SELECT id FROM compagnie_maritime WHERE code='PIL')),
('Hapag-Lloyd Berlin Express', 'IMO9851185', (SELECT id FROM compagnie_maritime WHERE code='HAP'));

INSERT INTO station (nom, code, lieu, id_mode_transport, description) VALUES
('Port de Toamasina', 'PTOM', 'Toamasina', (SELECT id FROM mode_transport WHERE nom='Maritime'), 'Port principal de Madagascar'),
('Port d''Antsiranana', 'PDAN', 'Antsiranana', (SELECT id FROM mode_transport WHERE nom='Maritime'), 'Port du nord'),
('Port de Mahajanga', 'PMAH', 'Mahajanga', (SELECT id FROM mode_transport WHERE nom='Maritime'), 'Port de l''ouest'),
('Port de Toliara', 'PTOL', 'Toliara', (SELECT id FROM mode_transport WHERE nom='Maritime'), 'Port du sud'),
('Port de Marseille', 'PMRS', 'Marseille', (SELECT id FROM mode_transport WHERE nom='Maritime'), 'Port francais'),
('Port du Havre', 'PHAV', 'Le Havre', (SELECT id FROM mode_transport WHERE nom='Maritime'), 'Port francais'),
('Port d''Anvers', 'PANV', 'Anvers', (SELECT id FROM mode_transport WHERE nom='Maritime'), 'Port belge'),
('Port de Rotterdam', 'PRTM', 'Rotterdam', (SELECT id FROM mode_transport WHERE nom='Maritime'), 'Port neerlandais'),
('Port de Hambourg', 'PHAM', 'Hambourg', (SELECT id FROM mode_transport WHERE nom='Maritime'), 'Port allemand'),
('Port de Shanghai', 'PSHA', 'Shanghai', (SELECT id FROM mode_transport WHERE nom='Maritime'), 'Port chinois'),
('Port de Singapour', 'PSIN', 'Singapour', (SELECT id FROM mode_transport WHERE nom='Maritime'), 'Port de Singapour'),
('Port de Busan', 'PBUS', 'Busan', (SELECT id FROM mode_transport WHERE nom='Maritime'), 'Port coreen'),
('Port de Jebel Ali', 'PJEA', 'Dubai', (SELECT id FROM mode_transport WHERE nom='Maritime'), 'Port emirati'),
('Port de Durban', 'PDUR', 'Durban', (SELECT id FROM mode_transport WHERE nom='Maritime'), 'Port sud-africain'),
('Port de Mombasa', 'PMOM', 'Mombasa', (SELECT id FROM mode_transport WHERE nom='Maritime'), 'Port kenyan'),
('Aeroport Ivato', 'TNR', 'Antananarivo', (SELECT id FROM mode_transport WHERE nom='Aerien'), 'Aeroport international d''Antananarivo'),
('Aeroport de Nosy Be', 'NOS', 'Nosy Be', (SELECT id FROM mode_transport WHERE nom='Aerien'), 'Aeroport de Nosy Be'),
('Aeroport de Toamasina', 'TMM', 'Toamasina', (SELECT id FROM mode_transport WHERE nom='Aerien'), 'Aeroport de Toamasina'),
('Aeroport Roissy Charles de Gaulle', 'CDG', 'Paris', (SELECT id FROM mode_transport WHERE nom='Aerien'), 'Aeroport parisien'),
('Aeroport d''Orly', 'ORY', 'Paris', (SELECT id FROM mode_transport WHERE nom='Aerien'), 'Aeroport parisien'),
('Aeroport de Francfort', 'FRA', 'Francfort', (SELECT id FROM mode_transport WHERE nom='Aerien'), 'Aeroport allemand'),
('Aeroport d''Amsterdam Schiphol', 'AMS', 'Amsterdam', (SELECT id FROM mode_transport WHERE nom='Aerien'), 'Aeroport neerlandais'),
('Aeroport de Londres Heathrow', 'LHR', 'Londres', (SELECT id FROM mode_transport WHERE nom='Aerien'), 'Aeroport britannique'),
('Aeroport de Bruxelles', 'BRU', 'Bruxelles', (SELECT id FROM mode_transport WHERE nom='Aerien'), 'Aeroport belge'),
('Aeroport de Geneve', 'GVA', 'Geneve', (SELECT id FROM mode_transport WHERE nom='Aerien'), 'Aeroport suisse'),
('Aeroport de Dubai', 'DXB', 'Dubai', (SELECT id FROM mode_transport WHERE nom='Aerien'), 'Aeroport emirati'),
('Aeroport de Doha', 'DOH', 'Doha', (SELECT id FROM mode_transport WHERE nom='Aerien'), 'Aeroport qatari'),
('Aeroport d''Istanbul', 'IST', 'Istanbul', (SELECT id FROM mode_transport WHERE nom='Aerien'), 'Aeroport turc'),
('Aeroport d''Addis-Abeba', 'ADD', 'Addis-Abeba', (SELECT id FROM mode_transport WHERE nom='Aerien'), 'Aeroport ethiopien'),
('Aeroport de Nairobi', 'NBO', 'Nairobi', (SELECT id FROM mode_transport WHERE nom='Aerien'), 'Aeroport kenyan');

INSERT INTO unite_mesure (nom, rapport_m_cube, description) VALUES
('Metre cube', 1.00, 'Metre cube'),
('Litre', 0.001, 'Litre');

-- ============================================================
-- 2. GENERATION DE DONNEES EN MASSE
-- ============================================================

DO $$
DECLARE
    v_agence_ids INT[];
    v_role_ids INT[];
    v_type_production_ids INT[];
    v_mode_transport_ids INT[];
    v_type_client_ids INT[];
    v_type_regime_ids INT[];
    v_bureau_ids INT[];
    v_mode_acheminement_ids INT[];
    v_categorie_marchandise_ids INT[];
    v_phase_ids INT[];
    v_traffic_ids INT[];
    v_inconterme_ids INT[];
    v_type_constat_ids INT[];
    v_expert_constat_ids INT[];
    v_pays_ids INT[];
    v_type_document_ids INT[];
    v_type_container_ids INT[];
    v_compagnie_maritime_ids INT[];
    v_compagnie_aerienne_ids INT[];
    v_navire_ids INT[];
    v_port_ids INT[];
    v_aeroport_ids INT[];
    v_unite_mesure_ids INT[];
    v_operation_ids INT[];

    v_client_ids INT[];
    v_utilisateur_ids INT[];
    v_ot_ids INT[] := '{}';
    v_dossier_ids INT[] := '{}';
    v_expedition_ids INT[] := '{}';
    v_document_dossier_ids INT[] := '{}';

    i INT;
    v_user_id INT;
    v_ot_id INT;
    v_dossier_id INT;
    v_expedition_id INT;
    v_agence_id INT;
    v_client_id INT;
    v_type_production_id INT;
    v_mode_transport_id INT;
    v_type_regime_id INT;
    v_bureau_id INT;
    v_traffic_id INT;
    v_phase_id INT;
    v_pays_origine INT;
    v_pays_destination INT;
    v_inconterme_id INT;
    v_mode_acheminement_id INT;
    v_type_document_id INT;
    v_operation_id INT;
    v_type_container_id INT;
    v_expert_constat_id INT;
    v_type_constat_id INT;
    v_unite_mesure_id INT;
    v_categorie_marchandise_id INT;
    v_compagnie_maritime_id INT;
    v_compagnie_aerienne_id INT;
    v_navire_id INT;
    v_port_emb INT;
    v_port_deb INT;
    v_aeroport_emb INT;
    v_aeroport_deb INT;
    v_dossier_mere INT;
    v_sous_dossier INT;
BEGIN
    -- Recuperation des IDs de reference
    v_agence_ids := ARRAY(SELECT id FROM agence);
    v_role_ids := ARRAY(SELECT id FROM role);
    v_type_production_ids := ARRAY(SELECT id FROM type_production);
    v_mode_transport_ids := ARRAY(SELECT id FROM mode_transport);
    v_type_client_ids := ARRAY(SELECT id FROM type_client);
    v_type_regime_ids := ARRAY(SELECT id FROM type_regime);
    v_bureau_ids := ARRAY(SELECT id FROM bureau);
    v_mode_acheminement_ids := ARRAY(SELECT id FROM mode_acheminement);
    v_categorie_marchandise_ids := ARRAY(SELECT id FROM categorie_marchandise);
    v_phase_ids := ARRAY(SELECT id FROM phase_dossier);
    v_traffic_ids := ARRAY(SELECT id FROM traffic);
    v_inconterme_ids := ARRAY(SELECT id FROM inconterme);
    v_type_constat_ids := ARRAY(SELECT id FROM type_constat);
    v_expert_constat_ids := ARRAY(SELECT id FROM expert_constat);
    v_pays_ids := ARRAY(SELECT id FROM pays);
    v_type_document_ids := ARRAY(SELECT id FROM type_document);
    v_type_container_ids := ARRAY(SELECT id FROM type_container);
    v_compagnie_maritime_ids := ARRAY(SELECT id FROM compagnie_maritime);
    v_compagnie_aerienne_ids := ARRAY(SELECT id FROM compagnie_aerienne);
    v_navire_ids := ARRAY(SELECT id FROM navire);
    v_port_ids := ARRAY(SELECT id FROM station WHERE id_mode_transport = (SELECT id FROM mode_transport WHERE nom='Maritime'));
    v_aeroport_ids := ARRAY(SELECT id FROM station WHERE id_mode_transport = (SELECT id FROM mode_transport WHERE nom='Aerien'));
    v_unite_mesure_ids := ARRAY(SELECT id FROM unite_mesure);
    v_operation_ids := ARRAY(SELECT id FROM operation_dossier);

    -- ============================================================
    -- CLIENTS (100)
    -- ============================================================
    FOR i IN 1..100 LOOP
        INSERT INTO clients (cp_client, nom, prenom, email, telephone, adresse, id_type_client, nif, stat, fax)
        VALUES (
            'CP' || lpad(i::text, 5, '0'),
            'ClientNom' || i,
            'Prenom' || i,
            'client' || i || '@mail.com',
            '032' || lpad(i::text, 7, '0'),
            'Adresse ' || i,
            v_type_client_ids[1 + floor(random() * array_length(v_type_client_ids, 1))::int],
            'NIF' || lpad(i::text, 10, '0'),
            'STAT' || lpad(i::text, 10, '0'),
            'Fax' || i
        );
    END LOOP;

    -- ============================================================
    -- UTILISATEURS (30) + AUTORISATIONS
    -- ============================================================
    FOR i IN 1..30 LOOP
        v_agence_id := v_agence_ids[1 + floor(random() * array_length(v_agence_ids, 1))::int];
        INSERT INTO utilisateur (nom_utilisateur, nom, prenom, email, mot_de_passe, id_role, id_agence)
        VALUES (
            'user' || i,
            'NomUser' || i,
            'PrenomUser' || i,
            'user' || i || '@transit.mg',
            'hashed_password',
            v_role_ids[1 + floor(random() * array_length(v_role_ids, 1))::int],
            v_agence_id
        ) RETURNING id INTO v_user_id;

        INSERT INTO autorisation (id_utilisateur, niveau_autorisation)
        VALUES (v_user_id, 1 + floor(random() * 5)::int);
    END LOOP;

    -- Recuperation des IDs clients et utilisateurs
    v_client_ids := ARRAY(SELECT id FROM clients);
    v_utilisateur_ids := ARRAY(SELECT id FROM utilisateur);

    -- ============================================================
    -- ORDRES DE TRANSIT (200)
    -- ============================================================
    FOR i IN 1..200 LOOP
        v_agence_id := v_agence_ids[1 + floor(random() * array_length(v_agence_ids, 1))::int];
        v_client_id := v_client_ids[1 + floor(random() * array_length(v_client_ids, 1))::int];
        v_type_production_id := v_type_production_ids[1 + floor(random() * array_length(v_type_production_ids, 1))::int];
        v_mode_transport_id := v_mode_transport_ids[1 + floor(random() * array_length(v_mode_transport_ids, 1))::int];
        v_type_regime_id := v_type_regime_ids[1 + floor(random() * array_length(v_type_regime_ids, 1))::int];
        v_bureau_id := v_bureau_ids[1 + floor(random() * array_length(v_bureau_ids, 1))::int];

        INSERT INTO ordre_transit (
            numero_ordre_transit, id_agence, id_client, id_type_production, id_mode_transport,
            numero_commande, id_utilisateur, id_type_regime, destination_finale,
            id_expediteur, id_destinataire, id_bureau_dedou, est_assure, id_destination_facture
        ) VALUES (
            10000 + i,
            v_agence_id,
            v_client_id,
            v_type_production_id,
            v_mode_transport_id,
            'CMD-' || lpad(i::text, 6, '0'),
            v_utilisateur_ids[1 + floor(random() * array_length(v_utilisateur_ids, 1))::int],
            v_type_regime_id,
            'Destination finale ' || i,
            v_client_ids[1 + floor(random() * array_length(v_client_ids, 1))::int],
            v_client_ids[1 + floor(random() * array_length(v_client_ids, 1))::int],
            v_bureau_id,
            (random() < 0.3),
            v_client_ids[1 + floor(random() * array_length(v_client_ids, 1))::int]
        ) RETURNING id INTO v_ot_id;

        v_ot_ids := array_append(v_ot_ids, v_ot_id);
    END LOOP;

    -- ============================================================
    -- INSTRUCTIONS (400)
    -- ============================================================
    FOR i IN 1..400 LOOP
        INSERT INTO instruction (id_ordre_transit, instruction)
        VALUES (
            v_ot_ids[1 + floor(random() * array_length(v_ot_ids, 1))::int],
            'Instruction ' || i || ' : suivi du dossier.'
        );
    END LOOP;

    -- ============================================================
    -- DOSSIERS (300)
    -- ============================================================
    FOR i IN 1..300 LOOP
        v_agence_id := v_agence_ids[1 + floor(random() * array_length(v_agence_ids, 1))::int];
        v_client_id := v_client_ids[1 + floor(random() * array_length(v_client_ids, 1))::int];
        v_ot_id := v_ot_ids[1 + floor(random() * array_length(v_ot_ids, 1))::int];
        v_traffic_id := v_traffic_ids[1 + floor(random() * array_length(v_traffic_ids, 1))::int];
        v_phase_id := v_phase_ids[1 + floor(random() * array_length(v_phase_ids, 1))::int];
        v_pays_origine := v_pays_ids[1 + floor(random() * array_length(v_pays_ids, 1))::int];
        v_pays_destination := v_pays_ids[1 + floor(random() * array_length(v_pays_ids, 1))::int];
        v_inconterme_id := v_inconterme_ids[1 + floor(random() * array_length(v_inconterme_ids, 1))::int];

        INSERT INTO dossier (
            id_agence, id_utilisateur, id_client, numero_dossier, id_ordre_transit, id_traffic,
            est_groupage, id_phase, echeance, pour_compte, id_pays_origine, id_pays_destination,
            est_prepaye, est_provisionne, provision_demandee, lieu_paiement_fret,
            depart, date_livraison, adresse_livraison, id_inconterme, est_sous_dossier
        ) VALUES (
            v_agence_id,
            v_utilisateur_ids[1 + floor(random() * array_length(v_utilisateur_ids, 1))::int],
            v_client_id,
            'DOS-' || lpad(i::text, 7, '0'),
            v_ot_id,
            v_traffic_id,
            (random() < 0.4),
            v_phase_id,
            CURRENT_TIMESTAMP + ((i % 60) || ' days')::interval,
            'Pour compte de ' || i,
            v_pays_origine,
            v_pays_destination,
            (random() < 0.5),
            (random() < 0.5),
            (random() * 1000000)::numeric(10, 2),
            'Antananarivo',
            'Port/Aeroport ' || i,
            CURRENT_TIMESTAMP + (((i % 120) + 30) || ' days')::interval,
            'Adresse livraison ' || i,
            v_inconterme_id,
            (random() < 0.2)
        ) RETURNING id INTO v_dossier_id;

        v_dossier_ids := array_append(v_dossier_ids, v_dossier_id);
    END LOOP;

    -- ============================================================
    -- CONSTATS (50)
    -- ============================================================
    FOR i IN 1..50 LOOP
        INSERT INTO constat (id_dossier, id_expert_constat, id_type_constat, description)
        VALUES (
            v_dossier_ids[1 + floor(random() * array_length(v_dossier_ids, 1))::int],
            v_expert_constat_ids[1 + floor(random() * array_length(v_expert_constat_ids, 1))::int],
            v_type_constat_ids[1 + floor(random() * array_length(v_type_constat_ids, 1))::int],
            'Constat ' || i || ' : description du constat.'
        );
    END LOOP;

    -- ============================================================
    -- DOCUMENTS DE DOSSIER (600)
    -- ============================================================
    FOR i IN 1..600 LOOP
        INSERT INTO document_dossier (id_dossier, id_document, chemin_fichier, est_remis)
        VALUES (
            v_dossier_ids[1 + floor(random() * array_length(v_dossier_ids, 1))::int],
            v_type_document_ids[1 + floor(random() * array_length(v_type_document_ids, 1))::int],
            '/documents/doc_' || i || '.pdf',
            (random() < 0.7)
        );
    END LOOP;

    v_document_dossier_ids := ARRAY(SELECT id FROM document_dossier);

    -- ============================================================
    -- REMISES (100)
    -- ============================================================
    FOR i IN 1..100 LOOP
        INSERT INTO remise (id_dossier, id_type_document, date_remise)
        VALUES (
            v_dossier_ids[1 + floor(random() * array_length(v_dossier_ids, 1))::int],
            v_type_document_ids[1 + floor(random() * array_length(v_type_document_ids, 1))::int],
            CURRENT_TIMESTAMP - ((i % 30) || ' days')::interval
        );
    END LOOP;

    -- ============================================================
    -- EXPEDITIONS (250)
    -- ============================================================
    FOR i IN 1..250 LOOP
        v_dossier_id := v_dossier_ids[1 + floor(random() * array_length(v_dossier_ids, 1))::int];
        v_mode_acheminement_id := v_mode_acheminement_ids[1 + floor(random() * array_length(v_mode_acheminement_ids, 1))::int];

        INSERT INTO expedition (
            id_dossier, est_achemine, id_mode_acheminement,
            id_compagnie_maritime, numero_bl, id_navire, date_bl, lieu_bl,
            id_port_embarquement, id_port_debarquement,
            id_compagnie_aerienne, numero_vol, numero_lta, date_lta, lieu_lta,
            id_aeroport_embarquement, id_aeroport_debarquement,
            lib_scan, date_depart, date_prevu, date_arrivee
        ) VALUES (
            v_dossier_id,
            (random() < 0.8),
            v_mode_acheminement_id,
            CASE WHEN random() < 0.6 THEN v_compagnie_maritime_ids[1 + floor(random() * array_length(v_compagnie_maritime_ids, 1))::int] ELSE NULL END,
            CASE WHEN random() < 0.6 THEN 'BL-' || lpad(i::text, 6, '0') ELSE NULL END,
            CASE WHEN random() < 0.6 THEN v_navire_ids[1 + floor(random() * array_length(v_navire_ids, 1))::int] ELSE NULL END,
            CASE WHEN random() < 0.6 THEN CURRENT_TIMESTAMP - ((i % 20) || ' days')::interval ELSE NULL END,
            CASE WHEN random() < 0.6 THEN 'Port de Toamasina' ELSE NULL END,
            CASE WHEN random() < 0.6 THEN v_port_ids[1 + floor(random() * array_length(v_port_ids, 1))::int] ELSE NULL END,
            CASE WHEN random() < 0.6 THEN v_port_ids[1 + floor(random() * array_length(v_port_ids, 1))::int] ELSE NULL END,
            CASE WHEN random() < 0.4 THEN v_compagnie_aerienne_ids[1 + floor(random() * array_length(v_compagnie_aerienne_ids, 1))::int] ELSE NULL END,
            CASE WHEN random() < 0.4 THEN 'VOL-' || lpad(i::text, 6, '0') ELSE NULL END,
            CASE WHEN random() < 0.4 THEN 'LTA-' || lpad(i::text, 6, '0') ELSE NULL END,
            CASE WHEN random() < 0.4 THEN CURRENT_TIMESTAMP - ((i % 15) || ' days')::interval ELSE NULL END,
            CASE WHEN random() < 0.4 THEN 'Aeroport Ivato' ELSE NULL END,
            CASE WHEN random() < 0.4 THEN v_aeroport_ids[1 + floor(random() * array_length(v_aeroport_ids, 1))::int] ELSE NULL END,
            CASE WHEN random() < 0.4 THEN v_aeroport_ids[1 + floor(random() * array_length(v_aeroport_ids, 1))::int] ELSE NULL END,
            'Scan ' || i,
            CURRENT_TIMESTAMP - ((i % 20) || ' days')::interval,
            CURRENT_TIMESTAMP + ((i % 20) || ' days')::interval,
            CURRENT_TIMESTAMP + ((i % 25) || ' days')::interval
        ) RETURNING id INTO v_expedition_id;

        v_expedition_ids := array_append(v_expedition_ids, v_expedition_id);
    END LOOP;

    -- ============================================================
    -- MARCHANDISES (600)
    -- ============================================================
    FOR i IN 1..600 LOOP
        INSERT INTO marchandise (
            id_dossier, id_unite_mesure, id_categorie_marchandise,
            marque, modele, description, poids, volume, quantite
        ) VALUES (
            v_dossier_ids[1 + floor(random() * array_length(v_dossier_ids, 1))::int],
            v_unite_mesure_ids[1 + floor(random() * array_length(v_unite_mesure_ids, 1))::int],
            v_categorie_marchandise_ids[1 + floor(random() * array_length(v_categorie_marchandise_ids, 1))::int],
            'Marque' || (i % 20),
            'Modele' || (i % 30),
            'Description marchandise ' || i,
            (random() * 5000)::numeric(10, 2),
            (random() * 100)::numeric(10, 2),
            1 + floor(random() * 100)::int
        );
    END LOOP;

    -- ============================================================
    -- CONTENEURS (300)
    -- ============================================================
    FOR i IN 1..300 LOOP
        INSERT INTO conteneur (id_dossier, id_type_container, numero_container, description)
        VALUES (
            v_dossier_ids[1 + floor(random() * array_length(v_dossier_ids, 1))::int],
            v_type_container_ids[1 + floor(random() * array_length(v_type_container_ids, 1))::int],
            'CONT-' || lpad(i::text, 7, '0'),
            'Conteneur ' || i
        );
    END LOOP;

    -- ============================================================
    -- GROUPAGE (100)
    -- ============================================================
    FOR i IN 1..100 LOOP
        INSERT INTO groupage (id_dossier, id_expedition, numero_groupage, partie_notifier)
        VALUES (
            v_dossier_ids[1 + floor(random() * array_length(v_dossier_ids, 1))::int],
            v_expedition_ids[1 + floor(random() * array_length(v_expedition_ids, 1))::int],
            'GRP-' || lpad(i::text, 7, '0'),
            'Partie notifier ' || i
        );
    END LOOP;

    -- ============================================================
    -- DOSSIERS INTERAGENCE (40)
    -- ============================================================
    FOR i IN 1..40 LOOP
        v_dossier_mere := v_dossier_ids[1 + floor(random() * array_length(v_dossier_ids, 1))::int];
        v_sous_dossier := v_dossier_ids[1 + floor(random() * array_length(v_dossier_ids, 1))::int];

        INSERT INTO dossier_interagence (
            id_dossier_mere, id_sous_dossier, id_agence_mere, id_agence_sous,
            numero_dossier_mere, numero_sous_dossier, motif
        ) VALUES (
            v_dossier_mere,
            v_sous_dossier,
            (SELECT id_agence FROM dossier WHERE id = v_dossier_mere),
            (SELECT id_agence FROM dossier WHERE id = v_sous_dossier),
            (SELECT numero_dossier FROM dossier WHERE id = v_dossier_mere),
            (SELECT numero_dossier FROM dossier WHERE id = v_sous_dossier),
            'Motif interagence ' || i
        );
    END LOOP;

    -- ============================================================
    -- MOUVEMENTS DE DOSSIER (1000)
    -- ============================================================
    FOR i IN 1..1000 LOOP
        INSERT INTO mvt_dossier (
            id_dossier, id_utilisateur, ip_utilisateur, id_operation,
            id_document_dossier, commentaire
        ) VALUES (
            v_dossier_ids[1 + floor(random() * array_length(v_dossier_ids, 1))::int],
            v_utilisateur_ids[1 + floor(random() * array_length(v_utilisateur_ids, 1))::int],
            ('192.168.' || (i % 255) || '.' || (i % 255))::inet,
            v_operation_ids[1 + floor(random() * array_length(v_operation_ids, 1))::int],
            CASE WHEN random() < 0.3 THEN v_document_dossier_ids[1 + floor(random() * array_length(v_document_dossier_ids, 1))::int] ELSE NULL END,
            'Mouvement ' || i
        );
    END LOOP;

    -- ============================================================
    -- DECLARATIONS (200)
    -- ============================================================
    FOR i IN 1..200 LOOP
        INSERT INTO declaration (id_dossier, numero_declaration, date_declaration, id_bureau, description)
        VALUES (
            v_dossier_ids[1 + floor(random() * array_length(v_dossier_ids, 1))::int],
            'DEC-' || lpad(i::text, 8, '0'),
            CURRENT_TIMESTAMP - ((i % 30) || ' days')::interval,
            v_bureau_ids[1 + floor(random() * array_length(v_bureau_ids, 1))::int],
            'Declaration ' || i
        );
    END LOOP;

    -- ============================================================
    -- SUSPENSIONS DE CONNEXION (5)
    -- ============================================================
    FOR i IN 1..5 LOOP
        INSERT INTO suspension_connexion (id_utilisateur, tentatives_echouees, date_suspension, duree_suspension)
        VALUES (
            (SELECT id FROM utilisateur WHERE nom_utilisateur = 'user' || i),
            i,
            CURRENT_TIMESTAMP - ((i || ' days')::interval),
            15
        )
        ON CONFLICT (id_utilisateur) DO NOTHING;
    END LOOP;

END $$;

COMMIT;