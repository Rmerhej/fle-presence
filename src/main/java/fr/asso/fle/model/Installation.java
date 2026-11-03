package fr.asso.fle.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Installation de l'application sur un appareil (compteur anonyme) :
 * ni adresse IP ni identifiant d'appareil, seulement la date, la plateforme et l'unité éventuelle.
 */
@Entity
@Table(name = "installation")
public class Installation {

    /** Plateformes reconnues. */
    public static final java.util.List<String> PLATEFORMES = java.util.List.of("android", "iphone", "ordinateur");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** null = installée depuis la page d'accueil. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unite_id")
    private UniteLocale unite;

    @Column(nullable = false, length = 20)
    private String plateforme;

    @Column(name = "installee_le", nullable = false)
    private LocalDateTime installeeLe;

    protected Installation() {
    }

    public Installation(UniteLocale unite, String plateforme, LocalDateTime installeeLe) {
        this.unite = unite;
        this.plateforme = plateforme;
        this.installeeLe = installeeLe;
    }

    public Long getId() { return id; }
    public UniteLocale getUnite() { return unite; }
    public String getPlateforme() { return plateforme; }
    public LocalDateTime getInstalleeLe() { return installeeLe; }
}
