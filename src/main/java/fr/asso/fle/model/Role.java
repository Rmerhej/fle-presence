package fr.asso.fle.model;

/**
 * Rôles des personnes qui se connectent.
 * Les apprenants n'ont pas de compte : ils consultent la page publique de leur unité.
 */
public enum Role {
    ADMIN("Administrateur"),
    RESPONSABLE("Responsable d'unité"),
    BENEVOLE("Bénévole");

    private final String libelle;

    Role(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
