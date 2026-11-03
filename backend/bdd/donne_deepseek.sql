-- Jeu de démonstration : à charger après base_centralisation.sql sur une base vide.
-- Si les tables contiennent déjà des données, utiliser reinitialiser_donnees.sql avant ce fichier.
-- Trois phases, workflows import/export, FOB uniquement, neuf agences.
-- Créations lissées sur douze mois ; répartition des agences pondérée par population urbaine.
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
('Mise a la consommation', 'MCI', 'Importation definitive'),
('Exportation', 'EXP', 'Exportation simple'),
('Transit', 'TRS', 'Transit international'),
('Entrepot', 'ENT', 'Regime d''entrepot douanier'),
('Admission temporaire', 'ADT', 'Admission temporaire');

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
('Exportation définitive (EX1)', 'Déclaration d''exportation définitive'),
('Transit international (TR)', 'Déclaration de transit international'),
('Admission temporaire (AT)', 'Déclaration pour régime d''admission temporaire'),
('Entrepôt douanier (E2)', 'Déclaration pour mise en entrepôt douanier');

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

-- ============================================================
-- 2. GENERATION DE DONNEES DE DEMONSTRATION
-- ============================================================
-- À charger sur une base vide après le schéma. Ce script ne supprime aucune
-- donnée existante et n'est pas destiné à être rejoué sur une base déjà remplie.
-- Dates relatives à l'exécution : débit régulier sur les douze derniers mois
-- calendaires, avec un mois courant proportionnel à sa durée déjà écoulée.
-- Les poids des villes proviennent du RGPH-3 INSTAT 2018 (population urbaine).
-- Source : https://instat.mg/documents/upload/main/INSTAT_RGPH3-Definitif-ResultatsGlogaux-Tome1_17-2021.pdf#page=45
-- Le quota d'Antananarivo est partagé entre TNR (90 %) et DIR (10 %).
-- Ce partage interne est une hypothèse de démonstration, distincte du recensement.
-- Toutes les dates réelles restent dans le passé ; seules les dates prévues,
-- les échéances et les limites de remise peuvent se situer dans le futur.

DO $$
DECLARE
    v_debut TIMESTAMPTZ := date_trunc('month', CURRENT_TIMESTAMP) - INTERVAL '11 months';
    v_agences INT[] := ARRAY(SELECT id FROM agence ORDER BY id);
    v_agences_dossiers INT[];
    v_roles INT[] := ARRAY(SELECT id FROM role ORDER BY id);
    v_productions INT[] := ARRAY(SELECT id FROM type_production ORDER BY id);
    v_clients INT[];
    v_utilisateurs INT[];
    v_ots_valides INT[];
    v_ots INT[] := '{}';
    v_dossiers INT[] := '{}';
    v_operations INT[];
    v_pays INT[] := ARRAY(SELECT id FROM pays ORDER BY id);
    v_types_clients INT[] := ARRAY(SELECT id FROM type_client ORDER BY id);
    v_bureaux INT[] := ARRAY(SELECT id FROM bureau ORDER BY id);
    v_unites INT[] := ARRAY(SELECT id FROM unite_mesure ORDER BY id);
    v_natures INT[] := ARRAY(SELECT id FROM nature_marchandise ORDER BY id);
    v_containers INT[] := ARRAY(SELECT id FROM type_container ORDER BY id);
    v_fob INT := (SELECT id FROM inconterme WHERE abreviation = 'FOB');
    v_preparation INT := (SELECT id FROM phase_dossier WHERE nom = 'PREPARATION');
    v_ouvert INT := (SELECT id FROM statut_dossier WHERE nom = 'Ouvert');
    v_user INT;
    v_ot INT;
    v_dossier INT;
    v_type INT;
    v_agence INT;
    v_role INT;
    v_regime INT;
    v_agence_destinataire INT;
    v_user_reponse INT;
    v_statut_ot INT;
    v_statut INT;
    v_phase INT;
    v_phase_suivante INT;
    v_doc INT;
    v_expedition INT;
    v_compagnie INT;
    v_navire INT;
    v_station_depart INT;
    v_station_arrivee INT;
    v_acheminement INT;
    v_nb_operations INT;
    v_nb_effectuees INT;
    v_creation TIMESTAMPTZ;
    v_fin_flux TIMESTAMPTZ;
    v_date_mvt TIMESTAMPTZ;
    v_date_doc TIMESTAMPTZ;
    v_date_depart TIMESTAMPTZ;
    v_date_prevue TIMESTAMPTZ;
    v_date_arrivee TIMESTAMPTZ;
    v_date_demande TIMESTAMPTZ;
    v_date_reponse TIMESTAMPTZ;
    v_numero TEXT;
    v_compteur INT := 0;
    i INT;
    j INT;
    ligne RECORD;
    op RECORD;
    type_doc RECORD;
    parent RECORD;
    enfant RECORD;
BEGIN
    -- Tirage reproductible ; les dates suivent la date d'exécution du script.
    PERFORM setseed(0.2718);
    UPDATE agence SET date_creation = v_debut - INTERVAL '4 months' + id * INTERVAL '1 day';

    -- Quotas exacts sur 300 dossiers par la méthode des plus forts restes.
    -- Compter Antananarivo une seule fois avant de partager TNR et DIR.
    WITH populations(ville, population) AS (VALUES
        ('Antananarivo', 1274225), ('Toamasina', 325857),
        ('Antsirabe', 246354), ('Mahajanga', 246022),
        ('Toliara', 168756), ('Antsiranana', 129320),
        ('Fort-Dauphin', 67284), ('Nosy Be', 50251)
    ), theoriques AS (
        SELECT ville, population, 300.0 * population / sum(population) OVER () AS quota
        FROM populations
    ), restes AS (
        SELECT *, floor(quota)::int AS base,
            row_number() OVER (ORDER BY quota - floor(quota) DESC, population DESC, ville) AS rang,
            300 - sum(floor(quota)::int) OVER () AS supplement
        FROM theoriques
    ), villes AS (
        SELECT ville, base + CASE WHEN rang <= supplement THEN 1 ELSE 0 END AS nombre
        FROM restes
    ), agences AS (
        SELECT a.id, a.code, CASE a.code
            WHEN 20 THEN round(v.nombre * 0.9)::int
            WHEN 90 THEN v.nombre - round(v.nombre * 0.9)::int
            ELSE v.nombre END AS nombre
        FROM agence a JOIN villes v ON v.ville = a.ville
    )
    SELECT array_agg(a.id ORDER BY md5('agence-demo-' || a.code::text || ':' || s.n::text))
    INTO v_agences_dossiers
    FROM agences a CROSS JOIN LATERAL generate_series(1, a.nombre) AS s(n);
    IF COALESCE(array_length(v_agences_dossiers, 1), 0) <> 300 THEN
        RAISE EXCEPTION 'La pondération des agences doit produire exactement 300 affectations.';
    END IF;

    -- 100 clients et 30 comptes de démonstration, créés avant leur activité.
    FOR i IN 1..100 LOOP
        INSERT INTO clients (nom, prenom, email, telephone, adresse, id_type_client,
                             nif, stat, fax, id_pays, cp_client, date_creation)
        VALUES ('ClientNom' || i, 'Prenom' || i, 'client' || i || '@mail.com',
                '032' || lpad(i::text, 7, '0'), 'Adresse ' || i,
                v_types_clients[1 + (i - 1) % array_length(v_types_clients, 1)],
                'NIF' || lpad(i::text, 10, '0'), 'STAT' || lpad(i::text, 10, '0'), 'Fax' || i,
                v_pays[1 + (i - 1) % array_length(v_pays, 1)], 'CLI' || lpad(i::text, 3, '0'),
                v_debut - INTERVAL '3 months' + random() * INTERVAL '20 days');
    END LOOP;

    FOR i IN 1..30 LOOP
        v_agence := v_agences[1 + (i - 1) % array_length(v_agences, 1)];
        v_role := v_roles[1 + (i - 1) % array_length(v_roles, 1)];
        v_creation := v_debut - INTERVAL '2 months' + i * INTERVAL '1 day';
        INSERT INTO utilisateur (nom_utilisateur, nom, prenom, email, mot_de_passe,
                                 id_role, id_agence, date_creation, date_modification)
        VALUES ('user' || i, 'NomUser' || i, 'PrenomUser' || i, 'user' || i || '@transit.mg',
                '$2a$12$KovwAzSOJPTaWqYJ2UgE/.sztG.CGSEgGa/QfoV.qlNK8qBtXzMna',
                v_role, v_agence, v_creation, v_creation) RETURNING id INTO v_user;
        -- Même accès administrateur que le jeu de démonstration précédent,
        -- limité aux comptes insérés ici, sans modifier d'autres utilisateurs.
        INSERT INTO autorisation (id_utilisateur, niveau_autorisation) VALUES (v_user, 10000);
    END LOOP;
    v_clients := ARRAY(SELECT id FROM clients ORDER BY id);
    v_utilisateurs := ARRAY(SELECT id FROM utilisateur ORDER BY id);

    -- 320 OT : suffisamment pour 300 dossiers et au moins 20 OT autonomes.
    -- Les 300 premiers suivent les quotas démographiques des dossiers ;
    -- les 20 OT supplémentaires utilisent les mêmes poids.
    FOR i IN 1..320 LOOP
        v_agence := CASE WHEN i <= 300 THEN v_agences_dossiers[i]
            ELSE v_agences_dossiers[1 + floor(random() * 300)::int] END;
        v_type := v_productions[1 + (i - 1) % array_length(v_productions, 1)];
        -- Permutation de deux productions : garder les mêmes volumes, avec
        -- un exemple maritime sur le dernier dossier créé aujourd'hui.
        IF i = 295 THEN v_type := v_productions[6]; END IF;
        IF i = 300 THEN v_type := v_productions[1]; END IF;
        SELECT r.id INTO v_regime FROM type_regime r
        WHERE r.code = (SELECT CASE tp.nom WHEN 'import' THEN 'MCI' WHEN 'export' THEN 'EXP' ELSE 'TRS' END
                        FROM type_production tp WHERE tp.id = v_type);
        SELECT id INTO v_user FROM utilisateur WHERE id_agence = v_agence ORDER BY id LIMIT 1;
        SELECT id INTO v_statut_ot FROM statut_ot
        WHERE nom = CASE WHEN i <= 300 THEN 'Valide'
                         WHEN i % 2 = 0 THEN 'Brouillon' ELSE 'Non valide' END;
        INSERT INTO ordre_transit (numero_ordre_transit, id_agence, id_client, id_type_production,
            id_nature_marchandise, numero_commande, id_utilisateur, id_type_regime, destination_finale,
            id_expediteur, id_destinataire, id_bureau_dedou, est_assure, id_destination_facture,
            id_statut_ot, date_creation)
        VALUES (10000 + i, v_agence, v_clients[1 + (i - 1) % 100], v_type,
                v_natures[1 + (i - 1) % array_length(v_natures, 1)],
                'CMD-' || lpad(i::text, 6, '0'), v_user,
                v_regime, 'Destination finale ' || i,
                v_clients[1 + i % 100], v_clients[1 + (i + 1) % 100],
                v_bureaux[1 + (i - 1) % array_length(v_bureaux, 1)], random() < 0.3,
                v_clients[1 + (i - 1) % 100], v_statut_ot,
                v_debut + random() * (CURRENT_TIMESTAMP - INTERVAL '3 days' - v_debut))
        RETURNING id INTO v_ot;
        v_ots := array_append(v_ots, v_ot);
    END LOOP;
    v_ots_valides := ARRAY(SELECT ot.id FROM ordre_transit ot JOIN statut_ot st ON st.id = ot.id_statut_ot
                          WHERE st.nom = 'Valide' ORDER BY ot.id);

    FOR i IN 1..400 LOOP
        INSERT INTO instruction (id_ordre_transit, instruction)
        VALUES (v_ots[1 + (i - 1) % 320], 'Instruction ' || i || ' : suivi du dossier.');
    END LOOP;

    -- 300 créneaux sur toute la durée écoulée : environ 25 à 28 créations
    -- par mois complet, sans compresser un quota complet au début du mois courant.
    FOR j IN 1..300 LOOP
            v_compteur := j;
            v_creation := v_debut + (CURRENT_TIMESTAMP - v_debut) * ((j - 1 + random()) / 300);
            IF j = 300 THEN
                -- Un dossier maritime du jour pour les parcours d'assignation,
                -- même si le script est lancé le premier jour du mois.
                v_creation := GREATEST(v_creation, date_trunc('day', CURRENT_TIMESTAMP));
            END IF;
            SELECT ot.*, tp.code AS code_production, a.code AS code_agence INTO ligne
            FROM ordre_transit ot JOIN type_production tp ON tp.id = ot.id_type_production
            JOIN agence a ON a.id = ot.id_agence WHERE ot.id = v_ots[v_compteur];
            v_numero := ligne.code_production::text || lpad(ligne.code_agence::text, 2, '0')
                     || to_char(v_creation, 'MM') || (EXTRACT(YEAR FROM v_creation)::int % 10)::text
                     || lpad(v_compteur::text, 4, '0');
            INSERT INTO dossier (id_agence, id_utilisateur, id_client, id_type_production,
                numero_dossier, id_ordre_transit, est_groupage, id_phase, id_statut, echeance,
                pour_compte, id_pays_origine, id_pays_destination, est_prepaye, est_provisionne,
                provision_demandee, lieu_paiement_fret, depart, adresse_livraison, id_inconterme,
                date_limite_remise, date_creation)
            VALUES (ligne.id_agence, ligne.id_utilisateur, ligne.id_client, ligne.id_type_production,
                v_numero, ligne.id, FALSE, v_preparation, v_ouvert,
                v_creation + (20 + v_compteur % 35) * INTERVAL '1 day',
                'Pour compte de ' || v_compteur,
                v_pays[1 + (v_compteur - 1) % array_length(v_pays, 1)],
                v_pays[1 + v_compteur % array_length(v_pays, 1)], random() < 0.5, FALSE,
                (50000 + random() * 950000)::numeric(10, 2), 'Antananarivo',
                'Port/Aeroport ' || v_compteur, 'Adresse livraison ' || v_compteur, v_fob,
                v_creation + (5 + v_compteur % 8) * INTERVAL '1 day', v_creation)
            RETURNING id INTO v_dossier;
            v_dossiers := array_append(v_dossiers, v_dossier);
    END LOOP;

    -- 40 sous-dossiers plus récents : même OT, même client et même production
    -- que leur dossier mère, avec une agence différente, sans boucle ni doublon.
    FOR i IN 1..40 LOOP
        -- Choisir un parent qui possède encore un enfant compatible. Cela
        -- préserve les quotas même lorsque la plupart des dossiers sont à TNR.
        SELECT p.* INTO parent FROM dossier p
        WHERE p.id = ANY(v_dossiers[1:180])
          AND NOT EXISTS (SELECT 1 FROM dossier_interagence di WHERE di.id_dossier_mere = p.id)
          AND EXISTS (
              SELECT 1 FROM dossier d WHERE d.id = ANY(v_dossiers[181:300])
                AND d.id_agence <> p.id_agence AND d.id_type_production = p.id_type_production
                AND NOT EXISTS (SELECT 1 FROM dossier_interagence di WHERE di.id_sous_dossier = d.id)
          )
        ORDER BY p.id LIMIT 1;
        IF parent.id IS NULL THEN RAISE EXCEPTION 'Dossier mère compatible introuvable'; END IF;
        SELECT d.* INTO enfant FROM dossier d
        WHERE d.id = ANY(v_dossiers[181:300]) AND d.id_agence <> parent.id_agence
          AND d.id_type_production = parent.id_type_production
          AND NOT EXISTS (SELECT 1 FROM dossier_interagence di WHERE di.id_sous_dossier = d.id)
        ORDER BY d.id LIMIT 1;
        IF enfant.id IS NULL THEN RAISE EXCEPTION 'Sous-dossier compatible introuvable'; END IF;
        UPDATE dossier SET id_ordre_transit = parent.id_ordre_transit, id_client = parent.id_client
        WHERE id = enfant.id;
        INSERT INTO dossier_interagence (id_dossier_mere, id_sous_dossier, motif, date_creation)
        VALUES (parent.id, enfant.id, 'Prise en charge par une autre agence', enfant.date_creation);

        -- Une demande acceptée est la cause du sous-dossier : réponse et
        -- création du sous-dossier ont la même date, par un utilisateur de l'agence sollicitée.
        SELECT id INTO v_user FROM utilisateur WHERE id_agence = parent.id_agence ORDER BY id LIMIT 1;
        SELECT id INTO v_user_reponse FROM utilisateur WHERE id_agence = enfant.id_agence ORDER BY id LIMIT 1;
        v_date_demande := parent.date_creation + (enfant.date_creation - parent.date_creation) * 0.5;
        INSERT INTO demande_interagence (id_dossier, id_agence_demandeuse, id_agence_destinataire,
            id_utilisateur_demande, id_utilisateur_reponse, motif, reponse_motif,
            statut, est_vue, date_demande, date_reponse)
        VALUES (parent.id, parent.id_agence, enfant.id_agence, v_user, v_user_reponse,
            'Prise en charge du dossier par l''agence ' || (SELECT nom FROM agence WHERE id = enfant.id_agence),
            'Demande acceptée ; sous-dossier créé pour le suivi local.', 'ACCEPTEE', TRUE,
            v_date_demande, enfant.date_creation);
    END LOOP;

    -- L'OT précède toujours le premier dossier qui lui est rattaché.
    UPDATE ordre_transit ot SET date_creation = LEAST(ot.date_creation,
        (SELECT min(d.date_creation) - INTERVAL '1 day' FROM dossier d WHERE d.id_ordre_transit = ot.id))
    WHERE EXISTS (SELECT 1 FROM dossier d WHERE d.id_ordre_transit = ot.id);

    -- Parcours réels : préfixe ordonné du workflow du type de production.
    -- Les types divers/colis, sans workflow défini, restent ouverts en préparation.
    FOR ligne IN SELECT * FROM dossier ORDER BY id LOOP
        v_operations := ARRAY(SELECT id FROM operation_dossier
            WHERE id_type_production = ligne.id_type_production ORDER BY niveau_operation, id);
        v_nb_operations := COALESCE(array_length(v_operations, 1), 0);
        IF v_nb_operations = 0 THEN CONTINUE; END IF;
        IF ligne.date_creation > CURRENT_TIMESTAMP - INTERVAL '3 days' THEN
            v_nb_effectuees := ligne.id % 5;
        ELSIF ligne.id % 17 = 0 THEN
            v_nb_effectuees := 0;
        ELSIF ligne.id % 5 IN (0, 1) THEN
            v_nb_effectuees := v_nb_operations;
        ELSE
            v_nb_effectuees := 1 + (ligne.id * 7) % v_nb_operations;
        END IF;
        v_phase := v_preparation;
        v_statut := v_ouvert;
        v_fin_flux := LEAST(ligne.date_creation + INTERVAL '45 days', CURRENT_TIMESTAMP);
        v_date_mvt := ligne.date_creation;
        FOR j IN 1..v_nb_effectuees LOOP
            SELECT * INTO op FROM operation_dossier WHERE id = v_operations[j];
            -- Après la dernière opération obligatoire d'une phase, avancer
            -- à la phase de l'opération suivante, comme le service Java.
            SELECT id_phase INTO v_phase_suivante FROM operation_dossier
            WHERE id_type_production = ligne.id_type_production AND niveau_operation > op.niveau_operation
            ORDER BY niveau_operation, id LIMIT 1;
            v_phase_suivante := COALESCE(v_phase_suivante, op.id_phase);
            v_date_mvt := ligne.date_creation + (v_fin_flux - ligne.date_creation) * (j::numeric / (v_nb_effectuees + 1));
            INSERT INTO mvt_dossier (id_dossier, id_utilisateur, id_operation,
                id_ancienne_phase, id_nouvelle_phase, id_ancien_statut, id_nouveau_statut,
                commentaire, date_mvt, origine)
            VALUES (ligne.id, ligne.id_utilisateur, op.id, v_phase, v_phase_suivante,
                v_statut, COALESCE(op.id_statut, v_statut), 'Opération effectuée : ' || op.description, v_date_mvt, 'SAISIE');
            v_phase := v_phase_suivante;
            v_statut := COALESCE(op.id_statut, v_statut);
        END LOOP;
        UPDATE dossier SET id_phase = v_phase, id_statut = v_statut,
            est_provisionne = EXISTS (SELECT 1 FROM mvt_dossier m JOIN operation_dossier o ON o.id = m.id_operation
                WHERE m.id_dossier = ligne.id AND o.description IN ('Provision', 'Provision (pour fret et débours)')),
            date_livraison = CASE WHEN v_phase = (SELECT id FROM phase_dossier WHERE nom = 'COMPTABILITE')
                THEN LEAST(ligne.date_creation + INTERVAL '25 days', v_date_mvt) ELSE NULL END
        WHERE id = ligne.id;
    END LOOP;

    -- Documents actifs du même flux et de la même nature que le dossier.
    -- Les documents requis par une opération réalisée sont toujours présents
    -- et remis AVANT cette opération. Quelques dossiers non clôturés restent
    -- volontairement incomplets pour les indicateurs de pièces manquantes.
    FOR ligne IN SELECT d.*, ot.id_nature_marchandise FROM dossier d
                 JOIN ordre_transit ot ON ot.id = d.id_ordre_transit ORDER BY d.id LOOP
        INSERT INTO document (id_dossier, id_type_document, nombre, chemin_fichier, nom_fichier, date_ajout)
        VALUES (ligne.id, (SELECT id FROM type_document WHERE nom = 'Document d''attachement'),
                1, '/documents/dossier_' || ligne.id || '.pdf', 'dossier_' || ligne.id || '.pdf', ligne.date_creation);
        FOR type_doc IN
            SELECT td.*, (SELECT min(m.date_mvt) FROM mvt_dossier m
                JOIN document_obligatoire_mvt dom ON dom.id_operation = m.id_operation
                WHERE m.id_dossier = ligne.id AND dom.id_type_document = td.id) AS premiere_utilisation
            FROM type_document td
            WHERE (td.id_type_production = ligne.id_type_production OR td.id_type_production IS NULL)
              AND td.nom <> 'Document d''attachement'
              AND (td.id_nature_marchandise IS NULL OR td.id_nature_marchandise = ligne.id_nature_marchandise)
        LOOP
            IF type_doc.premiere_utilisation IS NULL AND ligne.id % 5 = 0 THEN CONTINUE; END IF;
            IF NOT type_doc.est_obligatoire AND type_doc.premiere_utilisation IS NULL AND ligne.id % 3 <> 0 THEN CONTINUE; END IF;
            v_date_doc := ligne.date_creation +
                (COALESCE(type_doc.premiere_utilisation, LEAST(ligne.date_creation + INTERVAL '2 days', CURRENT_TIMESTAMP))
                    - ligne.date_creation) * 0.1;
            INSERT INTO document (id_dossier, id_type_document, nombre, chemin_fichier, nom_fichier, date_ajout)
            VALUES (ligne.id, type_doc.id, 1, '/documents/doc_' || ligne.id || '_' || type_doc.id || '.pdf',
                    type_doc.nom || '.pdf', v_date_doc);
            INSERT INTO remise (id_dossier, id_type_document, date_remise)
            VALUES (ligne.id, type_doc.id,
                LEAST(v_date_doc + INTERVAL '1 second', COALESCE(type_doc.premiere_utilisation, CURRENT_TIMESTAMP)));
        END LOOP;
    END LOOP;
    INSERT INTO document_mvt (id_mvt_dossier, id_document)
    SELECT m.id, (SELECT doc.id FROM document doc WHERE doc.id_dossier = m.id_dossier ORDER BY doc.id LIMIT 1)
    FROM mvt_dossier m WHERE m.id % 3 = 0;

    -- Expéditions maritimes/aériennes : dates déduites de l'OT et stations distinctes.
    -- Les OT encore en brouillon ou non validés n'ont ni expédition ni marchandise affectée.
    FOR ligne IN SELECT ot.*, tp.id_mode_transport, tp.nom AS sens, mt.nom AS transport
        FROM ordre_transit ot JOIN type_production tp ON tp.id = ot.id_type_production
        JOIN mode_transport mt ON mt.id = tp.id_mode_transport
        JOIN statut_ot st ON st.id = ot.id_statut_ot
        WHERE mt.nom IN ('Maritime', 'Aerien') AND st.nom = 'Valide' ORDER BY ot.id LOOP
        SELECT id INTO v_dossier FROM dossier WHERE id_ordre_transit = ligne.id ORDER BY id LIMIT 1;
        SELECT id INTO v_compagnie FROM compagnie WHERE id_mode_transport = ligne.id_mode_transport ORDER BY random() LIMIT 1;
        SELECT id INTO v_navire FROM navire WHERE id_compagnie = v_compagnie ORDER BY random() LIMIT 1;
        -- Import : départ étranger / arrivée Madagascar. Export : sens inverse.
        SELECT id INTO v_station_depart FROM station WHERE id_mode_transport = ligne.id_mode_transport
            AND ((code IN ('PTOM', 'PDAN', 'PMAH', 'PTOL', 'TNR', 'NOS', 'TMM')) = (ligne.sens = 'export'))
            ORDER BY random() LIMIT 1;
        SELECT id INTO v_station_arrivee FROM station
            WHERE id_mode_transport = ligne.id_mode_transport
              AND ((code IN ('PTOM', 'PDAN', 'PMAH', 'PTOL', 'TNR', 'NOS', 'TMM')) = (ligne.sens = 'import'))
            ORDER BY random() LIMIT 1;
        SELECT id INTO v_acheminement FROM mode_acheminement
        WHERE nom = CASE WHEN ligne.transport = 'Aerien' THEN 'Par Avion' ELSE 'Par Camion' END;
        v_date_depart := ligne.date_creation + INTERVAL '1 day';
        v_date_prevue := v_date_depart + CASE WHEN ligne.transport = 'Maritime' THEN INTERVAL '18 days' ELSE INTERVAL '2 days' END;
        v_date_arrivee := v_date_prevue + (ligne.id % 4) * INTERVAL '1 day';
        IF v_date_arrivee > CURRENT_TIMESTAMP THEN v_date_arrivee := NULL; END IF;
        IF v_date_depart > CURRENT_TIMESTAMP THEN v_date_depart := NULL; END IF;
        INSERT INTO expedition (id_ordre_transit, id_dossier, est_achemine, id_mode_transport,
            id_mode_acheminement, id_compagnie, id_navire, numero_bl, date_bl, lieu_bl,
            id_port_embarquement, id_port_debarquement, numero_vol, numero_lta, date_lta, lieu_lta,
            id_aeroport_embarquement, id_aeroport_debarquement, lib_scan, date_depart, date_prevu, date_arrivee)
        VALUES (ligne.id, v_dossier, v_date_arrivee IS NOT NULL, ligne.id_mode_transport,
            v_acheminement, v_compagnie, CASE WHEN ligne.transport = 'Maritime' THEN v_navire END,
            CASE WHEN ligne.transport = 'Maritime' THEN 'BL-' || lpad(ligne.id::text, 6, '0') END,
            CASE WHEN ligne.transport = 'Maritime' THEN v_date_depart END,
            CASE WHEN ligne.transport = 'Maritime' THEN (SELECT nom FROM station WHERE id = v_station_depart) END,
            CASE WHEN ligne.transport = 'Maritime' THEN v_station_depart END,
            CASE WHEN ligne.transport = 'Maritime' THEN v_station_arrivee END,
            CASE WHEN ligne.transport = 'Aerien' THEN 'VOL-' || lpad(ligne.id::text, 6, '0') END,
            CASE WHEN ligne.transport = 'Aerien' THEN 'LTA-' || lpad(ligne.id::text, 6, '0') END,
            CASE WHEN ligne.transport = 'Aerien' THEN v_date_depart END,
            CASE WHEN ligne.transport = 'Aerien' THEN (SELECT nom FROM station WHERE id = v_station_depart) END,
            CASE WHEN ligne.transport = 'Aerien' THEN v_station_depart END,
            CASE WHEN ligne.transport = 'Aerien' THEN v_station_arrivee END,
            'Scan OT ' || ligne.id, v_date_depart, v_date_prevue, v_date_arrivee);
    END LOOP;

    -- Les pays et lieux des dossiers suivent les stations de leur expédition.
    WITH pays_stations(code_station, code_pays) AS (VALUES
        ('PTOM', 'MDG'), ('PDAN', 'MDG'), ('PMAH', 'MDG'), ('PTOL', 'MDG'),
        ('TNR', 'MDG'), ('NOS', 'MDG'), ('TMM', 'MDG'),
        ('PMRS', 'FRA'), ('PHAV', 'FRA'), ('CDG', 'FRA'), ('ORY', 'FRA'),
        ('PANV', 'BEL'), ('BRU', 'BEL'), ('PRTM', 'NLD'), ('AMS', 'NLD'),
        ('PHAM', 'DEU'), ('FRA', 'DEU'), ('PSHA', 'CHN'), ('PSIN', 'SGP'),
        ('PBUS', 'KOR'), ('PJEA', 'ARE'), ('DXB', 'ARE'), ('PDUR', 'ZAF'),
        ('PMOM', 'KEN'), ('NBO', 'KEN'), ('LHR', 'GBR'), ('GVA', 'CHE'),
        ('DOH', 'QAT'), ('IST', 'TUR'), ('ADD', 'ETH')
    )
    UPDATE dossier d SET id_pays_origine = po.id, id_pays_destination = pd.id,
        depart = sd.lieu, adresse_livraison = 'Livraison à ' || sa.lieu
    FROM expedition e
    JOIN station sd ON sd.id = COALESCE(e.id_port_embarquement, e.id_aeroport_embarquement)
    JOIN station sa ON sa.id = COALESCE(e.id_port_debarquement, e.id_aeroport_debarquement)
    JOIN pays_stations origine ON origine.code_station = sd.code
    JOIN pays_stations destination ON destination.code_station = sa.code
    JOIN pays po ON po.code = origine.code_pays JOIN pays pd ON pd.code = destination.code_pays
    WHERE d.id_ordre_transit = e.id_ordre_transit;

    FOR i IN 1..50 LOOP
        SELECT * INTO ligne FROM dossier WHERE id = v_dossiers[1 + (i * 7) % 300];
        INSERT INTO constat (id_dossier, id_expert_constat, id_type_constat, description, date_creation)
        VALUES (ligne.id, (SELECT id FROM expert_constat ORDER BY id OFFSET (i % 10) LIMIT 1),
            (SELECT id FROM type_constat ORDER BY id OFFSET (i % 3) LIMIT 1), 'Constat ' || i,
            LEAST(ligne.date_creation + INTERVAL '1 hour', CURRENT_TIMESTAMP));
    END LOOP;

    FOR i IN 1..600 LOOP
        INSERT INTO marchandise (id_ordre_transit, id_unite_mesure, marque, modele, description, poids, volume, quantite)
        VALUES (v_ots_valides[1 + (i - 1) % array_length(v_ots_valides, 1)],
            v_unites[1 + (i - 1) % array_length(v_unites, 1)],
            'Marque' || i % 20, 'Modele' || i % 30, 'Description marchandise ' || i,
            (random() * 5000)::numeric(10, 2), (random() * 100)::numeric(10, 2), 1 + (i % 100));
    END LOOP;

    -- Un conteneur pour chaque dossier maritime ; aucun sur un dossier aérien ou terrestre.
    FOR ligne IN SELECT d.id FROM dossier d
        JOIN type_production tp ON tp.id = d.id_type_production
        JOIN mode_transport mt ON mt.id = tp.id_mode_transport
        WHERE mt.nom = 'Maritime' ORDER BY d.id LOOP
        v_dossier := ligne.id;
        INSERT INTO conteneur (id_dossier, id_type_container, numero_container, description)
        VALUES (v_dossier, v_containers[1 + (v_dossier - 1) % array_length(v_containers, 1)],
            'CONT-' || lpad(v_dossier::text, 7, '0'), 'Conteneur du dossier ' || v_dossier) RETURNING id INTO v_doc;
        INSERT INTO dossier_conteneur (id_dossier, id_conteneur) VALUES (v_dossier, v_doc);
    END LOOP;

    -- 20 demandes en attente et 20 refusées, sur des dossiers encore actifs
    -- sans autre demande. Les dates suivent l'ouverture et chaque auteur appartient
    -- à l'agence qui demande ou répond. Ce jeu n'expédie aucun email.
    v_compteur := 0;
    FOR ligne IN SELECT d.* FROM dossier d
        JOIN statut_dossier st ON st.id = d.id_statut
        WHERE st.nom NOT IN ('Clôturé', 'Annulé')
          AND NOT EXISTS (SELECT 1 FROM demande_interagence di WHERE di.id_dossier = d.id)
          AND NOT EXISTS (SELECT 1 FROM dossier_interagence di WHERE di.id_sous_dossier = d.id)
        ORDER BY d.date_creation DESC, d.id DESC LIMIT 40 LOOP
        v_compteur := v_compteur + 1;
        SELECT id INTO v_agence_destinataire FROM agence WHERE id <> ligne.id_agence
            ORDER BY (code + ligne.id) % 100, id LIMIT 1;
        SELECT id INTO v_user FROM utilisateur WHERE id_agence = ligne.id_agence ORDER BY id LIMIT 1;
        SELECT id INTO v_user_reponse FROM utilisateur WHERE id_agence = v_agence_destinataire ORDER BY id LIMIT 1;
        v_date_demande := ligne.date_creation + (CURRENT_TIMESTAMP - ligne.date_creation) * 0.2;
        v_date_reponse := v_date_demande + (CURRENT_TIMESTAMP - v_date_demande) * 0.5;
        INSERT INTO demande_interagence (id_dossier, id_agence_demandeuse, id_agence_destinataire,
            id_utilisateur_demande, id_utilisateur_reponse, motif, reponse_motif,
            statut, est_vue, date_demande, date_reponse)
        VALUES (ligne.id, ligne.id_agence, v_agence_destinataire, v_user,
            CASE WHEN v_compteur > 20 THEN v_user_reponse END,
            CASE WHEN v_compteur <= 20 THEN 'Demande de prise en charge locale en attente de confirmation.'
                 ELSE 'Demande de prise en charge locale examinée par l''agence sollicitée.' END,
            CASE WHEN v_compteur > 20 THEN 'Prise en charge indisponible ; le dossier reste suivi par son agence d''origine.' END,
            CASE WHEN v_compteur <= 20 THEN 'EN_ATTENTE' ELSE 'REFUSEE' END,
            CASE WHEN v_compteur <= 20 THEN v_compteur % 3 = 0 ELSE TRUE END,
            v_date_demande, CASE WHEN v_compteur > 20 THEN v_date_reponse END);
    END LOOP;

    FOR ligne IN SELECT d.id, d.date_creation, e.id AS expedition FROM dossier d
        JOIN expedition e ON e.id_ordre_transit = d.id_ordre_transit ORDER BY d.id LIMIT 100 LOOP
        INSERT INTO groupage (id_dossier, id_expedition, numero_groupage, partie_notifier, date_creation)
        VALUES (ligne.id, ligne.expedition, 'GRP-' || lpad(ligne.id::text, 7, '0'),
            'Partie notifier ' || ligne.id, LEAST(ligne.date_creation + INTERVAL '1 hour', CURRENT_TIMESTAMP));
        UPDATE dossier SET est_groupage = TRUE WHERE id = ligne.id;
    END LOOP;

    -- Une déclaration par dossier ayant réellement effectué cette opération.
    FOR ligne IN SELECT d.id, d.id_type_production, d.id_ordre_transit, m.date_mvt, tp.nom AS sens,
                        ot.id_bureau_dedou
        FROM dossier d JOIN mvt_dossier m ON m.id_dossier = d.id
        JOIN operation_dossier o ON o.id = m.id_operation AND o.description = 'Déclaration'
        JOIN type_production tp ON tp.id = d.id_type_production
        JOIN ordre_transit ot ON ot.id = d.id_ordre_transit ORDER BY d.id LOOP
        SELECT doc.id INTO v_doc FROM document doc JOIN type_document td ON td.id = doc.id_type_document
        WHERE doc.id_dossier = ligne.id AND td.nom LIKE 'Declaration en douane%' ORDER BY doc.id LIMIT 1;
        INSERT INTO declaration (id_dossier, numero_declaration, date_declaration, id_bureau,
            id_type_declaration, id_document, description, date_ajout)
        VALUES (ligne.id, 'DEC-' || lpad(ligne.id::text, 8, '0'), ligne.date_mvt, ligne.id_bureau_dedou,
            (SELECT id FROM type_declaration WHERE nom = CASE WHEN ligne.sens = 'import'
                THEN 'Mise à la consommation (IM4)' ELSE 'Exportation définitive (EX1)' END),
            v_doc, 'Déclaration du dossier ' || ligne.id, ligne.date_mvt);
    END LOOP;

    FOR i IN 1..5 LOOP
        INSERT INTO suspension_connexion (id_utilisateur, tentatives_echouees, date_suspension,
            duree_suspension_minutes, date_creation)
        VALUES (v_utilisateurs[i], i, CURRENT_TIMESTAMP - i * INTERVAL '1 day',
                15, CURRENT_TIMESTAMP - i * INTERVAL '1 day');
    END LOOP;
END $$;

COMMIT;

update autorisation set niveau_autorisation = 10000;
commit;

SELECT code from agence join utilisateur on agence.id = utilisateur.id_agence where utilisateur.id = 7;
