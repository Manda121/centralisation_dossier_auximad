-- À lancer après le schéma et donne_deepseek.sql dans un schéma de test isolé.
-- Ce fichier ne modifie aucune donnée et s'arrête à la première incohérence.
\set ON_ERROR_STOP on

DO $verification$
DECLARE
    v_table TEXT;
    v_preparation INTEGER;
    v_ouvert INTEGER;
BEGIN
    IF current_schema() IN ('public', 'pg_catalog', 'information_schema') THEN
        RAISE EXCEPTION 'La vérification doit utiliser un schéma de test isolé (search_path actuel : %).', current_schema();
    END IF;

    FOREACH v_table IN ARRAY ARRAY[
        'agence', 'inconterme', 'phase_dossier', 'statut_dossier',
        'type_production', 'mode_transport', 'operation_dossier', 'ordre_transit',
        'dossier', 'mvt_dossier', 'document', 'type_document', 'document_mvt',
        'document_obligatoire_mvt', 'remise', 'utilisateur', 'clients',
        'declaration', 'dossier_interagence', 'demande_interagence', 'conteneur',
        'dossier_conteneur', 'expedition', 'compagnie', 'navire', 'station', 'groupage',
        'marchandise', 'type_regime', 'statut_ot', 'pays', 'historique_mail'
    ] LOOP
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.tables
            WHERE table_schema = current_schema() AND table_name = v_table
        ) THEN
            RAISE EXCEPTION 'Table % absente du schéma de test %.', v_table, current_schema();
        END IF;
    END LOOP;

    IF (SELECT count(*) FROM agence) <> 9
       OR EXISTS (SELECT 1 FROM agence WHERE lower(nom) LIKE '%manakara%' OR lower(ville) LIKE '%manakara%') THEN
        RAISE EXCEPTION 'Le jeu doit contenir neuf agences et aucune agence de Manakara.';
    END IF;

    IF (SELECT count(*) FROM inconterme) <> 1
       OR NOT EXISTS (SELECT 1 FROM inconterme WHERE abreviation = 'FOB') THEN
        RAISE EXCEPTION 'FOB doit être le seul incoterm du jeu.';
    END IF;

    IF (SELECT count(*) FROM phase_dossier) <> 3
       OR (SELECT count(*) FROM phase_dossier WHERE nom IN ('PREPARATION', 'TRAITEMENT', 'COMPTABILITE')) <> 3 THEN
        RAISE EXCEPTION 'Les seules phases attendues sont PREPARATION, TRAITEMENT et COMPTABILITE.';
    END IF;

    SELECT id INTO STRICT v_preparation FROM phase_dossier WHERE nom = 'PREPARATION';
    SELECT id INTO STRICT v_ouvert FROM statut_dossier WHERE nom = 'Ouvert';

    IF (SELECT array_agg(nom::text ORDER BY id) FROM statut_dossier) IS DISTINCT FROM ARRAY[
        'Ouvert', 'Client avisé', 'Réservation confirmée', 'Documents reçus',
        'Conteneur positionné', 'Expertise réalisée', 'Déclaration établie',
        'Visite/scan effectué', 'Droits et taxes payés', 'PG payé', 'Bon à enlever obtenu',
        'Provision enregistrée', 'Facturé', 'Payé', 'Clôturé', 'En litige', 'Annulé'
    ]::text[] THEN
        RAISE EXCEPTION 'Les dix-sept statuts doivent correspondre à la liste retenue et être insérés dans leur ordre de déroulement.';
    END IF;

    IF (SELECT count(*) FROM operation_dossier) <> 64 THEN
        RAISE EXCEPTION '64 opérations attendues : import 15 x 2 modes et export 17 x 2 modes.';
    END IF;

    IF (SELECT count(*) FROM type_production tp JOIN mode_transport mt ON mt.id = tp.id_mode_transport
        WHERE tp.nom IN ('import', 'export') AND mt.nom IN ('Maritime', 'Aerien')) <> 4 THEN
        RAISE EXCEPTION 'Les quatre productions import/export maritime/aérien sont nécessaires.';
    END IF;

    IF EXISTS (
        SELECT tp.id FROM type_production tp
        JOIN mode_transport mt ON mt.id = tp.id_mode_transport
        LEFT JOIN operation_dossier op ON op.id_type_production = tp.id
        WHERE tp.nom IN ('import', 'export') AND mt.nom IN ('Maritime', 'Aerien')
        GROUP BY tp.id, tp.nom
        HAVING count(op.id) <> CASE WHEN tp.nom = 'import' THEN 15 ELSE 17 END
           OR count(DISTINCT op.niveau_operation) <> count(op.id)
           OR min(op.niveau_operation) <> 1
           OR max(op.niveau_operation) <> count(op.id)
    ) THEN
        RAISE EXCEPTION 'Les opérations doivent former les rangs uniques 1..15 (import) ou 1..17 (export), par production.';
    END IF;

    IF EXISTS (
        SELECT 1 FROM operation_dossier
        WHERE id_phase IS NULL OR id_type_production IS NULL
           OR id_statut = v_ouvert OR est_actif IS DISTINCT FROM TRUE
    ) THEN
        RAISE EXCEPTION 'Chaque opération active doit avoir une phase et une production ; Ouvert est réservé à la création.';
    END IF;

    IF EXISTS (
        SELECT 1 FROM operation_dossier
        WHERE (description IN ('Envoi proforma', 'Premier envoi document', 'Premier réception document',
            'Dispatche/Passeur', 'Confirmation paiement Droit & Taxes', 'Transmission',
            'Préalerte', 'Dépôt Déclaration', 'Récupération documents')) IS DISTINCT FROM (id_statut IS NULL)
    ) THEN
        RAISE EXCEPTION 'Les opérations dont le statut a été retiré doivent conserver le statut précédent ; les autres doivent garder leur statut associé.';
    END IF;

    -- Les listes de documents et les KPI applicatifs filtrent par production :
    -- un type documentaire générique (production NULL) serait invisible ici.
    IF EXISTS (
        SELECT 1 FROM document_obligatoire_mvt obligation
        JOIN operation_dossier op ON op.id = obligation.id_operation
        JOIN type_document td ON td.id = obligation.id_type_document
        WHERE td.id_type_production IS DISTINCT FROM op.id_type_production
    ) THEN
        RAISE EXCEPTION 'Les documents obligatoires d''une opération doivent avoir la même production que cette opération pour apparaître dans les listes et indicateurs.';
    END IF;

    IF EXISTS (
        SELECT 1 FROM type_production tp
        JOIN mode_transport mt ON mt.id = tp.id_mode_transport
        CROSS JOIN (VALUES ('Facture commerciale'), ('Declaration en douane'), ('Facture proforma')) AS famille(nom)
        WHERE tp.nom IN ('import', 'export') AND mt.nom IN ('Maritime', 'Aerien')
          AND NOT EXISTS (
              SELECT 1 FROM type_document td
              WHERE td.id_type_production = tp.id AND td.nom LIKE famille.nom || '%'
          )
    ) THEN
        RAISE EXCEPTION 'Chaque production import/export maritime/aérien doit proposer sa facture commerciale, sa déclaration en douane et sa facture proforma.';
    END IF;

    IF (SELECT count(*) FROM dossier) <> 300 OR (SELECT count(*) FROM ordre_transit) <> 320 THEN
        RAISE EXCEPTION 'Le jeu doit contenir 300 dossiers et 320 ordres de transit.';
    END IF;

    -- La capitale est comptée une seule fois : TNR et DIR partagent son quota.
    IF EXISTS (
        SELECT attendue.ville
        FROM (VALUES
            ('Antananarivo', 152), ('Toamasina', 39), ('Antsirabe', 30),
            ('Mahajanga', 29), ('Toliara', 20), ('Antsiranana', 16),
            ('Fort-Dauphin', 8), ('Nosy Be', 6)
        ) AS attendue(ville, quota)
        LEFT JOIN agence a ON a.ville = attendue.ville
        LEFT JOIN dossier d ON d.id_agence = a.id
        GROUP BY attendue.ville, attendue.quota
        HAVING count(d.id) <> attendue.quota
    ) THEN
        RAISE EXCEPTION 'Les dossiers doivent respecter les quotas démographiques des huit villes, sans compter Antananarivo deux fois.';
    END IF;

    IF EXISTS (
        SELECT attendue.code
        FROM (VALUES
            (20, 137), (90, 15), (10, 39), (21, 30), (30, 29),
            (50, 20), (70, 16), (93, 8), (80, 6)
        ) AS attendue(code, quota)
        LEFT JOIN agence a ON a.code = attendue.code
        LEFT JOIN dossier d ON d.id_agence = a.id
        GROUP BY attendue.code, attendue.quota
        HAVING count(d.id) <> attendue.quota
    ) THEN
        RAISE EXCEPTION 'Les quotas par agence doivent être respectés, dont 137 dossiers TNR et 15 DIR pour Antananarivo.';
    END IF;

    IF (SELECT count(*) FROM dossier d JOIN agence a ON a.id = d.id_agence WHERE a.code = 20)
        <= (SELECT max(total) FROM (
                SELECT count(d.id) AS total FROM agence a LEFT JOIN dossier d ON d.id_agence = a.id
                WHERE a.code <> 20 GROUP BY a.id
            ) autres_agences) THEN
        RAISE EXCEPTION 'TNR doit être l''agence ayant le plus de dossiers.';
    END IF;

    IF (SELECT count(*) FROM ordre_transit ot WHERE NOT EXISTS (
        SELECT 1 FROM dossier d WHERE d.id_ordre_transit = ot.id
    )) < 20 THEN
        RAISE EXCEPTION 'Au moins vingt ordres de transit doivent rester sans dossier.';
    END IF;

    IF (SELECT count(DISTINCT date_trunc('month', date_creation)) FROM dossier) <> 12
       OR (SELECT count(DISTINCT date_creation::date) FROM dossier) < 220
       OR (SELECT count(DISTINCT date_creation::date) FROM dossier
           WHERE date_creation >= CURRENT_DATE - INTERVAL '29 days') < 2 THEN
        RAISE EXCEPTION 'Les créations doivent couvrir douze mois, au moins 220 jours distincts et plusieurs jours sur les trente derniers jours.';
    END IF;

    IF EXISTS (
        SELECT date_creation::date FROM dossier
        GROUP BY date_creation::date HAVING count(*) > 3
    ) THEN
        RAISE EXCEPTION 'Les créations doivent être lissées : au maximum trois dossiers par jour.';
    END IF;

    IF (SELECT count(*) FROM dossier WHERE date_creation >= CURRENT_DATE - INTERVAL '29 days') NOT BETWEEN 20 AND 35 THEN
        RAISE EXCEPTION 'Entre vingt et trente-cinq créations sont attendues sur les trente derniers jours, sans pic artificiel dans le mois courant.';
    END IF;

    IF EXISTS (
        SELECT 1 FROM dossier d
        LEFT JOIN ordre_transit ot ON ot.id = d.id_ordre_transit
        WHERE d.date_creation IS NULL OR d.date_creation > NOW()
           OR ot.id IS NULL OR ot.date_creation IS NULL OR ot.date_creation > d.date_creation
           OR d.id_type_production IS DISTINCT FROM ot.id_type_production
           OR d.id_client IS DISTINCT FROM ot.id_client
    ) THEN
        RAISE EXCEPTION 'Chaque dossier doit conserver le client et la production de son OT, créé au plus tard à la création du dossier.';
    END IF;

    IF (SELECT count(*) FROM dossier_interagence) <> 40
       OR (SELECT count(DISTINCT id_sous_dossier) FROM dossier_interagence) <> 40
       OR EXISTS (
           SELECT 1 FROM dossier_interagence di
           JOIN dossier mere ON mere.id = di.id_dossier_mere
           JOIN dossier sous ON sous.id = di.id_sous_dossier
           WHERE mere.id = sous.id
              OR mere.id_agence IS NOT DISTINCT FROM sous.id_agence
              OR mere.id_ordre_transit IS DISTINCT FROM sous.id_ordre_transit
              OR mere.id_client IS DISTINCT FROM sous.id_client
              OR mere.id_type_production IS DISTINCT FROM sous.id_type_production
              OR sous.date_creation < mere.date_creation
       ) THEN
        RAISE EXCEPTION 'Les quarante sous-dossiers doivent partager OT, client et production de leur dossier mère, dans une autre agence, sans antériorité.';
    END IF;

    IF EXISTS (
        SELECT 1 FROM ordre_transit ot
        JOIN type_production tp ON tp.id = ot.id_type_production
        JOIN type_regime r ON r.id = ot.id_type_regime
        WHERE (tp.nom = 'import' AND r.code <> 'MCI') OR (tp.nom = 'export' AND r.code <> 'EXP')
    ) THEN
        RAISE EXCEPTION 'Les régimes des OT import et export doivent correspondre au sens de la production.';
    END IF;

    IF (SELECT count(*) FROM conteneur) <> (
        SELECT count(*) FROM dossier d JOIN type_production tp ON tp.id = d.id_type_production
        JOIN mode_transport mt ON mt.id = tp.id_mode_transport WHERE mt.nom = 'Maritime'
    ) OR EXISTS (
        SELECT 1 FROM conteneur c
        JOIN dossier d ON d.id = c.id_dossier
        JOIN type_production tp ON tp.id = d.id_type_production
        JOIN mode_transport mt ON mt.id = tp.id_mode_transport
        WHERE mt.nom <> 'Maritime' OR NOT EXISTS (
            SELECT 1 FROM dossier_conteneur dc WHERE dc.id_conteneur = c.id AND dc.id_dossier = d.id
        )
    ) OR EXISTS (
        SELECT 1 FROM dossier_conteneur dc JOIN dossier d ON d.id = dc.id_dossier
        JOIN type_production tp ON tp.id = d.id_type_production
        JOIN mode_transport mt ON mt.id = tp.id_mode_transport WHERE mt.nom <> 'Maritime'
    ) THEN
        RAISE EXCEPTION 'Seuls les dossiers maritimes doivent recevoir un conteneur et une association cohérente.';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM dossier d JOIN type_production tp ON tp.id = d.id_type_production
        JOIN mode_transport mt ON mt.id = tp.id_mode_transport
        WHERE mt.nom = 'Maritime' AND d.date_creation::date = CURRENT_DATE
    ) THEN
        RAISE EXCEPTION 'Un dossier maritime créé aujourd''hui doit permettre de tester les assignations du jour.';
    END IF;

    IF EXISTS (
        SELECT 1 FROM ordre_transit ot JOIN statut_ot st ON st.id = ot.id_statut_ot
        WHERE st.nom <> 'Valide' AND (
            EXISTS (SELECT 1 FROM expedition e WHERE e.id_ordre_transit = ot.id)
            OR EXISTS (SELECT 1 FROM marchandise m WHERE m.id_ordre_transit = ot.id)
        )
    ) THEN
        RAISE EXCEPTION 'Les OT encore en brouillon ou non validés ne doivent pas avoir de marchandise ni d''expédition affectée.';
    END IF;

    IF EXISTS (
        SELECT 1 FROM ordre_transit ot
        JOIN type_production tp ON tp.id = ot.id_type_production
        JOIN mode_transport mt ON mt.id = tp.id_mode_transport
        JOIN statut_ot st ON st.id = ot.id_statut_ot
        WHERE st.nom = 'Valide' AND mt.nom IN ('Maritime', 'Aerien')
          AND NOT EXISTS (SELECT 1 FROM expedition e WHERE e.id_ordre_transit = ot.id)
    ) OR EXISTS (
        SELECT 1 FROM expedition e
        JOIN ordre_transit ot ON ot.id = e.id_ordre_transit
        JOIN type_production tp ON tp.id = ot.id_type_production
        JOIN mode_transport mt ON mt.id = tp.id_mode_transport
        JOIN compagnie c ON c.id = e.id_compagnie
        LEFT JOIN navire n ON n.id = e.id_navire
        LEFT JOIN dossier d ON d.id = e.id_dossier
        LEFT JOIN station depart ON depart.id = coalesce(e.id_port_embarquement, e.id_aeroport_embarquement)
        LEFT JOIN station arrivee ON arrivee.id = coalesce(e.id_port_debarquement, e.id_aeroport_debarquement)
        WHERE e.id_mode_transport IS DISTINCT FROM tp.id_mode_transport
           OR c.id_mode_transport IS DISTINCT FROM e.id_mode_transport
           OR (n.id IS NOT NULL AND n.id_compagnie IS DISTINCT FROM c.id)
           OR (d.id IS NOT NULL AND d.id_ordre_transit IS DISTINCT FROM ot.id)
           OR depart.id IS NULL OR arrivee.id IS NULL OR depart.id = arrivee.id
           OR depart.id_mode_transport IS DISTINCT FROM e.id_mode_transport
           OR arrivee.id_mode_transport IS DISTINCT FROM e.id_mode_transport
           OR (mt.nom = 'Maritime' AND (n.id IS NULL OR e.numero_bl IS NULL
               OR e.id_port_embarquement IS NULL OR e.id_port_debarquement IS NULL
               OR e.numero_lta IS NOT NULL OR e.numero_vol IS NOT NULL OR e.date_lta IS NOT NULL
               OR e.lieu_lta IS NOT NULL OR e.id_aeroport_embarquement IS NOT NULL
               OR e.id_aeroport_debarquement IS NOT NULL OR e.lieu_bl IS DISTINCT FROM depart.nom))
           OR (mt.nom = 'Aerien' AND (e.numero_lta IS NULL OR e.numero_vol IS NULL
               OR e.id_aeroport_embarquement IS NULL OR e.id_aeroport_debarquement IS NULL
               OR e.id_navire IS NOT NULL OR e.numero_bl IS NOT NULL OR e.date_bl IS NOT NULL
               OR e.lieu_bl IS NOT NULL OR e.id_port_embarquement IS NOT NULL
               OR e.id_port_debarquement IS NOT NULL OR e.lieu_lta IS DISTINCT FROM depart.nom))
           OR e.date_depart < ot.date_creation OR e.date_depart > NOW()
           OR e.date_prevu < e.date_depart OR e.date_arrivee < e.date_depart OR e.date_arrivee > NOW()
           OR e.est_achemine IS DISTINCT FROM (e.date_arrivee IS NOT NULL)
    ) THEN
        RAISE EXCEPTION 'Les expéditions doivent utiliser le transport, la compagnie, le navire, les stations et les dates cohérents avec leur OT.';
    END IF;

    IF EXISTS (
        SELECT 1 FROM dossier d JOIN type_production tp ON tp.id = d.id_type_production
        JOIN pays origine ON origine.id = d.id_pays_origine
        JOIN pays destination ON destination.id = d.id_pays_destination
        WHERE (tp.nom = 'import' AND (origine.code = 'MDG' OR destination.code <> 'MDG'))
           OR (tp.nom = 'export' AND (origine.code <> 'MDG' OR destination.code = 'MDG'))
    ) OR EXISTS (
        SELECT 1 FROM expedition e
        JOIN ordre_transit ot ON ot.id = e.id_ordre_transit
        JOIN type_production tp ON tp.id = ot.id_type_production
        JOIN station depart ON depart.id = coalesce(e.id_port_embarquement, e.id_aeroport_embarquement)
        JOIN station arrivee ON arrivee.id = coalesce(e.id_port_debarquement, e.id_aeroport_debarquement)
        WHERE (depart.code IN ('PTOM', 'PDAN', 'PMAH', 'PTOL', 'TNR', 'NOS', 'TMM'))
                   IS DISTINCT FROM (tp.nom = 'export')
           OR (arrivee.code IN ('PTOM', 'PDAN', 'PMAH', 'PTOL', 'TNR', 'NOS', 'TMM'))
                   IS DISTINCT FROM (tp.nom = 'import')
    ) THEN
        RAISE EXCEPTION 'Les trajets et pays doivent respecter import vers Madagascar / export depuis Madagascar.';
    END IF;

    IF EXISTS (
        SELECT 1 FROM groupage g JOIN dossier d ON d.id = g.id_dossier
        JOIN expedition e ON e.id = g.id_expedition
        WHERE e.id_ordre_transit IS DISTINCT FROM d.id_ordre_transit
           OR d.est_groupage IS DISTINCT FROM TRUE
           OR g.date_creation < d.date_creation OR g.date_creation > NOW()
    ) THEN
        RAISE EXCEPTION 'Les groupages doivent relier un dossier à l''expédition de son propre OT.';
    END IF;

    IF (SELECT count(*) FROM demande_interagence) <> 80
       OR (SELECT count(*) FROM demande_interagence WHERE statut = 'ACCEPTEE') <> 40
       OR (SELECT count(*) FROM demande_interagence WHERE statut = 'EN_ATTENTE') <> 20
       OR (SELECT count(*) FROM demande_interagence WHERE statut = 'REFUSEE') <> 20
       OR EXISTS (SELECT id_dossier FROM demande_interagence WHERE statut = 'EN_ATTENTE'
                  GROUP BY id_dossier HAVING count(*) > 1) THEN
        RAISE EXCEPTION '80 demandes sont attendues : 40 acceptées, 20 en attente, 20 refusées, sans doublon en attente.';
    END IF;

    IF EXISTS (
        SELECT 1 FROM demande_interagence di
        JOIN dossier d ON d.id = di.id_dossier
        JOIN utilisateur auteur ON auteur.id = di.id_utilisateur_demande
        LEFT JOIN utilisateur repondant ON repondant.id = di.id_utilisateur_reponse
        WHERE di.id_agence_demandeuse = di.id_agence_destinataire
           OR di.id_agence_demandeuse IS DISTINCT FROM d.id_agence
           OR auteur.id_agence IS DISTINCT FROM di.id_agence_demandeuse
           OR di.date_demande < d.date_creation OR di.date_demande > NOW()
           OR (di.statut = 'EN_ATTENTE' AND (di.date_reponse IS NOT NULL
               OR di.id_utilisateur_reponse IS NOT NULL OR di.reponse_motif IS NOT NULL))
           OR (di.statut IN ('ACCEPTEE', 'REFUSEE') AND (di.date_reponse IS NULL
               OR di.date_reponse < di.date_demande OR di.date_reponse > NOW()
               OR repondant.id_agence IS DISTINCT FROM di.id_agence_destinataire
               OR di.est_vue IS DISTINCT FROM TRUE))
    ) THEN
        RAISE EXCEPTION 'Les agences, auteurs, réponses et dates des demandes interagences doivent être cohérents.';
    END IF;

    IF EXISTS (
        SELECT 1 FROM demande_interagence di WHERE di.statut = 'ACCEPTEE' AND NOT EXISTS (
            SELECT 1 FROM dossier_interagence lien JOIN dossier sous ON sous.id = lien.id_sous_dossier
            WHERE lien.id_dossier_mere = di.id_dossier AND sous.id_agence = di.id_agence_destinataire
              AND sous.date_creation = di.date_reponse AND lien.date_creation = di.date_reponse
        )
    ) OR EXISTS (
        SELECT 1 FROM dossier_interagence lien JOIN dossier sous ON sous.id = lien.id_sous_dossier
        WHERE NOT EXISTS (SELECT 1 FROM demande_interagence di
            WHERE di.statut = 'ACCEPTEE' AND di.id_dossier = lien.id_dossier_mere
              AND di.id_agence_destinataire = sous.id_agence AND di.date_reponse = sous.date_creation)
    ) OR EXISTS (SELECT 1 FROM historique_mail) THEN
        RAISE EXCEPTION 'Chaque sous-dossier doit provenir d''une demande acceptée ; aucun email fictif ne doit être mis en file.';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM mvt_dossier) OR EXISTS (
        SELECT 1 FROM mvt_dossier m
        JOIN dossier d ON d.id = m.id_dossier
        JOIN operation_dossier op ON op.id = m.id_operation
        WHERE op.id_type_production IS DISTINCT FROM d.id_type_production
           OR m.date_mvt IS NULL OR m.date_mvt < d.date_creation OR m.date_mvt > NOW()
           OR m.id_nouveau_statut IS DISTINCT FROM COALESCE(op.id_statut, m.id_ancien_statut)
           OR m.id_ancienne_phase IS NULL OR m.id_nouvelle_phase IS NULL
           OR m.id_ancien_statut IS NULL OR m.id_nouveau_statut IS NULL
    ) THEN
        RAISE EXCEPTION 'Les mouvements doivent suivre le type du dossier, le statut de leur opération et une chronologie dossier <= mouvement <= maintenant.';
    END IF;

    IF EXISTS (
        SELECT 1 FROM (
            SELECT m.*,
                lag(m.id_nouvelle_phase, 1, v_preparation) OVER w AS phase_attendue,
                lag(m.id_nouveau_statut, 1, v_ouvert) OVER w AS statut_attendu,
                lag(m.date_mvt) OVER w AS date_precedente,
                lag(op.niveau_operation) OVER w AS rang_precedent,
                op.niveau_operation AS rang
            FROM mvt_dossier m JOIN operation_dossier op ON op.id = m.id_operation
            WHERE NOT coalesce(m.est_annule, FALSE)
            WINDOW w AS (PARTITION BY m.id_dossier ORDER BY m.id)
        ) chaine
        WHERE id_ancienne_phase IS DISTINCT FROM phase_attendue
           OR id_ancien_statut IS DISTINCT FROM statut_attendu
           OR date_mvt < date_precedente OR rang <= rang_precedent
    ) THEN
        RAISE EXCEPTION 'La chaîne des mouvements doit conserver les anciennes phases/statuts et progresser dans l''ordre des opérations et des dates.';
    END IF;

    IF EXISTS (
        SELECT 1 FROM dossier d
        LEFT JOIN LATERAL (
            SELECT m.id_nouvelle_phase, m.id_nouveau_statut FROM mvt_dossier m
            WHERE m.id_dossier = d.id AND NOT coalesce(m.est_annule, FALSE)
            ORDER BY m.id DESC LIMIT 1
        ) dernier ON TRUE
        WHERE d.id_phase IS DISTINCT FROM coalesce(dernier.id_nouvelle_phase, v_preparation)
           OR d.id_statut IS DISTINCT FROM coalesce(dernier.id_nouveau_statut, v_ouvert)
    ) THEN
        RAISE EXCEPTION 'La phase et le statut de chaque dossier doivent correspondre à son dernier mouvement, ou à PREPARATION/Ouvert sans mouvement.';
    END IF;

    IF EXISTS (
        SELECT 1 FROM document_mvt dm
        JOIN document doc ON doc.id = dm.id_document
        JOIN mvt_dossier m ON m.id = dm.id_mvt_dossier
        WHERE doc.id_dossier IS DISTINCT FROM m.id_dossier OR doc.date_ajout > m.date_mvt
    ) THEN
        RAISE EXCEPTION 'Les documents joints à un mouvement doivent appartenir au même dossier et exister à la date du mouvement.';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM document_obligatoire_mvt) OR EXISTS (
        SELECT 1 FROM mvt_dossier m
        JOIN document_obligatoire_mvt obligation ON obligation.id_operation = m.id_operation
        WHERE NOT coalesce(m.est_annule, FALSE) AND NOT EXISTS (
            SELECT 1 FROM document doc
            WHERE doc.id_dossier = m.id_dossier
              AND doc.id_type_document = obligation.id_type_document
              AND doc.est_actif = TRUE AND doc.date_ajout <= m.date_mvt
              AND EXISTS (
                  SELECT 1 FROM remise r
                  WHERE r.id_dossier = m.id_dossier AND r.id_type_document = doc.id_type_document
                    AND r.date_remise <= m.date_mvt
              )
        )
    ) THEN
        RAISE EXCEPTION 'Les documents obligatoires des opérations effectuées doivent être présents, actifs et remis avant le mouvement.';
    END IF;

    IF EXISTS (SELECT 1 FROM utilisateur WHERE date_creation > NOW() OR date_modification > NOW())
       OR EXISTS (SELECT 1 FROM clients WHERE date_creation > NOW())
       OR EXISTS (SELECT 1 FROM ordre_transit WHERE date_creation > NOW())
       OR EXISTS (SELECT 1 FROM document WHERE date_ajout > NOW())
       OR EXISTS (SELECT 1 FROM remise WHERE date_remise > NOW())
       OR EXISTS (SELECT 1 FROM declaration WHERE date_ajout > NOW() OR date_declaration > NOW()) THEN
        RAISE EXCEPTION 'Les utilisateurs, clients, OT, ajouts de documents, remises et déclarations ne doivent pas être datés dans le futur.';
    END IF;

    RAISE NOTICE 'Vérifications du jeu donne_deepseek réussies dans le schéma %.', current_schema();
END;
$verification$;

-- Les deux agences de la capitale se partagent la population d'Antananarivo.
SELECT a.ville, count(*) AS dossiers,
       round(count(*) * 100.0 / (SELECT count(*) FROM dossier), 2) AS part_pourcent
FROM dossier d JOIN agence a ON a.id = d.id_agence
GROUP BY a.ville ORDER BY dossiers DESC, a.ville;

SELECT a.code, a.nom AS agence, a.ville, count(*) AS dossiers,
       round(count(*) * 100.0 / (SELECT count(*) FROM dossier), 2) AS part_pourcent
FROM dossier d JOIN agence a ON a.id = d.id_agence
GROUP BY a.id, a.code, a.nom, a.ville ORDER BY dossiers DESC, a.code;

-- Répartition visible dans le graphique annuel des créations.
SELECT to_char(date_creation, 'YYYY-MM') AS mois, count(*) AS dossiers
FROM dossier GROUP BY 1 ORDER BY 1;

-- Détail du graphique affiché par défaut, avec les jours sans création inclus.
SELECT j.jour::date AS jour, count(d.id) AS dossiers
FROM generate_series(CURRENT_DATE - INTERVAL '29 days', CURRENT_DATE, INTERVAL '1 day') AS j(jour)
LEFT JOIN dossier d ON d.date_creation >= j.jour AND d.date_creation < j.jour + INTERVAL '1 day'
GROUP BY j.jour ORDER BY j.jour;

SELECT date_trunc('week', date_mvt)::date AS semaine, count(*) AS mouvements
FROM mvt_dossier WHERE date_mvt >= NOW() - INTERVAL '8 weeks'
GROUP BY 1 ORDER BY 1;

SELECT ph.nom AS phase, s.nom AS statut, count(*) AS dossiers
FROM dossier d JOIN phase_dossier ph ON ph.id = d.id_phase
JOIN statut_dossier s ON s.id = d.id_statut
GROUP BY ph.nom, s.nom ORDER BY ph.nom, s.nom;

SELECT mt.nom AS transport, count(d.id) AS dossiers, count(c.id) AS conteneurs
FROM dossier d JOIN type_production tp ON tp.id = d.id_type_production
JOIN mode_transport mt ON mt.id = tp.id_mode_transport
LEFT JOIN conteneur c ON c.id_dossier = d.id
GROUP BY mt.nom ORDER BY mt.nom;

SELECT statut, count(*) AS demandes FROM demande_interagence GROUP BY statut ORDER BY statut;
