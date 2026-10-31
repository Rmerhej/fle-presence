package fr.asso.fle.model;

import jakarta.persistence.*;

/** Une unité locale de l'association (Chatou, Sartrouville...). */
@Entity
@Table(name = "unite_locale")
public class UniteLocale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nom;

    /** Identifiant utilisé dans l'adresse de la page publique : /u/{slug} */
    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(length = 255)
    private String adresse;

    @Column(nullable = false)
    private boolean active = true;

    public UniteLocale() {
    }

    public UniteLocale(String nom, String slug, String adresse) {
        this.nom = nom;
        this.slug = slug;
        this.adresse = adresse;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
