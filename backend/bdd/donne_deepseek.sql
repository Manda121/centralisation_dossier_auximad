BEGIN;

-- ============================================================
-- 0. STATUT DOSSIER (nouvelle table)
-- ============================================================
INSERT INTO statut_dossier (nom, description) VALUES
('Ouvert', 'Dossier en cours de traitement'),
('Clôturé', 'Dossier finalisé'),
('Annulé', 'Dossier annulé'),
('En litige', 'Dossier en litige');

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
(40, 'Manakara', 'Avenue de l''Independance', 'Manakara', '316'),
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

-- PHASE DOSSIER (avec id_mode_transport = NULL par défaut, car générique)
INSERT INTO phase_dossier (nom, id_mode_transport, description) VALUES
('PREPARATION', NULL, 'Preparation du dossier'),
('TRAITEMENT', NULL, 'Traitement du dossier'),
('DEBARQUEMENT/LIVRAISON', NULL, 'Debarquement et livraison'),
('COMPTABILITE', NULL, 'Comptabilite et cloture');

-- Récupération des IDs pour les opérations
DO $$
DECLARE
    v_statut_ouvert_id INT;
BEGIN
    SELECT id INTO v_statut_ouvert_id FROM statut_dossier WHERE nom = 'Ouvert';

    -- =========================================================================
-- 0. CORRECTION DE phase_dossier
-- =========================================================================
-- Le script existant ne definit que 4 phases generiques, dont
-- 'DEBARQUEMENT/LIVRAISON'. Or le flux import utilise 'DEBARQUEMENT' et
-- le flux export utilise 'EMBARQUEMENT' : ce sont deux etapes physiquement
-- distinctes (arrivee vs depart de la marchandise), donc deux phases
-- distinctes, pas une approximation partagee.

-- 1) On renomme la phase existante pour qu'elle corresponde exactement
--    au libelle utilise par le flux import.
UPDATE phase_dossier
SET nom = 'DEBARQUEMENT'
WHERE nom = 'DEBARQUEMENT/LIVRAISON';

-- 2) On ajoute la phase manquante pour le flux export.
INSERT INTO phase_dossier (nom, id_mode_transport, description)
VALUES ('EMBARQUEMENT', NULL, 'Embarquement et expedition (export)')
ON CONFLICT (nom) DO NOTHING;

-- =========================================================================
-- 1. INSERT INTO operation_dossier
-- Lie chaque operation a id_type_production (import / export)
--
-- Hypotheses retenues :
--  1. "import" et "export" existent chacun 2 fois dans type_production
--     (maritime + aerien) : la jointure sur tp.nom duplique donc
--     automatiquement chaque operation pour les deux modes de transport.
--  2. id_statut = 'Ouvert' pour toutes les operations, sauf 'Cloture' qui
--     est rattachee a 'Clôturé'.
--  3. niveau_autorisation / niveau_validation / niveau_operation suivent
--     la meme logique croissante que le script existant (1 = PREPARATION,
--     2 = TRAITEMENT, 3 = DEBARQUEMENT/EMBARQUEMENT, 4 = COMPTABILITE,
--     5 = Cloture).
--  4. est_obligatoire = FALSE pour les etapes purement informatives
--     ("Information Client", "Provision"), TRUE pour le reste.
-- =========================================================================

INSERT INTO operation_dossier
    (description, id_phase, id_statut, id_type_production,
     niveau_autorisation, niveau_validation, niveau_operation,
     est_obligatoire, est_actif)
SELECT
    t.description,
    (SELECT id FROM phase_dossier WHERE nom = t.phase),
    (SELECT id FROM statut_dossier WHERE nom = t.statut),
    tp.id,
    t.niveau_autorisation,
    t.niveau_validation,
    t.niveau_operation,
    t.est_obligatoire,
    TRUE
FROM (VALUES
    -- sens, phase, statut, description, niv_autorisation, niv_validation, niv_operation, est_obligatoire

    -- ================= IMPORT =================
    -- PREPARATION
    ('import', 'PREPARATION',   'Ouvert',   'Reservation TC/Ligne',     1, 1, 1, TRUE),
    ('import', 'PREPARATION',   'Ouvert',   'Reception BL/LTA',         1, 1, 1, TRUE),
    ('import', 'PREPARATION',   'Ouvert',   'Position Container',       1, 1, 1, TRUE),

    -- TRAITEMENT
    ('import', 'TRAITEMENT',    'Ouvert',   'Declaration',              2, 2, 2, TRUE),
    ('import', 'TRAITEMENT',    'Ouvert',   'Dispatch/Passeur',         2, 2, 2, TRUE),
    ('import', 'TRAITEMENT',    'Ouvert',   'Depot Declaration',        2, 2, 2, TRUE),
    ('import', 'TRAITEMENT',    'Ouvert',   'Visite Douane',            2, 3, 2, TRUE),
    ('import', 'TRAITEMENT',    'Ouvert',   'Empotage',                 2, 3, 2, TRUE),

    -- DEBARQUEMENT
    ('import', 'DEBARQUEMENT',  'Ouvert',   'Depart Camion',            3, 3, 3, TRUE),
    ('import', 'DEBARQUEMENT',  'Ouvert',   'Arrivee Camion',           3, 3, 3, TRUE),
    ('import', 'DEBARQUEMENT',  'Ouvert',   'Mise a quai',              3, 4, 3, TRUE),
    ('import', 'DEBARQUEMENT',  'Ouvert',   'Taxation BAE',             3, 4, 3, TRUE),
    ('import', 'DEBARQUEMENT',  'Ouvert',   'Closing date',             3, 4, 3, TRUE),
    ('import', 'DEBARQUEMENT',  'Ouvert',   'Information Client',       3, 4, 3, FALSE),

    -- COMPTABILITE
    ('import', 'COMPTABILITE',  'Ouvert',   'Provision',                4, 4, 4, FALSE),
    ('import', 'COMPTABILITE',  'Ouvert',   'Facturation',              4, 5, 4, TRUE),
    ('import', 'COMPTABILITE',  'Ouvert',   'Transmission',             4, 5, 4, TRUE),
    ('import', 'COMPTABILITE',  'Ouvert',   'Paiement',                 4, 5, 5, TRUE),
    ('import', 'COMPTABILITE',  'Clôturé',  'Cloture',                  5, 5, 5, TRUE),

    -- ================= EXPORT =================
    -- PREPARATION
    ('export', 'PREPARATION',   'Ouvert',   'Avis d''arrivee client',   1, 1, 1, TRUE),
    ('export', 'PREPARATION',   'Ouvert',   'Premier reception Doc',    1, 1, 1, TRUE),
    ('export', 'PREPARATION',   'Ouvert',   'Premier envoi Doc',        1, 1, 1, TRUE),
    ('export', 'PREPARATION',   'Ouvert',   'Expediee',                 1, 1, 1, TRUE),

    -- TRAITEMENT
    ('export', 'TRAITEMENT',    'Ouvert',   'Declaration',              2, 2, 2, TRUE),
    ('export', 'TRAITEMENT',    'Ouvert',   'Dispatch/Passeur',         2, 2, 2, TRUE),
    ('export', 'TRAITEMENT',    'Ouvert',   'Depot Declaration',        2, 2, 2, TRUE),
    ('export', 'TRAITEMENT',    'Ouvert',   'Visite Douane',            2, 3, 2, TRUE),
    ('export', 'TRAITEMENT',    'Ouvert',   'Depotage',                 2, 3, 2, TRUE),

    -- EMBARQUEMENT
    ('export', 'EMBARQUEMENT',  'Ouvert',   'Bon a enlever',            3, 3, 3, TRUE),
    ('export', 'EMBARQUEMENT',  'Ouvert',   'Sortie Aero/Port',         3, 3, 3, TRUE),
    ('export', 'EMBARQUEMENT',  'Ouvert',   'Taxation',                 3, 4, 3, TRUE),
    ('export', 'EMBARQUEMENT',  'Ouvert',   'Information Client',       3, 4, 3, FALSE),
    ('export', 'EMBARQUEMENT',  'Ouvert',   'Livraison',                3, 4, 3, TRUE),

    -- COMPTABILITE
    ('export', 'COMPTABILITE',  'Ouvert',   'Provision',                4, 4, 4, FALSE),
    ('export', 'COMPTABILITE',  'Ouvert',   'Facturation',              4, 5, 4, TRUE),
    ('export', 'COMPTABILITE',  'Ouvert',   'Transmission',             4, 5, 4, TRUE),
    ('export', 'COMPTABILITE',  'Ouvert',   'Paiement',                 4, 5, 5, TRUE),
    ('export', 'COMPTABILITE',  'Clôturé',  'Cloture',                  5, 5, 5, TRUE)

) AS t(sens, phase, statut, description, niveau_autorisation, niveau_validation, niveau_operation, est_obligatoire)
JOIN type_production tp ON tp.nom = t.sens;
END $$;

INSERT INTO inconterme (nom, abreviation, description) VALUES
('Ex Works', 'EXW', 'Depart usine'),
('Free Carrier', 'FCA', 'Franco transporteur'),
('Free Alongside Ship', 'FAS', 'Franco le long du navire'),
('Free On Board', 'FOB', 'Franco a bord'),
('Cost and Freight', 'CFR', 'Cout et fret'),
('Cost, Insurance and Freight', 'CIF', 'Cout, assurance et fret'),
('Carriage Paid To', 'CPT', 'Port paye jusqu''a'),
('Carriage and Insurance Paid To', 'CIP', 'Port paye, assurance comprise'),
('Delivered At Place', 'DAP', 'Rendu au lieu de destination'),
('Delivered At Place Unloaded', 'DPU', 'Rendu au lieu de destination decharge'),
('Delivered Duty Paid', 'DDP', 'Rendu droits acquittes');

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
-- 2. GENERATION DE DONNEES EN MASSE
-- ============================================================

DO $$
DECLARE
    v_agence_ids INT[];
    v_role_ids INT[];
    v_type_production_ids INT[];
    v_type_client_ids INT[];
    v_type_regime_ids INT[];
    v_bureau_ids INT[];
    v_mode_acheminement_ids INT[];
    v_nature_marchandise_ids INT[];
    v_phase_ids INT[];
    v_inconterme_ids INT[];
    v_type_constat_ids INT[];
    v_expert_constat_ids INT[];
    v_pays_ids INT[];
    v_type_document_ids INT[];
    v_type_declaration_ids INT[];
    v_type_container_ids INT[];
    v_compagnie_ids INT[];
    v_compagnie_maritime_ids INT[];
    v_compagnie_aerienne_ids INT[];
    v_navire_ids INT[];
    v_port_ids INT[];
    v_aeroport_ids INT[];
    v_unite_mesure_ids INT[];
    v_operation_ids INT[];
    v_statut_ids INT[];
    v_statut_ouvert_id INT;
    v_statut_ot_ids INT[];
    v_statut_ot_creer_id INT;

    v_client_ids INT[];
    v_utilisateur_ids INT[];
    v_ot_ids INT[] := '{}';
    v_dossier_ids INT[] := '{}';
    v_expedition_ids INT[] := '{}';
    v_document_ids INT[] := '{}';

    i INT;
    v_user_id INT;
    v_ot_id INT;
    v_dossier_id INT;
    v_expedition_id INT;
    v_agence_id INT;
    v_client_id INT;
    v_type_production_id INT;
    v_type_regime_id INT;
    v_bureau_id INT;
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
    v_nature_marchandise_id INT;
    v_compagnie_id INT;
    v_navire_id INT;
    v_port_emb INT;
    v_port_deb INT;
    v_aeroport_emb INT;
    v_aeroport_deb INT;
    v_dossier_mere INT;
    v_sous_dossier INT;
    v_mode_transport_maritime_id INT;
    v_mode_transport_aerien_id INT;
    v_expedition_mode_transport_id INT;
    v_doc_id INT;
    v_mvt_id INT;
    v_est_remis BOOLEAN;
    v_rand_statut_ot_id INT;
BEGIN
    -- Récupération des IDs des tables de référence
    v_agence_ids := ARRAY(SELECT id FROM agence);
    v_role_ids := ARRAY(SELECT id FROM role);
    v_type_production_ids := ARRAY(SELECT id FROM type_production);
    v_type_client_ids := ARRAY(SELECT id FROM type_client);
    v_type_regime_ids := ARRAY(SELECT id FROM type_regime);
    v_bureau_ids := ARRAY(SELECT id FROM bureau);
    v_mode_acheminement_ids := ARRAY(SELECT id FROM mode_acheminement);
    v_nature_marchandise_ids := ARRAY(SELECT id FROM nature_marchandise);
    v_phase_ids := ARRAY(SELECT id FROM phase_dossier);
    v_inconterme_ids := ARRAY(SELECT id FROM inconterme);
    v_type_constat_ids := ARRAY(SELECT id FROM type_constat);
    v_expert_constat_ids := ARRAY(SELECT id FROM expert_constat);
    v_pays_ids := ARRAY(SELECT id FROM pays);
    v_type_document_ids := ARRAY(SELECT id FROM type_document);
    v_type_declaration_ids := ARRAY(SELECT id FROM type_declaration);
    v_type_container_ids := ARRAY(SELECT id FROM type_container);
    v_compagnie_ids := ARRAY(SELECT id FROM compagnie);
    v_compagnie_maritime_ids := ARRAY(SELECT id FROM compagnie WHERE id_mode_transport = (SELECT id FROM mode_transport WHERE nom = 'Maritime'));
    v_compagnie_aerienne_ids := ARRAY(SELECT id FROM compagnie WHERE id_mode_transport = (SELECT id FROM mode_transport WHERE nom = 'Aerien'));
    v_navire_ids := ARRAY(SELECT id FROM navire);
    v_port_ids := ARRAY(SELECT id FROM station WHERE id_mode_transport = (SELECT id FROM mode_transport WHERE nom = 'Maritime'));
    v_aeroport_ids := ARRAY(SELECT id FROM station WHERE id_mode_transport = (SELECT id FROM mode_transport WHERE nom = 'Aerien'));
    v_unite_mesure_ids := ARRAY(SELECT id FROM unite_mesure);
    v_operation_ids := ARRAY(SELECT id FROM operation_dossier);
    v_statut_ids := ARRAY(SELECT id FROM statut_dossier);
    v_statut_ouvert_id := (SELECT id FROM statut_dossier WHERE nom = 'Ouvert');
    v_mode_transport_maritime_id := (SELECT id FROM mode_transport WHERE nom = 'Maritime');
    v_mode_transport_aerien_id := (SELECT id FROM mode_transport WHERE nom = 'Aerien');

    -- Récupération des statuts OT
    v_statut_ot_ids := ARRAY(SELECT id FROM statut_ot);

    -- CLIENTS (100) avec cp_client unique de 6 caractères
    FOR i IN 1..100 LOOP
        INSERT INTO clients (nom, prenom, email, telephone, adresse, id_type_client, nif, stat, fax, id_pays, cp_client)
        VALUES (
            'ClientNom' || i,
            'Prenom' || i,
            'client' || i || '@mail.com',
            '032' || lpad(i::text, 7, '0'),
            'Adresse ' || i,
            v_type_client_ids[1 + floor(random() * array_length(v_type_client_ids, 1))::int],
            'NIF' || lpad(i::text, 10, '0'),
            'STAT' || lpad(i::text, 10, '0'),
            'Fax' || i,
            v_pays_ids[1 + floor(random() * array_length(v_pays_ids, 1))::int],
            'CLI' || lpad(i::text, 3, '0')   -- 6 caractères, unique de CLI001 à CLI100
        );
    END LOOP;

    -- UTILISATEURS (30) + AUTORISATIONS
    FOR i IN 1..30 LOOP
        v_agence_id := v_agence_ids[1 + floor(random() * array_length(v_agence_ids, 1))::int];
        INSERT INTO utilisateur (nom_utilisateur, nom, prenom, email, mot_de_passe, id_role, id_agence)
        VALUES (
            'user' || i,
            'NomUser' || i,
            'PrenomUser' || i,
            'user' || i || '@transit.mg',
            '$2a$12$KovwAzSOJPTaWqYJ2UgE/.sztG.CGSEgGa/QfoV.qlNK8qBtXzMna',
            v_role_ids[1 + floor(random() * array_length(v_role_ids, 1))::int],
            v_agence_id
        ) RETURNING id INTO v_user_id;

        INSERT INTO autorisation (id_utilisateur, niveau_autorisation)
        VALUES (v_user_id, 1 + floor(random() * 5)::int);
    END LOOP;

    v_client_ids := ARRAY(SELECT id FROM clients);
    v_utilisateur_ids := ARRAY(SELECT id FROM utilisateur);

    -- ORDRES DE TRANSIT (200) avec statut OT aléatoire
    FOR i IN 1..200 LOOP
        v_agence_id := v_agence_ids[1 + floor(random() * array_length(v_agence_ids, 1))::int];
        v_client_id := v_client_ids[1 + floor(random() * array_length(v_client_ids, 1))::int];
        v_type_production_id := v_type_production_ids[1 + floor(random() * array_length(v_type_production_ids, 1))::int];
        v_type_regime_id := v_type_regime_ids[1 + floor(random() * array_length(v_type_regime_ids, 1))::int];
        v_bureau_id := v_bureau_ids[1 + floor(random() * array_length(v_bureau_ids, 1))::int];
        -- Choix aléatoire du statut OT
        v_rand_statut_ot_id := v_statut_ot_ids[1 + floor(random() * array_length(v_statut_ot_ids, 1))::int];

        INSERT INTO ordre_transit (
            numero_ordre_transit, id_agence, id_client, id_type_production,
            numero_commande, id_utilisateur, id_type_regime, destination_finale,
            id_expediteur, id_destinataire, id_bureau_dedou, est_assure, id_destination_facture,
            id_statut_ot
        ) VALUES (
            10000 + i,
            v_agence_id,
            v_client_id,
            v_type_production_id,
            'CMD-' || lpad(i::text, 6, '0'),
            v_utilisateur_ids[1 + floor(random() * array_length(v_utilisateur_ids, 1))::int],
            v_type_regime_id,
            'Destination finale ' || i,
            v_client_ids[1 + floor(random() * array_length(v_client_ids, 1))::int],
            v_client_ids[1 + floor(random() * array_length(v_client_ids, 1))::int],
            v_bureau_id,
            (random() < 0.3),
            v_client_ids[1 + floor(random() * array_length(v_client_ids, 1))::int],
            v_rand_statut_ot_id
        ) RETURNING id INTO v_ot_id;

        v_ot_ids := array_append(v_ot_ids, v_ot_id);
    END LOOP;

    -- INSTRUCTIONS (400)
    FOR i IN 1..400 LOOP
        INSERT INTO instruction (id_ordre_transit, instruction)
        VALUES (
            v_ot_ids[1 + floor(random() * array_length(v_ot_ids, 1))::int],
            'Instruction ' || i || ' : suivi du dossier.'
        );
    END LOOP;

    -- DOSSIERS (300)
    FOR i IN 1..300 LOOP
        v_agence_id := v_agence_ids[1 + floor(random() * array_length(v_agence_ids, 1))::int];
        v_client_id := v_client_ids[1 + floor(random() * array_length(v_client_ids, 1))::int];
        -- Les 20 derniers OT restent sans dossier pour tester la saisie autonome.
        v_ot_id := v_ot_ids[1 + floor(random() * (array_length(v_ot_ids, 1) - 20))::int];
        v_phase_id := v_phase_ids[1 + floor(random() * array_length(v_phase_ids, 1))::int];
        v_pays_origine := v_pays_ids[1 + floor(random() * array_length(v_pays_ids, 1))::int];
        v_pays_destination := v_pays_ids[1 + floor(random() * array_length(v_pays_ids, 1))::int];
        v_inconterme_id := v_inconterme_ids[1 + floor(random() * array_length(v_inconterme_ids, 1))::int];
        v_type_production_id := v_type_production_ids[1 + floor(random() * array_length(v_type_production_ids, 1))::int];

        INSERT INTO dossier (
            id_agence, id_utilisateur, id_client, id_type_production, numero_dossier, id_ordre_transit,
            est_groupage, id_phase, id_statut, echeance, pour_compte, id_pays_origine, id_pays_destination,
            est_prepaye, est_provisionne, provision_demandee, lieu_paiement_fret,
            depart, date_livraison, adresse_livraison, id_inconterme
        ) VALUES (
            v_agence_id,
            v_utilisateur_ids[1 + floor(random() * array_length(v_utilisateur_ids, 1))::int],
            v_client_id,
            v_type_production_id,
            (20260100000 + i)::text,
            v_ot_id,
            (random() < 0.4),
            v_phase_id,
            v_statut_ouvert_id,
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
            v_inconterme_id
        ) RETURNING id INTO v_dossier_id;

        v_dossier_ids := array_append(v_dossier_ids, v_dossier_id);
    END LOOP;

    -- CONSTATS (50)
    FOR i IN 1..50 LOOP
        INSERT INTO constat (id_dossier, id_expert_constat, id_type_constat, description)
        VALUES (
            v_dossier_ids[1 + floor(random() * array_length(v_dossier_ids, 1))::int],
            v_expert_constat_ids[1 + floor(random() * array_length(v_expert_constat_ids, 1))::int],
            v_type_constat_ids[1 + floor(random() * array_length(v_type_constat_ids, 1))::int],
            'Constat ' || i || ' : description du constat.'
        );
    END LOOP;

    -- DOCUMENTS
    FOR i IN 1..600 LOOP
        INSERT INTO document (id_dossier, id_type_document, nombre, chemin_fichier)
        VALUES (
            v_dossier_ids[1 + floor(random() * array_length(v_dossier_ids, 1))::int],
            v_type_document_ids[1 + floor(random() * array_length(v_type_document_ids, 1))::int],
            1,
            '/documents/doc_' || i || '.pdf'
        ) RETURNING id INTO v_doc_id;

        v_document_ids := array_append(v_document_ids, v_doc_id);
    END LOOP;

    -- REMISES (100)
    FOR i IN 1..100 LOOP
        INSERT INTO remise (id_dossier, id_type_document, date_remise)
        SELECT
            d.id_dossier,
            d.id_type_document,
            CURRENT_TIMESTAMP - ((i % 30) || ' days')::interval
        FROM document d
        WHERE d.id = v_document_ids[1 + floor(random() * array_length(v_document_ids, 1))::int]
        LIMIT 1;
    END LOOP;

    -- EXPEDITIONS : une par OT maritime/aerien, y compris sans dossier.
    FOR i IN 1..array_length(v_ot_ids, 1) LOOP
        v_ot_id := v_ot_ids[i];
        SELECT tp.id_mode_transport INTO v_expedition_mode_transport_id
        FROM ordre_transit ot JOIN type_production tp ON tp.id = ot.id_type_production
        WHERE ot.id = v_ot_id;
        IF v_expedition_mode_transport_id NOT IN (v_mode_transport_maritime_id, v_mode_transport_aerien_id) THEN
            CONTINUE;
        END IF;
        SELECT id INTO v_dossier_id FROM dossier WHERE id_ordre_transit = v_ot_id ORDER BY id LIMIT 1;
        v_mode_acheminement_id := v_mode_acheminement_ids[1 + floor(random() * array_length(v_mode_acheminement_ids, 1))::int];

        IF v_expedition_mode_transport_id = v_mode_transport_maritime_id THEN
            -- Maritime
            v_compagnie_id := v_compagnie_maritime_ids[1 + floor(random() * array_length(v_compagnie_maritime_ids, 1))::int];
            SELECT id INTO v_navire_id FROM navire WHERE id_compagnie = v_compagnie_id ORDER BY random() LIMIT 1;
            v_port_emb := v_port_ids[1 + floor(random() * array_length(v_port_ids, 1))::int];
            v_port_deb := v_port_ids[1 + floor(random() * array_length(v_port_ids, 1))::int];

            INSERT INTO expedition (
                id_ordre_transit, id_dossier, est_achemine, id_mode_transport, id_mode_acheminement,
                id_compagnie, numero_bl, id_navire, date_bl, lieu_bl,
                id_port_embarquement, id_port_debarquement,
                lib_scan, date_depart, date_prevu, date_arrivee
            ) VALUES (
                v_ot_id, v_dossier_id,
                (random() < 0.8),
                v_mode_transport_maritime_id,
                v_mode_acheminement_id,
                v_compagnie_id,
                'BL-' || lpad(i::text, 6, '0'),
                v_navire_id,
                CURRENT_TIMESTAMP - ((i % 20) || ' days')::interval,
                'Port de Toamasina',
                v_port_emb,
                v_port_deb,
                'Scan ' || i,
                CURRENT_TIMESTAMP - ((i % 20) || ' days')::interval,
                CURRENT_TIMESTAMP + ((i % 20) || ' days')::interval,
                CURRENT_TIMESTAMP + ((i % 25) || ' days')::interval
            ) RETURNING id INTO v_expedition_id;
        ELSE
            -- Aérien
            v_compagnie_id := v_compagnie_aerienne_ids[1 + floor(random() * array_length(v_compagnie_aerienne_ids, 1))::int];
            v_aeroport_emb := v_aeroport_ids[1 + floor(random() * array_length(v_aeroport_ids, 1))::int];
            v_aeroport_deb := v_aeroport_ids[1 + floor(random() * array_length(v_aeroport_ids, 1))::int];

            INSERT INTO expedition (
                id_ordre_transit, id_dossier, est_achemine, id_mode_transport, id_mode_acheminement,
                id_compagnie, numero_vol, numero_lta, date_lta, lieu_lta,
                id_aeroport_embarquement, id_aeroport_debarquement,
                lib_scan, date_depart, date_prevu, date_arrivee
            ) VALUES (
                v_ot_id, v_dossier_id,
                (random() < 0.8),
                v_mode_transport_aerien_id,
                v_mode_acheminement_id,
                v_compagnie_id,
                'VOL-' || lpad(i::text, 6, '0'),
                'LTA-' || lpad(i::text, 6, '0'),
                CURRENT_TIMESTAMP - ((i % 15) || ' days')::interval,
                'Aeroport Ivato',
                v_aeroport_emb,
                v_aeroport_deb,
                'Scan ' || i,
                CURRENT_TIMESTAMP - ((i % 20) || ' days')::interval,
                CURRENT_TIMESTAMP + ((i % 20) || ' days')::interval,
                CURRENT_TIMESTAMP + ((i % 25) || ' days')::interval
            ) RETURNING id INTO v_expedition_id;
        END IF;

        v_expedition_ids := array_append(v_expedition_ids, v_expedition_id);
    END LOOP;

    -- MARCHANDISES (600) - adapté : lie désormais à l'ordre de transit (id_ordre_transit) et utilise nature_marchandise
    FOR i IN 1..600 LOOP
        INSERT INTO marchandise (
            id_ordre_transit, id_unite_mesure, id_nature_marchandise,
            marque, modele, description, poids, volume, quantite
        ) VALUES (
            v_ot_ids[1 + floor(random() * array_length(v_ot_ids, 1))::int],
            v_unite_mesure_ids[1 + floor(random() * array_length(v_unite_mesure_ids, 1))::int],
            v_nature_marchandise_ids[1 + floor(random() * array_length(v_nature_marchandise_ids, 1))::int],
            'Marque' || (i % 20),
            'Modele' || (i % 30),
            'Description marchandise ' || i,
            (random() * 5000)::numeric(10, 2),
            (random() * 100)::numeric(10, 2),
            1 + floor(random() * 100)::int
        );
    END LOOP;

    -- CONTENEURS (300)
    FOR i IN 1..300 LOOP
        v_dossier_id := v_dossier_ids[1 + floor(random() * array_length(v_dossier_ids, 1))::int];
        INSERT INTO conteneur (id_dossier, id_type_container, numero_container, description)
        VALUES (
            v_dossier_id,
            v_type_container_ids[1 + floor(random() * array_length(v_type_container_ids, 1))::int],
            'CONT-' || lpad(i::text, 7, '0'),
            'Conteneur ' || i
        ) RETURNING id INTO v_doc_id;

        INSERT INTO dossier_conteneur (id_dossier, id_conteneur)
        VALUES (v_dossier_id, v_doc_id)
        ON CONFLICT DO NOTHING;
    END LOOP;

    -- GROUPAGE (100)
    FOR i IN 1..100 LOOP
        INSERT INTO groupage (id_dossier, id_expedition, numero_groupage, partie_notifier)
        VALUES (
            v_dossier_ids[1 + floor(random() * array_length(v_dossier_ids, 1))::int],
            v_expedition_ids[1 + floor(random() * array_length(v_expedition_ids, 1))::int],
            'GRP-' || lpad(i::text, 7, '0'),
            'Partie notifier ' || i
        );
    END LOOP;

    -- DOSSIERS INTERAGENCE (40)
    FOR i IN 1..40 LOOP
        v_dossier_mere := v_dossier_ids[1 + floor(random() * array_length(v_dossier_ids, 1))::int];
        v_sous_dossier := v_dossier_ids[1 + floor(random() * array_length(v_dossier_ids, 1))::int];

        INSERT INTO dossier_interagence (id_dossier_mere, id_sous_dossier, motif)
        VALUES (
            v_dossier_mere,
            v_sous_dossier,
            'Motif interagence ' || i
        );
    END LOOP;

    -- MOUVEMENTS DE DOSSIER (1000)
    FOR i IN 1..1000 LOOP
        INSERT INTO mvt_dossier (
            id_dossier, id_utilisateur, ip_utilisateur, id_operation,
            commentaire
        ) VALUES (
            v_dossier_ids[1 + floor(random() * array_length(v_dossier_ids, 1))::int],
            v_utilisateur_ids[1 + floor(random() * array_length(v_utilisateur_ids, 1))::int],
            ('192.168.' || (i % 255) || '.' || (i % 255))::inet,
            v_operation_ids[1 + floor(random() * array_length(v_operation_ids, 1))::int],
            'Mouvement ' || i
        ) RETURNING id INTO v_mvt_id;

        IF (i % 3 = 0) THEN
            v_doc_id := v_document_ids[1 + floor(random() * array_length(v_document_ids, 1))::int];
            INSERT INTO document_mvt (id_mvt_dossier, id_document)
            VALUES (v_mvt_id, v_doc_id);
        END IF;
    END LOOP;

    -- DECLARATIONS (200)
    FOR i IN 1..200 LOOP
        INSERT INTO declaration (id_dossier, numero_declaration, date_declaration, id_bureau, id_type_declaration, description)
        VALUES (
            v_dossier_ids[1 + floor(random() * array_length(v_dossier_ids, 1))::int],
            'DEC-' || lpad(i::text, 8, '0'),
            CURRENT_TIMESTAMP - ((i % 30) || ' days')::interval,
            v_bureau_ids[1 + floor(random() * array_length(v_bureau_ids, 1))::int],
            v_type_declaration_ids[1 + floor(random() * array_length(v_type_declaration_ids, 1))::int],
            'Declaration ' || i
        );
    END LOOP;

    -- SUSPENSIONS DE CONNEXION (5)
    FOR i IN 1..5 LOOP
        INSERT INTO suspension_connexion (id_utilisateur, tentatives_echouees, date_suspension, duree_suspension_minutes)
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