-- À exécuter sur la base existante avant de démarrer la nouvelle version.
BEGIN;

CREATE TABLE IF NOT EXISTS adresse_email_agence (
    id SERIAL PRIMARY KEY,
    id_agence INTEGER NOT NULL REFERENCES agence(id),
    email VARCHAR(254) NOT NULL,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (id_agence, email),
    CHECK (email = lower(trim(email)))
);

CREATE TABLE IF NOT EXISTS historique_mail (
    id BIGSERIAL PRIMARY KEY,
    id_demande_interagence INTEGER NOT NULL REFERENCES demande_interagence(id),
    id_agence INTEGER NOT NULL REFERENCES agence(id),
    nom_agence VARCHAR(100) NOT NULL,
    numero_dossier VARCHAR(100) NOT NULL,
    destinataire VARCHAR(254),
    expediteur VARCHAR(254),
    sujet VARCHAR(255) NOT NULL,
    corps TEXT NOT NULL,
    statut VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE'
        CHECK (statut IN ('EN_ATTENTE', 'ENVOYE', 'ECHEC', 'NON_CONFIGURE', 'ANNULE')),
    erreur TEXT,
    date_creation TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_tentative TIMESTAMPTZ,
    date_envoi TIMESTAMPTZ,
    id_mail_origine BIGINT REFERENCES historique_mail(id)
);

CREATE INDEX IF NOT EXISTS idx_historique_mail_file ON historique_mail(statut, id);
CREATE INDEX IF NOT EXISTS idx_historique_mail_agence ON historique_mail(id_agence, id DESC);
CREATE INDEX IF NOT EXISTS idx_historique_mail_demande ON historique_mail(id_demande_interagence);

COMMIT;
