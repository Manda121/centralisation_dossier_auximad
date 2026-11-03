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
        'declaration', 'dossier_interagence'
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
        WHERE id_phase IS NULL OR id_type_production IS NULL OR id_statut IS NULL
           OR id_statut = v_ouvert OR est_actif IS DISTINCT FROM TRUE
    ) THEN
        RAISE EXCEPTION 'Chaque opération active doit avoir une phase, une production et un statut précis ; Ouvert est réservé à la création.';
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

    IF (SELECT count(*) FROM ordre_transit ot WHERE NOT EXISTS (
        SELECT 1 FROM dossier d WHERE d.id_ordre_transit = ot.id
    )) < 20 THEN
        RAISE EXCEPTION 'Au moins vingt ordres de transit doivent rester sans dossier.';
    END IF;

    IF (SELECT count(DISTINCT date_trunc('month', date_creation)) FROM dossier) <> 12
       OR (SELECT count(DISTINCT date_creation::date) FROM dossier) < 30
       OR (SELECT count(DISTINCT date_creation::date) FROM dossier
           WHERE date_creation >= CURRENT_DATE - INTERVAL '29 days') < 2 THEN
        RAISE EXCEPTION 'Les créations doivent couvrir douze mois, trente jours distincts et plusieurs jours sur les trente derniers jours.';
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

    IF NOT EXISTS (SELECT 1 FROM mvt_dossier) OR EXISTS (
        SELECT 1 FROM mvt_dossier m
        JOIN dossier d ON d.id = m.id_dossier
        JOIN operation_dossier op ON op.id = m.id_operation
        WHERE op.id_type_production IS DISTINCT FROM d.id_type_production
           OR m.date_mvt IS NULL OR m.date_mvt < d.date_creation OR m.date_mvt > NOW()
           OR m.id_nouveau_statut IS DISTINCT FROM op.id_statut
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

-- Répartition visible dans le graphique annuel des créations.
SELECT to_char(date_creation, 'YYYY-MM') AS mois, count(*) AS dossiers
FROM dossier GROUP BY 1 ORDER BY 1;

-- Détail du graphique affiché par défaut sur les trente derniers jours.
SELECT date_creation::date AS jour, count(*) AS dossiers
FROM dossier WHERE date_creation >= CURRENT_DATE - INTERVAL '29 days'
GROUP BY 1 ORDER BY 1;

SELECT date_trunc('week', date_mvt)::date AS semaine, count(*) AS mouvements
FROM mvt_dossier WHERE date_mvt >= NOW() - INTERVAL '8 weeks'
GROUP BY 1 ORDER BY 1;

SELECT ph.nom AS phase, s.nom AS statut, count(*) AS dossiers
FROM dossier d JOIN phase_dossier ph ON ph.id = d.id_phase
JOIN statut_dossier s ON s.id = d.id_statut
GROUP BY ph.nom, s.nom ORDER BY ph.nom, s.nom;
