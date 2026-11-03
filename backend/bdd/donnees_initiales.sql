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

update autorisation set niveau_autorisation = 10000;
UPDATE utilisateur set id_agence = 1 where id = 7;
UPDATE utilisateur SET nom_utilisateur = 'admin', nom = 'admin' WHERE id = 7;

UPDATE utilisateur set id_agence = 3 where id = 3;
UPDATE utilisateur SET nom_utilisateur = 'admintmg', nom = 'admintmg' WHERE id = 3;

commit;

SELECT code from agence join utilisateur on agence.id = utilisateur.id_agence where utilisateur.id = 7;
