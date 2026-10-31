package fr.asso.fle.model;

import jakarta.persistence.*;

/** Un groupe d'apprenants dans une unité locale (ex. « Débutants », « Intermédiaires »). */
@Entity
@Table(name = "groupe")
public class Groupe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(length = 50)
    private String niveau;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "unite_id", nullable = false)
    private UniteLocale unite;

    @Column(nullable = false)
    private boolean actif = true;

    public Groupe() {
    }

    public Groupe(String nom, String niveau, UniteLocale unite) {
        this.nom = nom;
        this.niveau = niveau;
        this.unite = unite;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getNiveau() { return niveau; }
    public void setNiveau(String niveau) { this.niveau = niveau; }
    public UniteLocale getUnite() { return unite; }
    public void setUnite(UniteLocale unite) { this.unite = unite; }
    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
}
