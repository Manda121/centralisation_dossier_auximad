-- ============================================================
-- SEED DATA - dossier_auximad
-- Script de données de test volumineux et cohérent
-- Contexte : Transit / Douane / Logistique Madagascar (AUXIMAD)
-- Ordre respectant les contraintes de clés étrangères
-- ============================================================

BEGIN;

-- ============================================================
-- 1. TABLES DE RÉFÉRENCE SIMPLES
-- ============================================================

-- Agences
INSERT INTO agence (code, nom, adresse, ville, cp_client) VALUES
(100, 'AUXIMAD Antananarivo Siège', 'Lot II M 45 Bis Ambohijanahary', 'Antananarivo', '101'),
(200, 'AUXIMAD Toamasina Port', 'Boulevard Joffre, Port de Toamasina', 'Toamasina', '501'),
(300, 'AUXIMAD Ivato Aéroport', 'Aéroport International d''Ivato', 'Antananarivo', '105'),
(400, 'AUXIMAD Mahajanga', 'Avenue de France, Port de Mahajanga', 'Mahajanga', '401'),
(500, 'AUXIMAD Antsiranana', 'Quai Commercial, Port Diego', 'Antsiranana', '201'),
(600, 'AUXIMAD Toliara', 'Port de Toliara', 'Toliara', '601'),
(700, 'AUXIMAD Zone Franche Tanjombato', 'Zone Franche Industrielle Tanjombato', 'Antananarivo', '102'),
(800, 'AUXIMAD Nosy Be', 'Aéroport Fascene', 'Nosy Be', '207');

-- Rôles
INSERT INTO role (nom, description) VALUES
('Administrateur', 'Accès complet à toutes les fonctionnalités et paramétrages'),
('Directeur Agence', 'Gestion complète de son agence et supervision des dossiers'),
('Chef Transit', 'Supervision des opérations de transit et validation des phases'),
('Agent Transit', 'Saisie et suivi des dossiers de transit'),
('Agent Douane', 'Gestion des déclarations et formalités douanières'),
('Agent Documentaire', 'Gestion et contrôle des documents'),
('Comptable', 'Gestion des provisions, paiements et facturation'),
('Consultant', 'Accès en lecture seule pour consultation'),
('Superviseur Opérations', 'Validation des opérations critiques'),
('Administrateur Système', 'Gestion des utilisateurs et paramétrages techniques');

-- Types produit
INSERT INTO type_produit (nom, description) VALUES
('Marchandises générales', 'Produits divers non réglementés spécifiquement'),
('Produits alimentaires', 'Denrées alimentaires et produits périssables'),
('Produits pharmaceutiques', 'Médicaments et produits de santé'),
('Produits chimiques', 'Produits chimiques industriels et agricoles'),
('Véhicules et engins', 'Automobiles, engins de chantier, pièces détachées'),
('Matériel informatique', 'Ordinateurs, périphériques et composants'),
('Textile et habillement', 'Vêtements, tissus et accessoires'),
('Matériel de construction', 'Ciment, fer, matériaux de construction'),
('Produits pétroliers', 'Hydrocarbures et dérivés'),
('Produits agricoles', 'Café, vanille, litchis, épices, etc.'),
('Machines industrielles', 'Machines et équipements industriels'),
('Électroménager', 'Appareils électroménagers');

-- Modes de transport
INSERT INTO mode_transport (nom, description) VALUES
('Maritime', 'Transport par voie maritime'),
('Aérien', 'Transport par voie aérienne'),
('Routier', 'Transport par route'),
('Ferroviaire', 'Transport par chemin de fer'),
('Multimodal', 'Combinaison de plusieurs modes');

-- Types client
INSERT INTO type_client (nom, description) VALUES
('Importateur', 'Client importateur de marchandises'),
('Exportateur', 'Client exportateur de marchandises'),
('Transitaires', 'Société de transit tierce'),
('Zone Franche', 'Entreprise installée en zone franche'),
('Particulier', 'Client particulier'),
('ONG / Humanitaire', 'Organisation non gouvernementale ou humanitaire'),
('Administration', 'Entité publique ou administrative'),
('Transporteur', 'Société de transport');

-- Types régime
INSERT INTO type_regime (nom, code, description) VALUES
('Mise à la consommation', 'IM4', 'Régime d''importation définitive pour mise à la consommation'),
('Transit', 'T1', 'Régime de transit communautaire ou national'),
('Entrepôt sous douane', 'IM7', 'Mise en entrepôt sous douane'),
('Admission temporaire', 'IM5', 'Admission temporaire pour perfectionnement actif'),
('Exportation définitive', 'EX1', 'Exportation définitive de marchandises'),
('Réexportation', 'EX3', 'Réexportation de marchandises'),
('Zone Franche', 'ZF', 'Régime spécifique zone franche'),
('Perfectionnement actif', 'PA', 'Perfectionnement actif'),
('Perfectionnement passif', 'PP', 'Perfectionnement passif'),
('Cabotage', 'CAB', 'Cabotage national');

-- Types bureau
INSERT INTO type_bureau (nom, description) VALUES
('Bureau de port', 'Bureau douanier implanté dans un port'),
('Bureau d''aéroport', 'Bureau douanier implanté dans un aéroport'),
('Bureau intérieur', 'Bureau douanier intérieur (ville, zone franche)'),
('Bureau de frontière', 'Bureau douanier de frontière terrestre'),
('Bureau de zone franche', 'Bureau spécifique aux zones franches');

-- Bureaux
INSERT INTO bureau (nom, description, id_type_bureau) VALUES
('Bureau de Toamasina Port', 'Principal bureau portuaire de Madagascar', 1),
('Bureau d''Ivato Aéroport', 'Bureau de l''aéroport international d''Ivato', 2),
('Bureau d''Antanimena', 'Bureau intérieur principal d''Antananarivo', 3),
('Bureau Zone Franche Tanjombato', 'Bureau de la zone franche de Tanjombato', 5),
('Bureau de Mahajanga Port', 'Bureau du port de Mahajanga', 1),
('Bureau d''Antsiranana Port', 'Bureau du port d''Antsiranana (Diego)', 1),
('Bureau de Toliara Port', 'Bureau du port de Toliara', 1),
('Bureau de Fascene Nosy Be', 'Bureau de l''aéroport de Fascene', 2),
('Bureau de Soarano', 'Bureau intérieur Gare Soarano Antananarivo', 3),
('Bureau de Fort Dauphin', 'Bureau de Port d''Ehoala / Fort Dauphin', 1);

-- Modes d'acheminement
INSERT INTO mode_acheminement (nom, description) VALUES
('Conteneur FCL', 'Full Container Load - Conteneur complet'),
('Conteneur LCL', 'Less than Container Load - Groupage conteneur'),
('Vrac maritime', 'Marchandises en vrac par mer'),
('Fret aérien', 'Transport aérien standard'),
('Express aérien', 'Transport aérien express / courier'),
('Routier complet', 'Camion complet'),
('Routier groupage', 'Groupage routier'),
('Cabotage', 'Transport cabotage national');

-- Catégories marchandise
INSERT INTO nature_marchandise (nom, description) VALUES
('Produits alimentaires', 'Denrées alimentaires et boissons'),
('Produits pharmaceutiques', 'Médicaments et dispositifs médicaux'),
('Produits chimiques', 'Produits chimiques et engrais'),
('Machines et équipements', 'Machines industrielles et équipements'),
('Véhicules', 'Véhicules automobiles et engins'),
('Textile', 'Textiles, vêtements et chaussures'),
('Électronique', 'Matériel électronique et informatique'),
('Matériaux de construction', 'Ciment, fer, bois, etc.'),
('Produits agricoles', 'Produits agricoles bruts et transformés'),
('Produits pétroliers', 'Hydrocarbures et lubrifiants'),
('Meubles et ameublement', 'Meubles et articles d''ameublement'),
('Divers / Général', 'Marchandises diverses non classées');

-- Phases dossier
INSERT INTO phase_dossier (nom, description) VALUES
('Ouverture', 'Création et ouverture du dossier'),
('Réception documents', 'Réception et contrôle des documents'),
('Déclaration', 'Établissement de la déclaration en douane'),
('Contrôle douanier', 'Contrôle physique ou documentaire par la douane'),
('Liquidation', 'Liquidation des droits et taxes'),
('Paiement', 'Paiement des droits, taxes et prestations'),
('Enlèvement', 'Autorisation d''enlèvement des marchandises'),
('Livraison', 'Livraison au client final'),
('Clôture', 'Clôture administrative du dossier'),
('Contentieux', 'Dossier en contentieux ou litige'),
('Suspension', 'Dossier suspendu temporairement');

-- Traffics
INSERT INTO traffic (nom, code_traffic, description) VALUES
('Import Maritime', 'IMP-MAR', 'Importation par voie maritime'),
('Import Aérien', 'IMP-AIR', 'Importation par voie aérienne'),
('Export Maritime', 'EXP-MAR', 'Exportation par voie maritime'),
('Export Aérien', 'EXP-AIR', 'Exportation par voie aérienne'),
('Transit Maritime', 'TR-MAR', 'Transit par voie maritime'),
('Transit Aérien', 'TR-AIR', 'Transit par voie aérienne'),
('Cabotage', 'CAB', 'Cabotage national'),
('Zone Franche Import', 'ZF-IMP', 'Importation sous régime zone franche'),
('Zone Franche Export', 'ZF-EXP', 'Exportation sous régime zone franche');

-- Incoterms (inconterme)
INSERT INTO inconterme (nom, abreviation, description) VALUES
('Ex Works', 'EXW', 'Marchandises disponibles chez le vendeur'),
('Free Carrier', 'FCA', 'Remis au transporteur désigné'),
('Free Alongside Ship', 'FAS', 'Le long du navire au port d''embarquement'),
('Free On Board', 'FOB', 'À bord du navire au port d''embarquement'),
('Cost and Freight', 'CFR', 'Coût et fret jusqu''au port de destination'),
('Cost Insurance and Freight', 'CIF', 'Coût, assurance et fret jusqu''au port de destination'),
('Carriage Paid To', 'CPT', 'Port payé jusqu''au lieu de destination'),
('Carriage and Insurance Paid To', 'CIP', 'Port et assurance payés jusqu''au lieu de destination'),
('Delivered at Place', 'DAP', 'Rendu au lieu de destination'),
('Delivered at Place Unloaded', 'DPU', 'Rendu au lieu de destination déchargé'),
('Delivered Duty Paid', 'DDP', 'Rendu droits acquittés');

-- Types constat
INSERT INTO type_constat (nom, description) VALUES
('Constat d''avarie', 'Constat de dommages ou avaries sur marchandises'),
('Constat de manquant', 'Constat de quantités manquantes'),
('Constat de surplus', 'Constat de quantités excédentaires'),
('Constat d''ouverture', 'Constat d''ouverture de colis/conteneur'),
('Constat de non-conformité', 'Non-conformité qualitative ou documentaire'),
('Constat d''expertise', 'Expertise technique demandée'),
('Constat de livraison', 'Constat lors de la livraison');

-- Unités de mesure
INSERT INTO unite_mesure (nom, rapport_m_cube, description) VALUES
('Kilogramme', 0.001, 'Poids en kilogrammes'),
('Tonne', 1.000, 'Poids en tonnes métriques'),
('Mètre cube', 1.000, 'Volume en mètres cubes'),
('Litre', 0.001, 'Volume en litres'),
('Pièce', 0.000, 'Unité à la pièce'),
('Colis', 0.050, 'Colis standard approximatif'),
('Palette', 1.200, 'Palette standard'),
('Carton', 0.030, 'Carton standard'),
('Sac', 0.050, 'Sac (riz, ciment, etc.)'),
('Baril', 0.160, 'Baril standard 160 litres approx.');

-- Types container
INSERT INTO type_container (nom, description, volume, poids_max, longueur, largeur, hauteur) VALUES
('20'' Standard (DV)', 'Conteneur dry 20 pieds standard', 33.20, 21700.00, 5.90, 2.35, 2.39),
('20'' High Cube', 'Conteneur dry 20 pieds high cube', 37.40, 21700.00, 5.90, 2.35, 2.69),
('40'' Standard (DV)', 'Conteneur dry 40 pieds standard', 67.70, 26700.00, 12.03, 2.35, 2.39),
('40'' High Cube', 'Conteneur dry 40 pieds high cube', 76.30, 26500.00, 12.03, 2.35, 2.69),
('40'' Reefer', 'Conteneur frigorifique 40 pieds', 67.00, 26500.00, 11.55, 2.28, 2.25),
('20'' Open Top', 'Conteneur open top 20 pieds', 32.50, 21700.00, 5.90, 2.35, 2.35),
('40'' Open Top', 'Conteneur open top 40 pieds', 66.00, 26500.00, 12.03, 2.35, 2.35),
('20'' Flat Rack', 'Conteneur flat rack 20 pieds', 0.00, 30000.00, 5.90, 2.40, 2.20),
('40'' Flat Rack', 'Conteneur flat rack 40 pieds', 0.00, 40000.00, 12.05, 2.40, 2.20),
('45'' High Cube', 'Conteneur 45 pieds high cube', 86.00, 27500.00, 13.55, 2.35, 2.70);

-- Pays (principaux + partenaires Madagascar)
INSERT INTO pays (nom, code) VALUES
('Madagascar', 'MDG'),
('France', 'FRA'),
('Chine', 'CHN'),
('Inde', 'IND'),
('Afrique du Sud', 'ZAF'),
('Maurice', 'MUS'),
('Émirats Arabes Unis', 'ARE'),
('Singapour', 'SGP'),
('Allemagne', 'DEU'),
('Italie', 'ITA'),
('Espagne', 'ESP'),
('Belgique', 'BEL'),
('Pays-Bas', 'NLD'),
('Turquie', 'TUR'),
('Thaïlande', 'THA'),
('Malaisie', 'MYS'),
('Indonésie', 'IDN'),
('États-Unis', 'USA'),
('Royaume-Uni', 'GBR'),
('Japon', 'JPN'),
('Corée du Sud', 'KOR'),
('Brésil', 'BRA'),
('Kenya', 'KEN'),
('Tanzanie', 'TZA'),
('Comores', 'COM'),
('Seychelles', 'SYC'),
('Réunion', 'REU'),
('Égypte', 'EGY'),
('Maroc', 'MAR'),
('Vietnam', 'VNM');

-- ============================================================
-- 2. TABLES DÉPENDANTES NIVEAU 1
-- ============================================================

-- Utilisateurs (mots de passe fictifs hashés pour test - NE PAS UTILISER EN PROD)
-- Hash bcrypt exemple pour "Password123!" (à remplacer en production)
INSERT INTO utilisateur (nom_utilisateur, nom, prenom, email, mot_de_passe, id_role, id_agence) VALUES
('admin', 'RAKOTO', 'Jean', 'jean.rakoto@auximad.mg', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G2oQqGqKqKqKqK', 1, 1),
('dir.tnr', 'ANDRIAMAMPIONONA', 'Marie', 'marie.andria@auximad.mg', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G2oQqGqKqKqKqK', 2, 1),
('chef.tmm', 'RAHARISON', 'Paul', 'paul.raharison@auximad.mg', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G2oQqGqKqKqKqK', 3, 2),
('agent.tnr1', 'RABEMANANJARA', 'Sophie', 'sophie.rabe@auximad.mg', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G2oQqGqKqKqKqK', 4, 1),
('agent.tnr2', 'RANDRIANARISOA', 'Luc', 'luc.randria@auximad.mg', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G2oQqGqKqKqKqK', 4, 1),
('agent.tmm1', 'RAKOTONDRAZAKA', 'Hery', 'hery.rakoto@auximad.mg', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G2oQqGqKqKqKqK', 4, 2),
('agent.tmm2', 'RAZAFINDRAKOTO', 'Nirina', 'nirina.raza@auximad.mg', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G2oQqGqKqKqKqK', 4, 2),
('agent.ivato', 'ANDRIANASOLO', 'Fara', 'fara.andria@auximad.mg', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G2oQqGqKqKqKqK', 4, 3),
('douane.tnr', 'RABEARISON', 'Tojo', 'tojo.rabe@auximad.mg', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G2oQqGqKqKqKqK', 5, 1),
('douane.tmm', 'RASOLOMANANA', 'Voahangy', 'voahangy.rasolo@auximad.mg', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G2oQqGqKqKqKqK', 5, 2),
('doc.tnr', 'RAKOTOMALALA', 'Miora', 'miora.rakoto@auximad.mg', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G2oQqGqKqKqKqK', 6, 1),
('comptable1', 'ANDRIAMIHAMINA', 'Eric', 'eric.andria@auximad.mg', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G2oQqGqKqKqKqK', 7, 1),
('consultant1', 'RAZAFIMAHATRATRA', 'Lala', 'lala.raza@auximad.mg', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G2oQqGqKqKqKqK', 8, 1),
('super.ops', 'RAKOTOVAO', 'Haja', 'haja.rakoto@auximad.mg', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G2oQqGqKqKqKqK', 9, 1),
('sysadmin', 'ADMIN', 'Système', 'sysadmin@auximad.mg', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G2oQqGqKqKqKqK', 10, 1),
('agent.mjg', 'RAHARIMALALA', 'Patricia', 'patricia.rahari@auximad.mg', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G2oQqGqKqKqKqK', 4, 4),
('agent.diego', 'ANDRIANJAFY', 'Sitraka', 'sitraka.andria@auximad.mg', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G2oQqGqKqKqKqK', 4, 5),
('agent.tulear', 'RAKOTONDRABE', 'Nomena', 'nomena.rakoto@auximad.mg', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G2oQqGqKqKqKqK', 4, 6),
('agent.zf', 'RAZAFINDRAVAO', 'Tiana', 'tiana.raza@auximad.mg', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G2oQqGqKqKqKqK', 4, 7),
('agent.nosy', 'ANDRIAMAMPIONONA', 'Faly', 'faly.andria@auximad.mg', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G2oQqGqKqKqKqK', 4, 8),
('chef.ivato', 'RAKOTOSON', 'Mamisoa', 'mamisoa.rakoto@auximad.mg', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G2oQqGqKqKqKqK', 3, 3),
('agent.tnr3', 'RANDRIAMAMPIONONA', 'Hasina', 'hasina.randria@auximad.mg', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G2oQqGqKqKqKqK', 4, 1),
('agent.tnr4', 'RAHARISOA', 'Mamy', 'mamy.rahari@auximad.mg', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G2oQqGqKqKqKqK', 4, 1),
('agent.tmm3', 'ANDRIANARIVO', 'Lalao', 'lalao.andria@auximad.mg', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G2oQqGqKqKqKqK', 4, 2),
('comptable2', 'RAKOTOARISOA', 'Voahirana', 'voahirana.rakoto@auximad.mg', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G2oQqGqKqKqKqK', 7, 2);

-- Autorisations
INSERT INTO autorisation (id_utilisateur, niveau_autorisation) VALUES
(1, 10), (2, 9), (3, 8), (4, 5), (5, 5), (6, 5), (7, 5), (8, 5),
(9, 6), (10, 6), (11, 4), (12, 7), (13, 2), (14, 8), (15, 10),
(16, 5), (17, 5), (18, 5), (19, 5), (20, 5), (21, 8), (22, 5),
(23, 5), (24, 5), (25, 7);

-- Opérations dossier (liées aux phases)
INSERT INTO operation_dossier (description, id_phase, niveau_autorisation, niveau_validation, niveau_operation, est_obligatoire) VALUES
('Création du dossier', 1, 3, 5, 1, TRUE),
('Saisie des informations générales', 1, 3, 5, 2, TRUE),
('Réception des documents d''origine', 2, 3, 5, 1, TRUE),
('Contrôle de conformité documentaire', 2, 4, 6, 2, TRUE),
('Saisie de la déclaration en douane', 3, 5, 7, 1, TRUE),
('Validation de la déclaration', 3, 6, 8, 2, TRUE),
('Demande de contrôle physique', 4, 5, 7, 1, FALSE),
('Réalisation du contrôle douanier', 4, 6, 8, 2, FALSE),
('Liquidation des droits et taxes', 5, 6, 8, 1, TRUE),
('Émission du bulletin de liquidation', 5, 6, 8, 2, TRUE),
('Enregistrement du paiement', 6, 5, 7, 1, TRUE),
('Validation du paiement', 6, 7, 9, 2, TRUE),
('Autorisation d''enlèvement', 7, 6, 8, 1, TRUE),
('Émission du bon à enlever', 7, 6, 8, 2, TRUE),
('Organisation de la livraison', 8, 4, 6, 1, TRUE),
('Confirmation de livraison', 8, 4, 6, 2, TRUE),
('Clôture du dossier', 9, 5, 8, 1, TRUE),
('Archivage des documents', 9, 4, 6, 2, TRUE),
('Ouverture contentieux', 10, 7, 9, 1, FALSE),
('Suivi contentieux', 10, 7, 9, 2, FALSE),
('Suspension du dossier', 11, 6, 8, 1, FALSE),
('Reprise du dossier', 11, 6, 8, 2, FALSE);

-- Experts constat
INSERT INTO expert_constat (nom, prenom, email, telephone, adresse) VALUES
('RAKOTO', 'Jean-Claude', 'jc.rakoto@expert.mg', '+261320000001', 'Antananarivo'),
('ANDRIAMAMPIONONA', 'Sophie', 's.andria@expert.mg', '+261320000002', 'Toamasina'),
('RAHARISON', 'Michel', 'm.raharison@expert.mg', '+261320000003', 'Antananarivo'),
('RASOLOMANANA', 'Voahangy', 'v.rasolo@expert.mg', '+261320000004', 'Mahajanga'),
('RANDRIANARISOA', 'Patrick', 'p.randria@expert.mg', '+261320000005', 'Antananarivo'),
('RAKOTONDRAZAKA', 'Hery', 'h.rakoto@expert.mg', '+261320000006', 'Toamasina'),
('ANDRIANASOLO', 'Fara', 'f.andria@expert.mg', '+261320000007', 'Antananarivo'),
('RAZAFINDRAKOTO', 'Nirina', 'n.raza@expert.mg', '+261320000008', 'Antsiranana');

-- Compagnies maritimes
INSERT INTO compagnie_maritime (nom, code, id_pays) VALUES
('CMA CGM', 'CMA', 2),
('MSC Mediterranean Shipping', 'MSC', 11),
('Maersk Line', 'MAE', 13),
('PIL Pacific International Lines', 'PIL', 8),
('Hapag-Lloyd', 'HAP', 9),
('COSCO Shipping', 'COS', 3),
('Evergreen Marine', 'EVE', 20),
('ONE Ocean Network Express', 'ONE', 20),
('Yang Ming', 'YML', 20),
('ZIM Integrated Shipping', 'ZIM', 18),
('Unifeeder', 'UNI', 13),
('Swire Shipping', 'SWI', 19);

-- Compagnies aériennes
INSERT INTO compagnie_aerienne (nom, code, id_pays) VALUES
('Air Madagascar / Tsaradia', 'MD', 1),
('Air France', 'AF', 2),
('Emirates', 'EK', 7),
('Ethiopian Airlines', 'ET', 23),
('Kenya Airways', 'KQ', 23),
('Turkish Airlines', 'TK', 14),
('Qatar Airways', 'QR', 7),
('Air Mauritius', 'MK', 6),
('Corsair', 'SS', 2),
('Ewa Air', 'ZD', 27),
('Madagasikara Airways', 'MDG', 1);

-- Navires
INSERT INTO navire (nom, imo, id_compagnie_maritime) VALUES
('CMA CGM FORTUNE', 'IMO9123456', 1),
('MSC OSCAR', 'IMO9703318', 2),
('MAERSK SEALAND', 'IMO9195218', 3),
('PIL KOTA JAYA', 'IMO9345678', 4),
('HAPAG LLOYD EXPRESS', 'IMO9456789', 5),
('COSCO SHIPPING UNIVERSE', 'IMO9789653', 6),
('EVER GREEN', 'IMO9812345', 7),
('ONE COMMITMENT', 'IMO9823456', 8),
('YANG MING WELLNESS', 'IMO9834567', 9),
('ZIM ROTTERDAM', 'IMO9845678', 10),
('CMA CGM TANA', 'IMO9856789', 1),
('MSC MADAGASCAR', 'IMO9867890', 2),
('MAERSK TOAMASINA', 'IMO9878901', 3),
('PIL INDIAN OCEAN', 'IMO9889012', 4),
('CMA CGM DIEGO', 'IMO9890123', 1);

-- Stations (ports & aéroports)
INSERT INTO station (nom, code, lieu, id_mode_transport, description) VALUES
-- Ports
('Port de Toamasina', 'TMM', 'Toamasina', 1, 'Principal port commercial de Madagascar'),
('Port de Mahajanga', 'MJN', 'Mahajanga', 1, 'Port de la côte nord-ouest'),
('Port d''Antsiranana', 'DIE', 'Antsiranana', 1, 'Port de Diego-Suarez'),
('Port de Toliara', 'TLE', 'Toliara', 1, 'Port de la côte sud-ouest'),
('Port d''Ehoala', 'FTU', 'Taolagnaro', 1, 'Port de Fort Dauphin / Ehoala'),
('Port de Nosy Be', 'NOS', 'Nosy Be', 1, 'Port de Nosy Be'),
-- Aéroports
('Aéroport International d''Ivato', 'TNR', 'Antananarivo', 2, 'Principal aéroport international'),
('Aéroport de Fascene', 'NOS', 'Nosy Be', 2, 'Aéroport de Nosy Be'),
('Aéroport de Toamasina', 'TMM', 'Toamasina', 2, 'Aéroport de Toamasina'),
('Aéroport de Mahajanga', 'MJN', 'Mahajanga', 2, 'Aéroport de Mahajanga'),
('Aéroport d''Antsiranana', 'DIE', 'Antsiranana', 2, 'Aéroport de Diego'),
('Aéroport de Toliara', 'TLE', 'Toliara', 2, 'Aéroport de Toliara'),
-- Ports étrangers fréquents
('Port de Durban', 'DUR', 'Durban', 1, 'Port d''Afrique du Sud'),
('Port de Jebel Ali', 'JEA', 'Dubaï', 1, 'Port des EAU'),
('Port de Singapour', 'SIN', 'Singapour', 1, 'Port de Singapour'),
('Port de Shanghai', 'SHA', 'Shanghai', 1, 'Port de Chine'),
('Port de Nhava Sheva', 'NSA', 'Mumbai', 1, 'Port d''Inde'),
('Port du Havre', 'LEH', 'Le Havre', 1, 'Port de France'),
('Aéroport de Paris CDG', 'CDG', 'Paris', 2, 'Aéroport Charles de Gaulle'),
('Aéroport de Dubai', 'DXB', 'Dubaï', 2, 'Aéroport de Dubai'),
('Aéroport de Johannesburg', 'JNB', 'Johannesburg', 2, 'Aéroport O.R. Tambo'),
('Aéroport de Maurice', 'MRU', 'Port Louis', 2, 'Aéroport Sir Seewoosagur Ramgoolam');

-- Types document
INSERT INTO type_document (nom, id_type_produit, id_mode_transport, id_nature_marchandise, est_obligatoire, description) VALUES
('Connaissement (BL)', NULL, 1, NULL, TRUE, 'Bill of Lading - Connaissement maritime'),
('Lettre de transport aérien (LTA/AWB)', NULL, 2, NULL, TRUE, 'Air Waybill'),
('Facture commerciale', NULL, NULL, NULL, TRUE, 'Facture proforma ou définitive'),
('Liste de colisage (Packing List)', NULL, NULL, NULL, TRUE, 'Liste détaillée des colis'),
('Certificat d''origine', NULL, NULL, NULL, FALSE, 'Certificat d''origine des marchandises'),
('Certificat phytosanitaire', 2, NULL, 1, FALSE, 'Certificat pour produits végétaux'),
('Certificat sanitaire', 2, NULL, 1, FALSE, 'Certificat sanitaire pour produits animaux'),
('Licence d''importation', NULL, NULL, NULL, FALSE, 'Licence d''importation si requise'),
('Déclaration en douane', NULL, NULL, NULL, TRUE, 'Déclaration en douane (DAU)'),
('Assurance cargo', NULL, NULL, NULL, FALSE, 'Police d''assurance transport'),
('Certificat d''analyse', 3, NULL, 2, FALSE, 'Certificat d''analyse pour produits pharmaceutiques'),
('Document de transport multimodal', NULL, 5, NULL, FALSE, 'Document pour transport multimodal'),
('Bon de livraison', NULL, NULL, NULL, FALSE, 'Bon de livraison client'),
('Quittance de paiement', NULL, NULL, NULL, FALSE, 'Quittance des droits et taxes'),
('Autorisation d''enlèvement', NULL, NULL, NULL, FALSE, 'Bon à enlever / Mainlevée'),
('Certificat EUR.1', NULL, NULL, NULL, FALSE, 'Certificat de circulation EUR.1'),
('Facture de fret', NULL, NULL, NULL, FALSE, 'Facture du transporteur'),
('Manifeste de cargaison', NULL, 1, NULL, FALSE, 'Manifeste du navire ou de l''avion');

-- ============================================================
-- 3. CLIENTS (volume important)
-- ============================================================

INSERT INTO clients (cp_client, nom, prenom, email, telephone, adresse, id_type_client, nif, stat, fax) VALUES
-- Importateurs
('CLI-0001', 'SODIAM', 'Import', 'import@sodiam.mg', '+261320100001', 'Antananarivo', 1, 'NIF000000001', 'STAT000000001', NULL),
('CLI-0002', 'LEADERS PRICE', 'Madagascar', 'achats@leadersprice.mg', '+261320100002', 'Antananarivo', 1, 'NIF000000002', 'STAT000000002', NULL),
('CLI-0003', 'SCORE', 'Distribution', 'import@score.mg', '+261320100003', 'Antananarivo', 1, 'NIF000000003', 'STAT000000003', NULL),
('CLI-0004', 'SHOPRITE', 'Madagascar', 'procurement@shoprite.mg', '+261320100004', 'Antananarivo', 1, 'NIF000000004', 'STAT000000004', NULL),
('CLI-0005', 'COURTS', 'Madagascar', 'import@courts.mg', '+261320100005', 'Antananarivo', 1, 'NIF000000005', 'STAT000000005', NULL),
('CLI-0006', 'HOLCIM', 'Madagascar', 'logistique@holcim.mg', '+261320100006', 'Antananarivo', 1, 'NIF000000006', 'STAT000000006', NULL),
('CLI-0007', 'JIRAMA', 'Approvisionnement', 'appro@jirama.mg', '+261320100007', 'Antananarivo', 1, 'NIF000000007', 'STAT000000007', NULL),
('CLI-0008', 'TELMA', 'Madagascar', 'logistique@telma.mg', '+261320100008', 'Antananarivo', 1, 'NIF000000008', 'STAT000000008', NULL),
('CLI-0009', 'ORANGE', 'Madagascar', 'supply@orange.mg', '+261320100009', 'Antananarivo', 1, 'NIF000000009', 'STAT000000009', NULL),
('CLI-0010', 'AIRTEL', 'Madagascar', 'logistics@airtel.mg', '+261320100010', 'Antananarivo', 1, 'NIF000000010', 'STAT000000010', NULL),
-- Exportateurs
('CLI-0011', 'VANILLA', 'Export Company', 'export@vanilla.mg', '+261320100011', 'Sambava', 2, 'NIF000000011', 'STAT000000011', NULL),
('CLI-0012', 'SCAEM', 'Export', 'contact@scaem.mg', '+261320100012', 'Antananarivo', 2, 'NIF000000012', 'STAT000000012', NULL),
('CLI-0013', 'HENRI', 'Fraise', 'export@henrifraise.mg', '+261320100013', 'Antananarivo', 2, 'NIF000000013', 'STAT000000013', NULL),
('CLI-0014', 'RAMANANDRAIBE', 'Export', 'export@ramanandraibe.mg', '+261320100014', 'Antananarivo', 2, 'NIF000000014', 'STAT000000014', NULL),
('CLI-0015', 'SOMAPECHE', 'Export', 'export@somapeche.mg', '+261320100015', 'Mahajanga', 2, 'NIF000000015', 'STAT000000015', NULL),
-- Zone Franche
('CLI-0016', 'COTONA', 'Zone Franche', 'import@cotona.mg', '+261320100016', 'Tanjombato', 4, 'NIF000000016', 'STAT000000016', NULL),
('CLI-0017', 'SOCIETE TEXTILE', 'Tanjombato', 'logistique@stt.mg', '+261320100017', 'Tanjombato', 4, 'NIF000000017', 'STAT000000017', NULL),
('CLI-0018', 'MAGNARD', 'Madagascar', 'import@magnard.mg', '+261320100018', 'Tanjombato', 4, 'NIF000000018', 'STAT000000018', NULL),
('CLI-0019', 'EROSKI', 'ZF', 'contact@eroski.mg', '+261320100019', 'Tanjombato', 4, 'NIF000000019', 'STAT000000019', NULL),
('CLI-0020', 'FOURNIER', 'Zone Franche', 'import@fournier.mg', '+261320100020', 'Tanjombato', 4, 'NIF000000020', 'STAT000000020', NULL),
-- Autres
('CLI-0021', 'MSF', 'Logistique', 'log@msf.mg', '+261320100021', 'Antananarivo', 6, 'NIF000000021', 'STAT000000021', NULL),
('CLI-0022', 'CROIX ROUGE', 'Malagasy', 'logistique@crm.mg', '+261320100022', 'Antananarivo', 6, 'NIF000000022', 'STAT000000022', NULL),
('CLI-0023', 'MINISTERE', 'Sante Publique', 'appro@sante.gov.mg', '+261320100023', 'Antananarivo', 7, 'NIF000000023', 'STAT000000023', NULL),
('CLI-0024', 'SODIAT', 'Transport', 'transit@sodiat.mg', '+261320100024', 'Antananarivo', 3, 'NIF000000024', 'STAT000000024', NULL),
('CLI-0025', 'TRANS', 'Mada', 'ops@transmada.mg', '+261320100025', 'Toamasina', 8, 'NIF000000025', 'STAT000000025', NULL),
-- Clients supplémentaires pour volume
('CLI-0026', 'IMPORT', 'Plus', 'contact@importplus.mg', '+261320100026', 'Antananarivo', 1, 'NIF000000026', 'STAT000000026', NULL),
('CLI-0027', 'GLOBAL', 'Trade MG', 'ops@globaltrade.mg', '+261320100027', 'Antananarivo', 1, 'NIF000000027', 'STAT000000027', NULL),
('CLI-0028', 'OCEAN', 'Logistics', 'info@oceanlog.mg', '+261320100028', 'Toamasina', 3, 'NIF000000028', 'STAT000000028', NULL),
('CLI-0029', 'EAST', 'Africa Trading', 'trade@eastafrica.mg', '+261320100029', 'Antananarivo', 1, 'NIF000000029', 'STAT000000029', NULL),
('CLI-0030', 'INDIAN', 'Ocean Supply', 'supply@ios.mg', '+261320100030', 'Antananarivo', 1, 'NIF000000030', 'STAT000000030', NULL),
('CLI-0031', 'PHARMA', 'Mada', 'import@pharmamada.mg', '+261320100031', 'Antananarivo', 1, 'NIF000000031', 'STAT000000031', NULL),
('CLI-0032', 'AGRO', 'Export MG', 'export@agroexport.mg', '+261320100032', 'Antananarivo', 2, 'NIF000000032', 'STAT000000032', NULL),
('CLI-0033', 'TECH', 'Import Solutions', 'tech@importsol.mg', '+261320100033', 'Antananarivo', 1, 'NIF000000033', 'STAT000000033', NULL),
('CLI-0034', 'BUILD', 'Materials Co', 'achats@buildmat.mg', '+261320100034', 'Antananarivo', 1, 'NIF000000034', 'STAT000000034', NULL),
('CLI-0035', 'AUTO', 'Parts Madagascar', 'parts@autoparts.mg', '+261320100035', 'Antananarivo', 1, 'NIF000000035', 'STAT000000035', NULL),
('CLI-0036', 'FOOD', 'Import Distributors', 'food@foodimp.mg', '+261320100036', 'Antananarivo', 1, 'NIF000000036', 'STAT000000036', NULL),
('CLI-0037', 'CHEM', 'Industrial', 'chem@chemid.mg', '+261320100037', 'Antananarivo', 1, 'NIF000000037', 'STAT000000037', NULL),
('CLI-0038', 'FASHION', 'Textile Import', 'fashion@textile.mg', '+261320100038', 'Antananarivo', 1, 'NIF000000038', 'STAT000000038', NULL),
('CLI-0039', 'HOME', 'Appliance Store', 'home@appliance.mg', '+261320100039', 'Antananarivo', 1, 'NIF000000039', 'STAT000000039', NULL),
('CLI-0040', 'PAPER', 'Office Supplies', 'paper@office.mg', '+261320100040', 'Antananarivo', 1, 'NIF000000040', 'STAT000000040', NULL),
-- Expéditeurs / Destinataires étrangers (simulés comme clients)
('CLI-0041', 'SHANGHAI', 'Trading Co', 'export@shanghai-trading.cn', '+8613800000001', 'Shanghai, Chine', 2, 'NIFCN000000041', 'STATCN000000041', NULL),
('CLI-0042', 'DUBAI', 'General Trading', 'ops@dubai-gt.ae', '+971500000002', 'Dubaï, EAU', 2, 'NIFAE000000042', 'STATAE000000042', NULL),
('CLI-0043', 'DURBAN', 'Exporters Ltd', 'export@durban-exp.za', '+27820000003', 'Durban, Afrique du Sud', 2, 'NIFZA000000043', 'STATZA000000043', NULL),
('CLI-0044', 'PARIS', 'Export SAS', 'export@paris-export.fr', '+33100000004', 'Paris, France', 2, 'NIFFR000000044', 'STATFR000000044', NULL),
('CLI-0045', 'MUMBAI', 'Spices & More', 'export@mumbai-spices.in', '+91220000005', 'Mumbai, Inde', 2, 'NIFIN000000045', 'STATIN000000045', NULL),
('CLI-0046', 'SINGAPORE', 'Logistics Hub', 'hub@sg-logistics.sg', '+6590000006', 'Singapour', 2, 'NIFSG000000046', 'STATSG000000046', NULL),
('CLI-0047', 'ISTANBUL', 'Textile Export', 'export@istanbul-tex.tr', '+90530000007', 'Istanbul, Turquie', 2, 'NIFTR000000047', 'STATTR000000047', NULL),
('CLI-0048', 'BANGKOK', 'Agri Products', 'export@bangkok-agri.th', '+66800000008', 'Bangkok, Thaïlande', 2, 'NIFTH000000048', 'STATTH000000048', NULL),
('CLI-0049', 'HAMBURG', 'Machinery GmbH', 'export@hamburg-mach.de', '+49400000009', 'Hambourg, Allemagne', 2, 'NIFDE000000049', 'STATDE000000049', NULL),
('CLI-0050', 'ROTTERDAM', 'Chemicals BV', 'export@rotterdam-chem.nl', '+31100000010', 'Rotterdam, Pays-Bas', 2, 'NIFNL000000050', 'STATNL000000050', NULL);

-- ============================================================
-- 4. ORDRES DE TRANSIT + INSTRUCTIONS
-- ============================================================

INSERT INTO ordre_transit (numero_ordre_transit, id_agence, id_client, id_type_produit, id_mode_transport, numero_commande, id_utilisateur, id_type_regime, destination_finale, id_expediteur, id_destinataire, id_bureau_dedou, est_assure, id_destination_facture) VALUES
(10001, 2, 1, 1, 1, 'CMD-2025-001', 6, 1, 'Antananarivo - Entrepôt client', 41, 1, 1, TRUE, 1),
(10002, 2, 2, 2, 1, 'CMD-2025-002', 6, 1, 'Antananarivo - Magasin Leaders', 43, 2, 1, TRUE, 2),
(10003, 2, 3, 1, 1, 'CMD-2025-003', 7, 1, 'Antananarivo - Score', 41, 3, 1, FALSE, 3),
(10004, 2, 6, 8, 1, 'CMD-2025-004', 6, 1, 'Antananarivo - Chantier Holcim', 49, 6, 1, TRUE, 6),
(10005, 3, 8, 6, 2, 'CMD-2025-005', 8, 1, 'Antananarivo - Telma', 42, 8, 2, TRUE, 8),
(10006, 2, 16, 7, 1, 'CMD-2025-006', 7, 7, 'Zone Franche Tanjombato', 47, 16, 4, TRUE, 16),
(10007, 2, 11, 10, 1, 'CMD-2025-007', 6, 5, 'Export vers Europe', 11, 44, 1, TRUE, 11),
(10008, 1, 21, 3, 2, 'CMD-2025-008', 4, 1, 'Antananarivo - MSF', 44, 21, 2, TRUE, 21),
(10009, 2, 31, 3, 1, 'CMD-2025-009', 6, 1, 'Antananarivo - Pharma Mada', 50, 31, 1, TRUE, 31),
(10010, 2, 4, 1, 1, 'CMD-2025-010', 7, 1, 'Antananarivo - Shoprite', 43, 4, 1, FALSE, 4),
(10011, 2, 34, 8, 1, 'CMD-2025-011', 6, 1, 'Antananarivo - Build Materials', 41, 34, 1, TRUE, 34),
(10012, 3, 33, 6, 2, 'CMD-2025-012', 8, 1, 'Antananarivo - Tech Import', 42, 33, 2, TRUE, 33),
(10013, 2, 17, 7, 1, 'CMD-2025-013', 7, 7, 'Zone Franche Tanjombato', 47, 17, 4, TRUE, 17),
(10014, 2, 35, 5, 1, 'CMD-2025-014', 6, 1, 'Antananarivo - Auto Parts', 49, 35, 1, TRUE, 35),
(10015, 4, 15, 10, 1, 'CMD-2025-015', 16, 5, 'Export produits de la mer', 15, 43, 5, TRUE, 15),
(10016, 2, 36, 2, 1, 'CMD-2025-016', 6, 1, 'Antananarivo - Food Import', 45, 36, 1, TRUE, 36),
(10017, 2, 37, 4, 1, 'CMD-2025-017', 7, 1, 'Antananarivo - Chem Industrial', 50, 37, 1, TRUE, 37),
(10018, 2, 38, 7, 1, 'CMD-2025-018', 6, 1, 'Antananarivo - Fashion Textile', 47, 38, 1, FALSE, 38),
(10019, 3, 39, 12, 2, 'CMD-2025-019', 8, 1, 'Antananarivo - Home Appliance', 42, 39, 2, TRUE, 39),
(10020, 2, 7, 9, 1, 'CMD-2025-020', 6, 1, 'Antananarivo - JIRAMA', 42, 7, 1, TRUE, 7),
(10021, 2, 1, 1, 1, 'CMD-2025-021', 6, 2, 'Transit vers Antananarivo', 41, 1, 1, TRUE, 1),
(10022, 2, 26, 1, 1, 'CMD-2025-022', 7, 1, 'Antananarivo - Import Plus', 46, 26, 1, TRUE, 26),
(10023, 2, 27, 11, 1, 'CMD-2025-023', 6, 1, 'Antananarivo - Global Trade', 49, 27, 1, TRUE, 27),
(10024, 1, 23, 3, 2, 'CMD-2025-024', 4, 1, 'Antananarivo - Ministère Santé', 44, 23, 2, TRUE, 23),
(10025, 2, 5, 12, 1, 'CMD-2025-025', 7, 1, 'Antananarivo - Courts', 41, 5, 1, TRUE, 5),
(10026, 7, 18, 7, 1, 'CMD-2025-026', 19, 7, 'Zone Franche Tanjombato', 47, 18, 4, TRUE, 18),
(10027, 2, 29, 1, 1, 'CMD-2025-027', 6, 1, 'Antananarivo - East Africa', 43, 29, 1, FALSE, 29),
(10028, 2, 30, 1, 1, 'CMD-2025-028', 7, 1, 'Antananarivo - Indian Ocean Supply', 45, 30, 1, TRUE, 30),
(10029, 3, 9, 6, 2, 'CMD-2025-029', 8, 1, 'Antananarivo - Orange', 42, 9, 2, TRUE, 9),
(10030, 2, 40, 1, 1, 'CMD-2025-030', 6, 1, 'Antananarivo - Paper Office', 41, 40, 1, TRUE, 40);

-- Instructions
INSERT INTO instruction (id_ordre_transit, instruction) VALUES
(1, 'Livraison urgente - priorité haute. Contacter le client dès arrivée au port.'),
(2, 'Marchandises alimentaires - respect de la chaîne du froid si applicable.'),
(3, 'Documents originaux à remettre au client contre signature.'),
(4, 'Matériel de construction - livraison sur chantier, coordination avec Holcim.'),
(5, 'Matériel informatique - emballage fragile, manipulation soignée.'),
(6, 'Régime zone franche - formalités ZF obligatoires.'),
(7, 'Export vanille - certificats phytosanitaires et d''origine à vérifier.'),
(8, 'Marchandises humanitaires - priorité et exonérations éventuelles.'),
(9, 'Produits pharmaceutiques - certificats d''analyse et autorisations requises.'),
(10, 'Groupage possible avec autres commandes Shoprite.'),
(11, 'Ciment et fer - contrôle de poids et volume important.'),
(12, 'Matériel informatique haute valeur - assurance obligatoire.'),
(13, 'Textile zone franche - documents d''origine et de fabrication.'),
(14, 'Pièces automobiles - vérifier numéros de série si applicable.'),
(15, 'Produits de la mer - exportation, chaîne du froid critique.'),
(16, 'Produits alimentaires - dates de péremption à contrôler.'),
(17, 'Produits chimiques - fiches de sécurité (SDS) obligatoires.'),
(18, 'Textile - contrôle qualité à l''arrivée recommandé.'),
(19, 'Électroménager - vérification de l''état des emballages.'),
(20, 'Produits pétroliers - réglementations spécifiques à respecter.');

-- ============================================================
-- 5. DOSSIERS (volume important pour tests)
-- ============================================================

INSERT INTO dossier (
    id_agence, id_utilisateur, id_client, numero_dossier, id_ordre_transit, id_traffic,
    est_groupage, id_phase, echeance, pour_compte,
    id_pays_origine, id_pays_destination, est_prepaye, est_provisionne,
    provision_demandee, lieu_paiement_fret, depart, date_livraison, adresse_livraison,
    id_inconterme, est_sous_dossier
) VALUES
-- Dossiers import maritime Toamasina
(2, 6, 1, 'DOS-2025-TMM-0001', 1, 1, FALSE, 5, '2025-03-15 00:00:00', 'SODIAM', 3, 1, FALSE, TRUE, 2500000.00, 'Toamasina', 'Shanghai', '2025-03-20 00:00:00', 'Entrepôt SODIAM Antananarivo', 6, FALSE),
(2, 6, 2, 'DOS-2025-TMM-0002', 2, 1, FALSE, 4, '2025-03-18 00:00:00', 'LEADERS PRICE', 5, 1, FALSE, TRUE, 1800000.00, 'Toamasina', 'Durban', '2025-03-25 00:00:00', 'Magasin Leaders Antananarivo', 5, FALSE),
(2, 7, 3, 'DOS-2025-TMM-0003', 3, 1, TRUE, 3, '2025-03-20 00:00:00', 'SCORE', 3, 1, FALSE, FALSE, 0.00, 'Toamasina', 'Shanghai', '2025-03-28 00:00:00', 'Score Antananarivo', 4, FALSE),
(2, 6, 6, 'DOS-2025-TMM-0004', 4, 1, FALSE, 6, '2025-03-10 00:00:00', 'HOLCIM', 9, 1, TRUE, TRUE, 5000000.00, 'Toamasina', 'Hambourg', '2025-03-15 00:00:00', 'Chantier Holcim', 6, FALSE),
(2, 7, 4, 'DOS-2025-TMM-0005', 10, 1, FALSE, 7, '2025-03-12 00:00:00', 'SHOPRITE', 5, 1, FALSE, TRUE, 1200000.00, 'Toamasina', 'Durban', '2025-03-18 00:00:00', 'Shoprite Antananarivo', 5, FALSE),
(2, 6, 16, 'DOS-2025-TMM-0006', 6, 8, FALSE, 2, '2025-03-25 00:00:00', 'COTONA ZF', 14, 1, FALSE, TRUE, 800000.00, 'Toamasina', 'Istanbul', '2025-04-01 00:00:00', 'Zone Franche Tanjombato', 4, FALSE),
(2, 6, 31, 'DOS-2025-TMM-0007', 9, 1, FALSE, 5, '2025-03-22 00:00:00', 'PHARMA MADA', 13, 1, FALSE, TRUE, 3500000.00, 'Toamasina', 'Rotterdam', '2025-03-30 00:00:00', 'Pharma Mada Antananarivo', 6, FALSE),
(2, 7, 34, 'DOS-2025-TMM-0008', 11, 1, FALSE, 4, '2025-03-28 00:00:00', 'BUILD MATERIALS', 3, 1, FALSE, TRUE, 2200000.00, 'Toamasina', 'Shanghai', '2025-04-05 00:00:00', 'Build Materials Antananarivo', 5, FALSE),
(2, 6, 35, 'DOS-2025-TMM-0009', 14, 1, FALSE, 3, '2025-04-02 00:00:00', 'AUTO PARTS', 9, 1, FALSE, FALSE, 0.00, 'Toamasina', 'Hambourg', '2025-04-10 00:00:00', 'Auto Parts Antananarivo', 4, FALSE),
(2, 7, 36, 'DOS-2025-TMM-0010', 16, 1, FALSE, 2, '2025-04-05 00:00:00', 'FOOD IMPORT', 4, 1, FALSE, TRUE, 950000.00, 'Toamasina', 'Mumbai', '2025-04-12 00:00:00', 'Food Import Antananarivo', 5, FALSE),
-- Dossiers aériens Ivato
(3, 8, 8, 'DOS-2025-TNR-0001', 5, 2, FALSE, 6, '2025-03-08 00:00:00', 'TELMA', 7, 1, TRUE, TRUE, 1500000.00, 'Ivato', 'Dubai', '2025-03-10 00:00:00', 'Telma Antananarivo', 8, FALSE),
(3, 8, 21, 'DOS-2025-TNR-0002', 8, 2, FALSE, 8, '2025-03-05 00:00:00', 'MSF', 2, 1, TRUE, TRUE, 800000.00, 'Ivato', 'Paris CDG', '2025-03-07 00:00:00', 'MSF Antananarivo', 9, FALSE),
(3, 8, 33, 'DOS-2025-TNR-0003', 12, 2, FALSE, 5, '2025-03-15 00:00:00', 'TECH IMPORT', 7, 1, FALSE, TRUE, 2100000.00, 'Ivato', 'Dubai', '2025-03-18 00:00:00', 'Tech Import Antananarivo', 8, FALSE),
(3, 21, 39, 'DOS-2025-TNR-0004', 19, 2, FALSE, 4, '2025-03-20 00:00:00', 'HOME APPLIANCE', 7, 1, FALSE, TRUE, 1100000.00, 'Ivato', 'Dubai', '2025-03-25 00:00:00', 'Home Appliance Antananarivo', 7, FALSE),
(3, 8, 23, 'DOS-2025-TNR-0005', 24, 2, FALSE, 7, '2025-03-12 00:00:00', 'MINISTERE SANTE', 2, 1, TRUE, TRUE, 3000000.00, 'Ivato', 'Paris CDG', '2025-03-14 00:00:00', 'Ministère Santé', 9, FALSE),
(3, 8, 9, 'DOS-2025-TNR-0006', 29, 2, FALSE, 3, '2025-03-25 00:00:00', 'ORANGE', 7, 1, FALSE, FALSE, 0.00, 'Ivato', 'Dubai', '2025-03-30 00:00:00', 'Orange Antananarivo', 8, FALSE),
-- Dossiers export
(2, 6, 11, 'DOS-2025-TMM-0011', 7, 3, FALSE, 5, '2025-03-30 00:00:00', 'VANILLA EXPORT', 1, 2, FALSE, TRUE, 500000.00, 'Toamasina', 'Toamasina', '2025-04-10 00:00:00', 'Port Le Havre', 4, FALSE),
(4, 16, 15, 'DOS-2025-MJN-0001', 15, 3, FALSE, 4, '2025-04-01 00:00:00', 'SOMAPECHE', 1, 5, FALSE, TRUE, 700000.00, 'Mahajanga', 'Mahajanga', '2025-04-15 00:00:00', 'Port Durban', 5, FALSE),
-- Dossiers zone franche
(7, 19, 18, 'DOS-2025-ZF-0001', 26, 8, FALSE, 2, '2025-04-08 00:00:00', 'MAGNARD ZF', 14, 1, FALSE, TRUE, 600000.00, 'Toamasina', 'Istanbul', '2025-04-15 00:00:00', 'Zone Franche Tanjombato', 4, FALSE),
(2, 7, 17, 'DOS-2025-TMM-0012', 13, 8, FALSE, 3, '2025-04-10 00:00:00', 'SOCIETE TEXTILE ZF', 14, 1, FALSE, TRUE, 450000.00, 'Toamasina', 'Istanbul', '2025-04-18 00:00:00', 'Zone Franche Tanjombato', 4, FALSE),
-- Autres dossiers pour volume
(2, 6, 26, 'DOS-2025-TMM-0013', 22, 1, FALSE, 1, '2025-04-15 00:00:00', 'IMPORT PLUS', 8, 1, FALSE, FALSE, 0.00, 'Toamasina', 'Singapour', '2025-04-25 00:00:00', 'Import Plus Antananarivo', 6, FALSE),
(2, 7, 27, 'DOS-2025-TMM-0014', 23, 1, FALSE, 2, '2025-04-18 00:00:00', 'GLOBAL TRADE', 9, 1, FALSE, TRUE, 2800000.00, 'Toamasina', 'Hambourg', '2025-04-28 00:00:00', 'Global Trade Antananarivo', 5, FALSE),
(2, 6, 29, 'DOS-2025-TMM-0015', 27, 1, TRUE, 3, '2025-04-20 00:00:00', 'EAST AFRICA', 5, 1, FALSE, TRUE, 900000.00, 'Toamasina', 'Durban', '2025-04-30 00:00:00', 'East Africa Antananarivo', 5, FALSE),
(2, 7, 30, 'DOS-2025-TMM-0016', 28, 1, FALSE, 4, '2025-04-22 00:00:00', 'INDIAN OCEAN SUPPLY', 4, 1, FALSE, TRUE, 1600000.00, 'Toamasina', 'Mumbai', '2025-05-02 00:00:00', 'IOS Antananarivo', 6, FALSE),
(2, 6, 37, 'DOS-2025-TMM-0017', 17, 1, FALSE, 5, '2025-04-25 00:00:00', 'CHEM INDUSTRIAL', 13, 1, FALSE, TRUE, 3200000.00, 'Toamasina', 'Rotterdam', '2025-05-05 00:00:00', 'Chem Industrial Antananarivo', 6, FALSE),
(2, 7, 38, 'DOS-2025-TMM-0018', 18, 1, FALSE, 2, '2025-04-28 00:00:00', 'FASHION TEXTILE', 14, 1, FALSE, FALSE, 0.00, 'Toamasina', 'Istanbul', '2025-05-08 00:00:00', 'Fashion Textile Antananarivo', 4, FALSE),
(2, 6, 5, 'DOS-2025-TMM-0019', 25, 1, FALSE, 6, '2025-03-28 00:00:00', 'COURTS', 3, 1, FALSE, TRUE, 1400000.00, 'Toamasina', 'Shanghai', '2025-04-05 00:00:00', 'Courts Antananarivo', 5, FALSE),
(2, 7, 40, 'DOS-2025-TMM-0020', 30, 1, FALSE, 3, '2025-05-01 00:00:00', 'PAPER OFFICE', 3, 1, FALSE, TRUE, 750000.00, 'Toamasina', 'Shanghai', '2025-05-10 00:00:00', 'Paper Office Antananarivo', 5, FALSE),
(2, 6, 7, 'DOS-2025-TMM-0021', 20, 1, FALSE, 4, '2025-05-05 00:00:00', 'JIRAMA', 7, 1, FALSE, TRUE, 4500000.00, 'Toamasina', 'Dubai', '2025-05-15 00:00:00', 'JIRAMA Antananarivo', 6, FALSE),
(1, 4, 1, 'DOS-2025-TNR-0007', 21, 5, FALSE, 2, '2025-05-08 00:00:00', 'SODIAM TRANSIT', 3, 1, FALSE, TRUE, 1100000.00, 'Toamasina', 'Shanghai', '2025-05-18 00:00:00', 'SODIAM Antananarivo', 5, FALSE),
-- Dossiers supplémentaires pour tests de volume
(2, 6, 1, 'DOS-2025-TMM-0022', NULL, 1, FALSE, 1, '2025-05-10 00:00:00', 'SODIAM', 3, 1, FALSE, FALSE, 0.00, 'Toamasina', 'Shanghai', '2025-05-20 00:00:00', 'Entrepôt SODIAM', 6, FALSE),
(2, 7, 2, 'DOS-2025-TMM-0023', NULL, 1, FALSE, 1, '2025-05-12 00:00:00', 'LEADERS PRICE', 5, 1, FALSE, FALSE, 0.00, 'Toamasina', 'Durban', '2025-05-22 00:00:00', 'Leaders Antananarivo', 5, FALSE),
(2, 6, 3, 'DOS-2025-TMM-0024', NULL, 1, TRUE, 2, '2025-05-15 00:00:00', 'SCORE', 3, 1, FALSE, TRUE, 600000.00, 'Toamasina', 'Shanghai', '2025-05-25 00:00:00', 'Score Antananarivo', 4, FALSE),
(3, 8, 8, 'DOS-2025-TNR-0008', NULL, 2, FALSE, 1, '2025-05-18 00:00:00', 'TELMA', 7, 1, FALSE, FALSE, 0.00, 'Ivato', 'Dubai', '2025-05-20 00:00:00', 'Telma Antananarivo', 8, FALSE),
(2, 6, 16, 'DOS-2025-TMM-0025', NULL, 8, FALSE, 1, '2025-05-20 00:00:00', 'COTONA ZF', 14, 1, FALSE, FALSE, 0.00, 'Toamasina', 'Istanbul', '2025-05-30 00:00:00', 'ZF Tanjombato', 4, FALSE),
(2, 7, 31, 'DOS-2025-TMM-0026', NULL, 1, FALSE, 2, '2025-05-22 00:00:00', 'PHARMA MADA', 13, 1, FALSE, TRUE, 2000000.00, 'Toamasina', 'Rotterdam', '2025-06-01 00:00:00', 'Pharma Mada', 6, FALSE),
(2, 6, 34, 'DOS-2025-TMM-0027', NULL, 1, FALSE, 3, '2025-05-25 00:00:00', 'BUILD MATERIALS', 3, 1, FALSE, TRUE, 1800000.00, 'Toamasina', 'Shanghai', '2025-06-05 00:00:00', 'Build Materials', 5, FALSE),
(3, 8, 21, 'DOS-2025-TNR-0009', NULL, 2, FALSE, 5, '2025-05-10 00:00:00', 'MSF', 2, 1, TRUE, TRUE, 950000.00, 'Ivato', 'Paris CDG', '2025-05-12 00:00:00', 'MSF Antananarivo', 9, FALSE),
(2, 7, 11, 'DOS-2025-TMM-0028', NULL, 3, FALSE, 3, '2025-05-28 00:00:00', 'VANILLA EXPORT', 1, 2, FALSE, TRUE, 400000.00, 'Toamasina', 'Toamasina', '2025-06-10 00:00:00', 'Port Le Havre', 4, FALSE),
(2, 6, 36, 'DOS-2025-TMM-0029', NULL, 1, FALSE, 4, '2025-06-01 00:00:00', 'FOOD IMPORT', 4, 1, FALSE, TRUE, 1100000.00, 'Toamasina', 'Mumbai', '2025-06-12 00:00:00', 'Food Import', 5, FALSE),
(2, 7, 37, 'DOS-2025-TMM-0030', NULL, 1, FALSE, 1, '2025-06-05 00:00:00', 'CHEM INDUSTRIAL', 13, 1, FALSE, FALSE, 0.00, 'Toamasina', 'Rotterdam', '2025-06-15 00:00:00', 'Chem Industrial', 6, FALSE),
(2, 6, 38, 'DOS-2025-TMM-0031', NULL, 1, FALSE, 2, '2025-06-08 00:00:00', 'FASHION TEXTILE', 14, 1, FALSE, TRUE, 550000.00, 'Toamasina', 'Istanbul', '2025-06-18 00:00:00', 'Fashion Textile', 4, FALSE),
(2, 7, 5, 'DOS-2025-TMM-0032', NULL, 1, FALSE, 5, '2025-06-10 00:00:00', 'COURTS', 3, 1, FALSE, TRUE, 1300000.00, 'Toamasina', 'Shanghai', '2025-06-20 00:00:00', 'Courts Antananarivo', 5, FALSE),
(2, 6, 40, 'DOS-2025-TMM-0033', NULL, 1, FALSE, 3, '2025-06-12 00:00:00', 'PAPER OFFICE', 3, 1, FALSE, TRUE, 680000.00, 'Toamasina', 'Shanghai', '2025-06-22 00:00:00', 'Paper Office', 5, FALSE),
(2, 7, 7, 'DOS-2025-TMM-0034', NULL, 1, FALSE, 4, '2025-06-15 00:00:00', 'JIRAMA', 7, 1, FALSE, TRUE, 3800000.00, 'Toamasina', 'Dubai', '2025-06-25 00:00:00', 'JIRAMA', 6, FALSE),
(3, 8, 33, 'DOS-2025-TNR-0010', NULL, 2, FALSE, 2, '2025-06-18 00:00:00', 'TECH IMPORT', 7, 1, FALSE, TRUE, 1750000.00, 'Ivato', 'Dubai', '2025-06-20 00:00:00', 'Tech Import', 8, FALSE),
(2, 6, 26, 'DOS-2025-TMM-0035', NULL, 1, FALSE, 1, '2025-06-20 00:00:00', 'IMPORT PLUS', 8, 1, FALSE, FALSE, 0.00, 'Toamasina', 'Singapour', '2025-06-30 00:00:00', 'Import Plus', 6, FALSE),
(2, 7, 27, 'DOS-2025-TMM-0036', NULL, 1, FALSE, 2, '2025-06-22 00:00:00', 'GLOBAL TRADE', 9, 1, FALSE, TRUE, 2500000.00, 'Toamasina', 'Hambourg', '2025-07-02 00:00:00', 'Global Trade', 5, FALSE),
(2, 6, 29, 'DOS-2025-TMM-0037', NULL, 1, TRUE, 3, '2025-06-25 00:00:00', 'EAST AFRICA', 5, 1, FALSE, TRUE, 850000.00, 'Toamasina', 'Durban', '2025-07-05 00:00:00', 'East Africa', 5, FALSE),
(2, 7, 30, 'DOS-2025-TMM-0038', NULL, 1, FALSE, 4, '2025-06-28 00:00:00', 'INDIAN OCEAN SUPPLY', 4, 1, FALSE, TRUE, 1450000.00, 'Toamasina', 'Mumbai', '2025-07-08 00:00:00', 'IOS', 6, FALSE),
(2, 6, 1, 'DOS-2025-TMM-0039', NULL, 1, FALSE, 5, '2025-07-01 00:00:00', 'SODIAM', 3, 1, FALSE, TRUE, 1900000.00, 'Toamasina', 'Shanghai', '2025-07-12 00:00:00', 'SODIAM', 6, FALSE),
(2, 7, 2, 'DOS-2025-TMM-0040', NULL, 1, FALSE, 6, '2025-07-03 00:00:00', 'LEADERS PRICE', 5, 1, FALSE, TRUE, 1650000.00, 'Toamasina', 'Durban', '2025-07-15 00:00:00', 'Leaders', 5, FALSE);

-- ============================================================
-- 6. EXPÉDITIONS
-- ============================================================

INSERT INTO expedition (
    id_dossier, est_achemine, id_mode_acheminement,
    id_compagnie_maritime, numero_bl, id_navire, date_bl, lieu_bl,
    id_port_embarquement, id_port_debarquement,
    id_compagnie_aerienne, numero_vol, numero_lta, date_lta, lieu_lta,
    id_aeroport_embarquement, id_aeroport_debarquement,
    lib_scan, date_depart, date_prevu, date_arrivee
) VALUES
-- Maritimes
(1, TRUE, 1, 1, 'CMAU1234567890', 1, '2025-02-10 00:00:00', 'Shanghai', 16, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SCAN-TMM-001', '2025-02-12 00:00:00', '2025-03-05 00:00:00', '2025-03-04 00:00:00'),
(2, TRUE, 1, 4, 'PIL9876543210', 4, '2025-02-15 00:00:00', 'Durban', 13, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SCAN-TMM-002', '2025-02-17 00:00:00', '2025-02-24 00:00:00', '2025-02-23 00:00:00'),
(3, TRUE, 2, 1, 'CMAU2345678901', 11, '2025-02-18 00:00:00', 'Shanghai', 16, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SCAN-TMM-003', '2025-02-20 00:00:00', '2025-03-12 00:00:00', '2025-03-11 00:00:00'),
(4, TRUE, 1, 5, 'HLCU3456789012', 5, '2025-02-05 00:00:00', 'Hambourg', 18, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SCAN-TMM-004', '2025-02-08 00:00:00', '2025-03-01 00:00:00', '2025-02-28 00:00:00'),
(5, TRUE, 1, 4, 'PIL8765432109', 14, '2025-02-12 00:00:00', 'Durban', 13, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SCAN-TMM-005', '2025-02-14 00:00:00', '2025-02-21 00:00:00', '2025-02-20 00:00:00'),
(6, TRUE, 1, 1, 'CMAU3456789012', 1, '2025-02-25 00:00:00', 'Istanbul via Singapour', 15, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SCAN-TMM-006', '2025-02-28 00:00:00', '2025-03-20 00:00:00', NULL),
(7, TRUE, 1, 10, 'ZIMU4567890123', 10, '2025-02-20 00:00:00', 'Rotterdam', 18, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SCAN-TMM-007', '2025-02-22 00:00:00', '2025-03-15 00:00:00', '2025-03-14 00:00:00'),
(8, TRUE, 1, 6, 'COSU5678901234', 6, '2025-03-01 00:00:00', 'Shanghai', 16, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SCAN-TMM-008', '2025-03-03 00:00:00', '2025-03-25 00:00:00', NULL),
(9, FALSE, 1, 5, 'HLCU6789012345', 5, '2025-03-05 00:00:00', 'Hambourg', 18, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2025-03-08 00:00:00', '2025-03-28 00:00:00', NULL),
(10, TRUE, 1, 4, 'PIL7654321098', 4, '2025-03-08 00:00:00', 'Mumbai', 17, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SCAN-TMM-010', '2025-03-10 00:00:00', '2025-03-28 00:00:00', NULL),
-- Aériens
(11, TRUE, 4, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 3, 'EK0721', 'EK12345678', '2025-03-01 00:00:00', 'Dubai', 20, 7, 'SCAN-TNR-001', '2025-03-02 00:00:00', '2025-03-03 00:00:00', '2025-03-03 00:00:00'),
(12, TRUE, 5, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 2, 'AF0942', 'AF87654321', '2025-02-28 00:00:00', 'Paris CDG', 19, 7, 'SCAN-TNR-002', '2025-03-01 00:00:00', '2025-03-02 00:00:00', '2025-03-02 00:00:00'),
(13, TRUE, 4, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 3, 'EK0723', 'EK23456789', '2025-03-10 00:00:00', 'Dubai', 20, 7, 'SCAN-TNR-003', '2025-03-11 00:00:00', '2025-03-12 00:00:00', '2025-03-12 00:00:00'),
(14, TRUE, 4, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 3, 'EK0725', 'EK34567890', '2025-03-15 00:00:00', 'Dubai', 20, 7, 'SCAN-TNR-004', '2025-03-16 00:00:00', '2025-03-17 00:00:00', NULL),
(15, TRUE, 5, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 2, 'AF0944', 'AF98765432', '2025-03-08 00:00:00', 'Paris CDG', 19, 7, 'SCAN-TNR-005', '2025-03-09 00:00:00', '2025-03-10 00:00:00', '2025-03-10 00:00:00'),
(16, FALSE, 4, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 3, 'EK0727', 'EK45678901', '2025-03-20 00:00:00', 'Dubai', 20, 7, NULL, '2025-03-21 00:00:00', '2025-03-22 00:00:00', NULL),
-- Export
(17, TRUE, 1, 1, 'CMAU7890123456', 11, '2025-03-15 00:00:00', 'Toamasina', 1, 18, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SCAN-EXP-001', '2025-03-18 00:00:00', '2025-04-05 00:00:00', NULL),
(18, TRUE, 1, 4, 'PIL6543210987', 14, '2025-03-20 00:00:00', 'Mahajanga', 2, 13, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SCAN-EXP-002', '2025-03-22 00:00:00', '2025-04-01 00:00:00', NULL),
-- Autres
(19, TRUE, 1, 1, 'CMAU8901234567', 1, '2025-03-22 00:00:00', 'Istanbul via Singapour', 15, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SCAN-ZF-001', '2025-03-25 00:00:00', '2025-04-12 00:00:00', NULL),
(20, TRUE, 1, 1, 'CMAU9012345678', 11, '2025-03-25 00:00:00', 'Istanbul via Singapour', 15, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SCAN-ZF-002', '2025-03-28 00:00:00', '2025-04-15 00:00:00', NULL),
(21, FALSE, 1, 6, 'COSU0123456789', 6, '2025-04-01 00:00:00', 'Singapour', 15, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2025-04-03 00:00:00', '2025-04-20 00:00:00', NULL),
(22, TRUE, 1, 5, 'HLCU1234567890', 5, '2025-04-05 00:00:00', 'Hambourg', 18, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SCAN-TMM-022', '2025-04-08 00:00:00', '2025-04-28 00:00:00', NULL),
(23, TRUE, 2, 4, 'PIL5432109876', 4, '2025-04-08 00:00:00', 'Durban', 13, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SCAN-TMM-023', '2025-04-10 00:00:00', '2025-04-17 00:00:00', NULL),
(24, TRUE, 1, 4, 'PIL4321098765', 14, '2025-04-10 00:00:00', 'Mumbai', 17, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SCAN-TMM-024', '2025-04-12 00:00:00', '2025-04-30 00:00:00', NULL),
(25, TRUE, 1, 10, 'ZIMU2345678901', 10, '2025-04-12 00:00:00', 'Rotterdam', 18, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SCAN-TMM-025', '2025-04-15 00:00:00', '2025-05-05 00:00:00', NULL);

-- ============================================================
-- 7. MARCHANDISES + CONTENEURS + GROUPAGES
-- ============================================================

INSERT INTO marchandise (id_dossier, id_unite_mesure, id_nature_marchandise, marque, modele, description, poids, volume, quantite) VALUES
(1, 2, 12, 'Divers', 'Général', 'Marchandises générales diverses en conteneurs', 18500.00, 28.50, 1),
(2, 2, 1, 'Divers alimentaires', 'N/A', 'Produits alimentaires divers', 12000.00, 22.00, 1),
(3, 2, 12, 'Divers', 'Groupage', 'Marchandises groupées score', 8500.00, 15.30, 1),
(4, 2, 8, 'Holcim', 'Ciment/Fer', 'Ciment et matériaux de construction', 25000.00, 18.00, 1),
(5, 2, 1, 'Shoprite', 'Divers', 'Produits alimentaires et non alimentaires', 9800.00, 20.50, 1),
(6, 2, 6, 'Cotona', 'Textile', 'Tissus et produits textiles ZF', 6500.00, 32.00, 1),
(7, 1, 2, 'Pharma', 'Médicaments', 'Produits pharmaceutiques divers', 3200.00, 8.50, 150),
(8, 2, 8, 'Build', 'Matériaux', 'Matériaux de construction divers', 22000.00, 25.00, 1),
(9, 2, 5, 'Auto', 'Pièces', 'Pièces détachées automobiles', 7800.00, 18.00, 1),
(10, 2, 1, 'Food', 'Divers', 'Produits alimentaires importés', 11000.00, 21.00, 1),
(11, 1, 7, 'Telma', 'Equipements', 'Équipements télécom et informatique', 450.00, 2.80, 25),
(12, 1, 2, 'MSF', 'Médical', 'Médicaments et matériel médical humanitaire', 280.00, 1.50, 40),
(13, 1, 7, 'Tech', 'IT', 'Matériel informatique et serveurs', 620.00, 3.20, 18),
(14, 1, 12, 'Home', 'Électroménager', 'Appareils électroménagers', 890.00, 4.50, 12),
(15, 1, 2, 'MinSanté', 'Médicaments', 'Médicaments pour ministère de la santé', 510.00, 2.10, 60),
(16, 1, 7, 'Orange', 'Telecom', 'Équipements réseau Orange', 380.00, 2.00, 15),
(17, 1, 9, 'Vanilla', 'Vanille', 'Vanille Bourbon Madagascar qualité export', 1200.00, 3.50, 80),
(18, 2, 9, 'Somapeche', 'Produits mer', 'Produits de la mer congelés', 15000.00, 28.00, 1),
(19, 2, 6, 'Magnard', 'Textile', 'Produits textiles zone franche', 7200.00, 30.00, 1),
(20, 2, 6, 'STT', 'Textile', 'Textile pour société textile ZF', 5800.00, 27.00, 1),
(21, 2, 12, 'ImportPlus', 'Divers', 'Marchandises générales', 14000.00, 24.00, 1),
(22, 2, 4, 'Global', 'Machines', 'Machines industrielles', 18500.00, 22.00, 1),
(23, 2, 12, 'EastAfrica', 'Groupage', 'Marchandises groupées', 9200.00, 16.50, 1),
(24, 2, 12, 'IOS', 'Divers', 'Fournitures diverses', 13500.00, 23.00, 1),
(25, 2, 3, 'Chem', 'Chimiques', 'Produits chimiques industriels', 16000.00, 20.00, 1);

INSERT INTO conteneur (id_dossier, id_type_container, numero_container, description) VALUES
(1, 3, 'CMAU1234567', 'Conteneur 40'' standard marchandises générales'),
(1, 3, 'CMAU1234568', 'Conteneur 40'' standard marchandises générales'),
(2, 1, 'PILU7654321', 'Conteneur 20'' produits alimentaires'),
(3, 1, 'CMAU2345678', 'Conteneur 20'' groupage'),
(4, 3, 'HLCU3456789', 'Conteneur 40'' matériaux construction'),
(4, 8, 'HLCU3456790', 'Flat rack pour éléments longs'),
(5, 1, 'PILU8765432', 'Conteneur 20'' Shoprite'),
(6, 4, 'CMAU3456789', 'Conteneur 40'' HC textile ZF'),
(7, 1, 'ZIMU4567890', 'Conteneur 20'' pharma (température contrôlée)'),
(8, 3, 'COSU5678901', 'Conteneur 40'' matériaux'),
(9, 1, 'HLCU6789012', 'Conteneur 20'' pièces auto'),
(10, 1, 'PILU7654322', 'Conteneur 20'' food'),
(17, 1, 'CMAU7890123', 'Conteneur 20'' vanille export'),
(18, 5, 'PILU6543210', 'Conteneur 40'' reefer produits mer'),
(19, 4, 'CMAU8901234', 'Conteneur 40'' HC textile ZF'),
(20, 4, 'CMAU9012345', 'Conteneur 40'' HC textile ZF'),
(21, 3, 'COSU0123456', 'Conteneur 40'' général'),
(22, 3, 'HLCU1234567', 'Conteneur 40'' machines'),
(23, 1, 'PILU5432109', 'Conteneur 20'' groupage'),
(24, 3, 'PILU4321098', 'Conteneur 40'' divers'),
(25, 1, 'ZIMU2345678', 'Conteneur 20'' produits chimiques');

INSERT INTO groupage (id_dossier, id_expedition, numero_groupage, partie_notifier) VALUES
(3, 3, 'GRP-2025-001', 'SCORE Madagascar - Service Import'),
(23, 23, 'GRP-2025-002', 'East Africa Trading - Antananarivo');

-- ============================================================
-- 8. DOCUMENTS, REMISES, CONSTATS, DÉCLARATIONS, MVT
-- ============================================================

INSERT INTO document_dossier (id_dossier, id_document, chemin_fichier, est_remis) VALUES
(1, 1, '/docs/2025/TMM-0001/BL_CMAU1234567890.pdf', TRUE),
(1, 3, '/docs/2025/TMM-0001/Facture_SODIAM.pdf', TRUE),
(1, 4, '/docs/2025/TMM-0001/PackingList.pdf', TRUE),
(1, 5, '/docs/2025/TMM-0001/Certificat_Origine.pdf', FALSE),
(1, 9, '/docs/2025/TMM-0001/Declaration.pdf', TRUE),
(2, 1, '/docs/2025/TMM-0002/BL_PIL9876543210.pdf', TRUE),
(2, 3, '/docs/2025/TMM-0002/Facture_Leaders.pdf', TRUE),
(2, 4, '/docs/2025/TMM-0002/PackingList.pdf', TRUE),
(2, 6, '/docs/2025/TMM-0002/Phyto.pdf', TRUE),
(2, 9, '/docs/2025/TMM-0002/Declaration.pdf', TRUE),
(3, 1, '/docs/2025/TMM-0003/BL_CMAU2345678901.pdf', TRUE),
(3, 3, '/docs/2025/TMM-0003/Facture_Score.pdf', TRUE),
(3, 4, '/docs/2025/TMM-0003/PackingList.pdf', TRUE),
(4, 1, '/docs/2025/TMM-0004/BL_HLCU3456789012.pdf', TRUE),
(4, 3, '/docs/2025/TMM-0004/Facture_Holcim.pdf', TRUE),
(4, 4, '/docs/2025/TMM-0004/PackingList.pdf', TRUE),
(4, 9, '/docs/2025/TMM-0004/Declaration.pdf', TRUE),
(4, 14, '/docs/2025/TMM-0004/Quittance.pdf', TRUE),
(5, 1, '/docs/2025/TMM-0005/BL_PIL8765432109.pdf', TRUE),
(5, 3, '/docs/2025/TMM-0005/Facture_Shoprite.pdf', TRUE),
(5, 9, '/docs/2025/TMM-0005/Declaration.pdf', TRUE),
(5, 15, '/docs/2025/TMM-0005/Bon_Enlever.pdf', TRUE),
(11, 2, '/docs/2025/TNR-0001/LTA_EK12345678.pdf', TRUE),
(11, 3, '/docs/2025/TNR-0001/Facture_Telma.pdf', TRUE),
(11, 4, '/docs/2025/TNR-0001/PackingList.pdf', TRUE),
(11, 9, '/docs/2025/TNR-0001/Declaration.pdf', TRUE),
(12, 2, '/docs/2025/TNR-0002/LTA_AF87654321.pdf', TRUE),
(12, 3, '/docs/2025/TNR-0002/Facture_MSF.pdf', TRUE),
(12, 11, '/docs/2025/TNR-0002/Certificat_Analyse.pdf', TRUE),
(12, 9, '/docs/2025/TNR-0002/Declaration.pdf', TRUE),
(12, 13, '/docs/2025/TNR-0002/Bon_Livraison.pdf', TRUE);

INSERT INTO remise (id_dossier, id_type_document, date_remise) VALUES
(1, 1, '2025-03-05 10:00:00'),
(1, 3, '2025-03-05 10:00:00'),
(1, 4, '2025-03-05 10:00:00'),
(2, 1, '2025-02-24 09:30:00'),
(2, 3, '2025-02-24 09:30:00'),
(4, 1, '2025-03-01 11:00:00'),
(4, 14, '2025-03-08 14:00:00'),
(5, 15, '2025-03-10 16:00:00'),
(11, 2, '2025-03-03 08:00:00'),
(12, 2, '2025-03-02 08:30:00'),
(12, 13, '2025-03-04 15:00:00');

INSERT INTO constat (id_dossier, id_expert_constat, id_type_constat, description) VALUES
(3, 2, 1, 'Légères avaries constatées sur 3 cartons suite à manipulation portuaire. Photos jointes.'),
(7, 1, 4, 'Ouverture de contrôle douanier effectuée en présence de l''expert. Conformité vérifiée.'),
(9, 5, 2, 'Manquant de 2 pièces détachées signalé par le client à la réception. Enquête en cours.'),
(18, 4, 1, 'Contrôle température reefer à l''arrivée : écart mineur détecté et corrigé.');

INSERT INTO declaration (id_dossier, numero_declaration, date_declaration, id_bureau, description) VALUES
(1, 'DEC-2025-TMM-0001', '2025-03-06 00:00:00', 1, 'Déclaration mise à la consommation marchandises générales SODIAM'),
(2, 'DEC-2025-TMM-0002', '2025-02-25 00:00:00', 1, 'Déclaration produits alimentaires Leaders Price'),
(4, 'DEC-2025-TMM-0004', '2025-03-02 00:00:00', 1, 'Déclaration matériaux construction Holcim'),
(5, 'DEC-2025-TMM-0005', '2025-03-08 00:00:00', 1, 'Déclaration Shoprite - mainlevée émise'),
(7, 'DEC-2025-TMM-0007', '2025-03-15 00:00:00', 1, 'Déclaration produits pharmaceutiques'),
(11, 'DEC-2025-TNR-0001', '2025-03-04 00:00:00', 2, 'Déclaration aérienne équipements Telma'),
(12, 'DEC-2025-TNR-0002', '2025-03-03 00:00:00', 2, 'Déclaration humanitaire MSF'),
(15, 'DEC-2025-TNR-0005', '2025-03-11 00:00:00', 2, 'Déclaration médicaments Ministère Santé'),
(17, 'DEC-2025-TMM-0011', '2025-03-20 00:00:00', 1, 'Déclaration export vanille');

INSERT INTO mvt_dossier (id_dossier, id_utilisateur, ip_utilisateur, id_operation, id_document_dossier, commentaire) VALUES
(1, 6, '10.0.1.15', 1, NULL, 'Création du dossier DOS-2025-TMM-0001'),
(1, 6, '10.0.1.15', 2, NULL, 'Saisie informations générales et lien ordre de transit'),
(1, 11, '10.0.1.22', 3, 1, 'Réception BL et documents originaux'),
(1, 11, '10.0.1.22', 4, 1, 'Contrôle documentaire OK'),
(1, 9, '10.0.1.30', 5, 5, 'Saisie déclaration en douane'),
(1, 9, '10.0.1.30', 6, 5, 'Validation déclaration'),
(1, 12, '10.0.1.40', 11, NULL, 'Enregistrement provision client'),
(2, 6, '10.0.1.15', 1, NULL, 'Création dossier Leaders Price'),
(2, 6, '10.0.1.15', 2, NULL, 'Saisie complète'),
(2, 11, '10.0.1.22', 3, 6, 'Réception documents'),
(2, 10, '10.0.1.31', 5, 10, 'Saisie déclaration'),
(4, 6, '10.0.1.15', 1, NULL, 'Création dossier Holcim matériaux'),
(4, 12, '10.0.1.40', 11, NULL, 'Paiement enregistré - provision OK'),
(4, 9, '10.0.1.30', 13, NULL, 'Autorisation d''enlèvement émise'),
(5, 7, '10.0.1.16', 1, NULL, 'Création dossier Shoprite'),
(5, 10, '10.0.1.31', 13, 21, 'Bon à enlever émis'),
(11, 8, '10.0.2.10', 1, NULL, 'Création dossier aérien Telma'),
(11, 8, '10.0.2.10', 5, 26, 'Déclaration aérienne saisie'),
(12, 8, '10.0.2.10', 1, NULL, 'Création dossier humanitaire MSF'),
(12, 8, '10.0.2.10', 16, 30, 'Livraison confirmée au client'),
(12, 4, '10.0.1.12', 17, NULL, 'Clôture du dossier MSF');

-- ============================================================
-- 9. DOSSIERS INTER-AGENCES + SUSPENSIONS
-- ============================================================

INSERT INTO dossier_interagence (id_dossier_mere, id_sous_dossier, id_agence_mere, id_agence_sous, numero_dossier_mere, numero_sous_dossier, motif) VALUES
(1, 30, 2, 1, 'DOS-2025-TMM-0001', 'DOS-2025-TNR-0007', 'Transit hinterland Toamasina → Antananarivo pour livraison finale'),
(5, 30, 2, 1, 'DOS-2025-TMM-0005', 'DOS-2025-TNR-0007', 'Coordination inter-agence pour livraison Shoprite');

INSERT INTO suspension_connexion (id_utilisateur, tentatives_echouees, date_suspension, duree_suspension) VALUES
(13, 3, NULL, 15),
(25, 1, NULL, 15);

COMMIT;

-- ============================================================
-- FIN DU SCRIPT DE SEED
-- Résumé approximatif des volumes générés :
-- - 8 agences
-- - 10 rôles
-- - 25 utilisateurs
-- - 12 types produit, 5 modes transport, 8 types client
-- - 10 types régime, 5 types bureau, 10 bureaux
-- - 8 modes acheminement, 12 catégories marchandise
-- - 11 phases, 22 opérations
-- - 9 traffics, 11 Incoterms, 7 types constat
-- - 8 experts, 10 unités mesure, 10 types container
-- - 30 pays
-- - 12 compagnies maritimes, 11 compagnies aériennes
-- - 15 navires, 22 stations (ports/aéroports)
-- - 18 types document
-- - 50 clients
-- - 30 ordres de transit + instructions
-- - 40 dossiers
-- - 25 expéditions
-- - 25 marchandises, 21 conteneurs, 2 groupages
-- - Documents, remises, constats, déclarations, mouvements
-- - 2 dossiers inter-agences, 2 suspensions
-- ============================================================
