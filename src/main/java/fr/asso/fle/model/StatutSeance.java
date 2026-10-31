package fr.asso.fle.model;

public enum StatutSeance {
    PREVUE("Cours maintenu", "success", "bi-check-circle-fill"),
    ANNULEE("Cours annulé", "danger", "bi-x-circle-fill"),
    REPORTEE("Cours déplacé", "warning", "bi-arrow-right-circle-fill");

    private final String libelle;
    private final String couleur;
    private final String icone;

    StatutSeance(String libelle, String couleur, String icone) {
        this.libelle = libelle;
        this.couleur = couleur;
        this.icone = icone;
    }

    public String getLibelle() {
        return libelle;
    }

    /** Couleur Bootstrap (success, danger, warning). */
    public String getCouleur() {
        return couleur;
    }

    /** Icône Bootstrap Icons. */
    public String getIcone() {
        return icone;
    }
}
