-- Schéma initial FLE Présence (MySQL 8)

CREATE TABLE unite_locale (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom     VARCHAR(100) NOT NULL,
    slug    VARCHAR(100) NOT NULL,
    adresse VARCHAR(255),
    active  BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_unite_nom  UNIQUE (nom),
    CONSTRAINT uk_unite_slug UNIQUE (slug)
);

CREATE TABLE groupe (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom      VARCHAR(100) NOT NULL,
    niveau   VARCHAR(50),
    unite_id BIGINT       NOT NULL,
    actif    BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_groupe_unite FOREIGN KEY (unite_id) REFERENCES unite_locale (id)
);

CREATE TABLE creneau (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    groupe_id    BIGINT      NOT NULL,
    jour_semaine VARCHAR(10) NOT NULL,
    heure_debut  TIME        NOT NULL,
    heure_fin    TIME        NOT NULL,
    salle        VARCHAR(100),
    actif        BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_creneau_groupe FOREIGN KEY (groupe_id) REFERENCES groupe (id)
);

CREATE TABLE seance (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    groupe_id        BIGINT      NOT NULL,
    creneau_id       BIGINT,
    date_seance      DATE        NOT NULL,
    heure_debut      TIME        NOT NULL,
    heure_fin        TIME        NOT NULL,
    salle            VARCHAR(100),
    statut           VARCHAR(20) NOT NULL DEFAULT 'PREVUE',
    motif            VARCHAR(255),
    reportee_vers_id BIGINT,
    modifiee_le      DATETIME,
    CONSTRAINT uk_seance_groupe_date_heure UNIQUE (groupe_id, date_seance, heure_debut),
    CONSTRAINT fk_seance_groupe   FOREIGN KEY (groupe_id)        REFERENCES groupe (id),
    CONSTRAINT fk_seance_creneau  FOREIGN KEY (creneau_id)       REFERENCES creneau (id),
    CONSTRAINT fk_seance_report   FOREIGN KEY (reportee_vers_id) REFERENCES seance (id)
);

CREATE INDEX idx_seance_date ON seance (date_seance);

CREATE TABLE presence (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    seance_id     BIGINT      NOT NULL,
    nb_benevoles  INT         NOT NULL,
    nb_apprenants INT         NOT NULL,
    commentaire   VARCHAR(500),
    saisi_par     VARCHAR(50) NOT NULL,
    saisi_le      DATETIME    NOT NULL,
    modifie_par   VARCHAR(50),
    modifie_le    DATETIME,
    CONSTRAINT uk_presence_seance UNIQUE (seance_id),
    CONSTRAINT fk_presence_seance FOREIGN KEY (seance_id) REFERENCES seance (id),
    CONSTRAINT ck_presence_nombres CHECK (nb_benevoles >= 0 AND nb_apprenants >= 0)
);

CREATE TABLE utilisateur (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL,
    password    VARCHAR(100) NOT NULL,
    nom_affiche VARCHAR(100) NOT NULL,
    role        VARCHAR(20)  NOT NULL,
    unite_id    BIGINT,
    actif       BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_utilisateur_username UNIQUE (username),
    CONSTRAINT fk_utilisateur_unite FOREIGN KEY (unite_id) REFERENCES unite_locale (id)
);
