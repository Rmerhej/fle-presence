package fr.asso.fle.model;

import jakarta.persistence.*;

/** Compte d'un administrateur, d'un responsable ou d'un bénévole. */
@Entity
@Table(name = "utilisateur")
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** Mot de passe chiffré (BCrypt). */
    @Column(nullable = false, length = 100)
    private String password;

    @Column(name = "nom_affiche", nullable = false, length = 100)
    private String nomAffiche;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /** Unité de rattachement (null pour l'administrateur). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "unite_id")
    private UniteLocale unite;

    @Column(nullable = false)
    private boolean actif = true;

    public Utilisateur() {
    }

    public Utilisateur(String username, String password, String nomAffiche, Role role, UniteLocale unite) {
        this.username = username;
        this.password = password;
        this.nomAffiche = nomAffiche;
        this.role = role;
        this.unite = unite;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getNomAffiche() { return nomAffiche; }
    public void setNomAffiche(String nomAffiche) { this.nomAffiche = nomAffiche; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public UniteLocale getUnite() { return unite; }
    public void setUnite(UniteLocale unite) { this.unite = unite; }
    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
}
