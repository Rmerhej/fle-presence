-- Installations de l'application (PWA) : un enregistrement par appareil, sans aucune donnée personnelle
-- (ni adresse IP, ni identifiant d'appareil) : seulement la date, la plateforme et l'unité éventuelle.
CREATE TABLE installation (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    unite_id     BIGINT,
    plateforme   VARCHAR(20) NOT NULL,
    installee_le DATETIME    NOT NULL,
    CONSTRAINT fk_installation_unite FOREIGN KEY (unite_id) REFERENCES unite_locale (id) ON DELETE SET NULL
);

CREATE INDEX idx_installation_date ON installation (installee_le);
