-- Historique des modifications + notifications push + paramètres techniques

CREATE TABLE journal (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    date_action DATETIME     NOT NULL,
    auteur      VARCHAR(50)  NOT NULL,
    unite_id    BIGINT,
    action      VARCHAR(40)  NOT NULL,
    details     VARCHAR(500),
    CONSTRAINT fk_journal_unite FOREIGN KEY (unite_id) REFERENCES unite_locale (id)
);

CREATE INDEX idx_journal_date ON journal (date_action);

-- Abonnements aux notifications : adresse technique fournie par le navigateur, sans nom ni email
CREATE TABLE push_abonnement (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    unite_id      BIGINT        NOT NULL,
    groupe_id     BIGINT,
    endpoint      VARCHAR(1000) NOT NULL,
    endpoint_hash CHAR(64)      NOT NULL,
    p256dh        VARCHAR(255)  NOT NULL,
    auth          VARCHAR(255)  NOT NULL,
    cree_le       DATETIME      NOT NULL,
    CONSTRAINT uk_push_endpoint UNIQUE (endpoint_hash),
    CONSTRAINT fk_push_unite  FOREIGN KEY (unite_id)  REFERENCES unite_locale (id),
    CONSTRAINT fk_push_groupe FOREIGN KEY (groupe_id) REFERENCES groupe (id)
);

-- Paramètres générés par l'application (clés des notifications)
CREATE TABLE parametre (
    cle    VARCHAR(100)  NOT NULL PRIMARY KEY,
    valeur VARCHAR(2000) NOT NULL
);
