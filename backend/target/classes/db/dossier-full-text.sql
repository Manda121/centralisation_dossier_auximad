-- Keep identifiers separated by hyphens searchable as individual tokens.
CREATE OR REPLACE FUNCTION auximad_vecteur(texte text)
RETURNS tsvector LANGUAGE sql IMMUTABLE PARALLEL SAFE AS $$
    SELECT to_tsvector('simple', regexp_replace(COALESCE(texte, ''), '[^[:alnum:]]+', ' ', 'g'));
$$;

-- Indexed full-text searches for dossier filters and the global search.
-- Safe to run again: only missing vectors are backfilled.
CREATE OR REPLACE FUNCTION auximad_recherche_prefixe(texte text)
RETURNS tsquery LANGUAGE sql IMMUTABLE PARALLEL SAFE AS $$
    SELECT to_tsquery('simple', COALESCE(string_agg(quote_literal(lexeme) || ':*', ' & '), ''))
    FROM unnest(tsvector_to_array(auximad_vecteur(COALESCE(texte, '')))) AS mots(lexeme);
$$;

ALTER TABLE dossier ADD COLUMN IF NOT EXISTS recherche_fts tsvector;

CREATE OR REPLACE FUNCTION auximad_dossier_vecteur(
    numero text, compte text, lieu_depart text, livraison text, paiement text,
    client_nom text, client_prenom text, client_cp text)
RETURNS tsvector LANGUAGE sql IMMUTABLE PARALLEL SAFE AS $$
    SELECT auximad_vecteur(COALESCE(numero, '') || ' ' || COALESCE(compte, '') || ' ' ||
        COALESCE(lieu_depart, '') || ' ' || COALESCE(livraison, '') || ' ' ||
        COALESCE(paiement, '') || ' ' || COALESCE(client_nom, '') || ' ' ||
        COALESCE(client_prenom, '') || ' ' || COALESCE(client_cp, ''));
$$;

CREATE OR REPLACE FUNCTION auximad_dossier_actualiser_fts()
RETURNS trigger LANGUAGE plpgsql SET search_path FROM CURRENT AS $$
DECLARE client_ligne record;
BEGIN
    SELECT nom, prenom, cp_client INTO client_ligne FROM clients WHERE id = NEW.id_client;
    NEW.recherche_fts := auximad_dossier_vecteur(NEW.numero_dossier, NEW.pour_compte,
        NEW.depart, NEW.adresse_livraison, NEW.lieu_paiement_fret,
        client_ligne.nom, client_ligne.prenom, client_ligne.cp_client);
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS dossier_actualiser_fts ON dossier;
CREATE TRIGGER dossier_actualiser_fts
BEFORE INSERT OR UPDATE OF numero_dossier, pour_compte, depart, adresse_livraison,
    lieu_paiement_fret, id_client ON dossier
FOR EACH ROW EXECUTE FUNCTION auximad_dossier_actualiser_fts();

CREATE OR REPLACE FUNCTION auximad_client_actualiser_dossiers_fts()
RETURNS trigger LANGUAGE plpgsql SET search_path FROM CURRENT AS $$
BEGIN
    IF (NEW.nom, NEW.prenom, NEW.cp_client) IS DISTINCT FROM (OLD.nom, OLD.prenom, OLD.cp_client) THEN
        UPDATE dossier SET recherche_fts = auximad_dossier_vecteur(numero_dossier, pour_compte,
            depart, adresse_livraison, lieu_paiement_fret, NEW.nom, NEW.prenom, NEW.cp_client)
        WHERE id_client = NEW.id;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS client_actualiser_dossiers_fts ON clients;
CREATE TRIGGER client_actualiser_dossiers_fts
AFTER UPDATE OF nom, prenom, cp_client ON clients
FOR EACH ROW EXECUTE FUNCTION auximad_client_actualiser_dossiers_fts();

UPDATE dossier d SET recherche_fts = auximad_dossier_vecteur(d.numero_dossier, d.pour_compte,
    d.depart, d.adresse_livraison, d.lieu_paiement_fret, c.nom, c.prenom, c.cp_client)
FROM clients c WHERE c.id = d.id_client AND d.recherche_fts IS NULL;

CREATE INDEX IF NOT EXISTS idx_dossier_recherche_fts ON dossier USING GIN (recherche_fts);
CREATE INDEX IF NOT EXISTS idx_dossier_numerodossier_fts ON dossier USING GIN (auximad_vecteur(COALESCE(numero_dossier, '')));
CREATE INDEX IF NOT EXISTS idx_clients_clientnom_fts ON clients USING GIN (auximad_vecteur(COALESCE(nom, '') || ' ' || COALESCE(prenom, '')));
CREATE INDEX IF NOT EXISTS idx_clients_cpclient_fts ON clients USING GIN (auximad_vecteur(COALESCE(cp_client, '')));
CREATE INDEX IF NOT EXISTS idx_dossier_pourcompte_fts ON dossier USING GIN (auximad_vecteur(COALESCE(pour_compte, '')));
CREATE INDEX IF NOT EXISTS idx_dossier_depart_fts ON dossier USING GIN (auximad_vecteur(COALESCE(depart, '')));
CREATE INDEX IF NOT EXISTS idx_dossier_adresselivraison_fts ON dossier USING GIN (auximad_vecteur(COALESCE(adresse_livraison, '')));
CREATE INDEX IF NOT EXISTS idx_dossier_lieupaiementfret_fts ON dossier USING GIN (auximad_vecteur(COALESCE(lieu_paiement_fret, '')));
