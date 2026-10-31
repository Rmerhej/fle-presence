package fr.asso.fle.model;

/** Types d'actions enregistrées dans l'historique. */
public enum TypeAction {
    SEANCE_ANNULEE("Séance annulée", "bi-x-circle", "danger"),
    SEANCE_REPORTEE("Séance déplacée", "bi-arrow-right-circle", "warning"),
    SEANCE_RETABLIE("Séance rétablie", "bi-arrow-counterclockwise", "success"),
    SEANCE_AJOUTEE("Séance ajoutée", "bi-plus-circle", "primary"),
    ANNULATION_GROUPEE("Annulation sur une période", "bi-calendar-x", "danger"),
    PLANNING_GENERE("Planning complété", "bi-arrow-repeat", "secondary"),
    CRENEAU_AJOUTE("Horaire ajouté", "bi-clock", "primary"),
    CRENEAU_SUPPRIME("Horaire supprimé", "bi-clock-history", "secondary"),
    GROUPE_CREE("Groupe créé", "bi-people", "primary"),
    GROUPE_RETIRE("Groupe retiré", "bi-people", "secondary"),
    PRESENCE_SAISIE("Présences saisies", "bi-pencil-square", "success"),
    PRESENCE_MODIFIEE("Présences corrigées", "bi-pencil", "warning"),
    COMPTE_CREE("Compte créé", "bi-person-plus", "primary"),
    COMPTE_MODIFIE("Compte modifié", "bi-person-gear", "secondary"),
    UNITE_CREEE("Unité créée", "bi-geo-alt", "primary"),
    UNITE_MODIFIEE("Unité modifiée", "bi-geo-alt", "secondary");

    private final String libelle;
    private final String icone;
    private final String couleur;

    TypeAction(String libelle, String icone, String couleur) {
        this.libelle = libelle;
        this.icone = icone;
        this.couleur = couleur;
    }

    public String getLibelle() { return libelle; }
    public String getIcone() { return icone; }
    public String getCouleur() { return couleur; }
}
