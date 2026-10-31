package fr.asso.fle.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Abonnement d'un navigateur aux notifications d'une unité (et éventuellement d'un groupe).
 * Contient seulement l'adresse technique fournie par le navigateur : ni nom, ni email.
 */
@Entity
@Table(name = "push_abonnement")
public class PushAbonnement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "unite_id", nullable = false)
    private UniteLocale unite;

    /** null = tous les groupes de l'unité. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "groupe_id")
    private Groupe groupe;

    @Column(nullable = false, length = 1000)
    private String endpoint;

    /** SHA-256 de l'endpoint (index unique, l'endpoint étant trop long pour MySQL). */
    @Column(name = "endpoint_hash", nullable = false, length = 64, unique = true)
    private String endpointHash;

    @Column(nullable = false)
    private String p256dh;

    @Column(nullable = false)
    private String auth;

    @Column(name = "cree_le", nullable = false)
    private LocalDateTime creeLe;

    public Long getId() { return id; }
    public UniteLocale getUnite() { return unite; }
    public void setUnite(UniteLocale unite) { this.unite = unite; }
    public Groupe getGroupe() { return groupe; }
    public void setGroupe(Groupe groupe) { this.groupe = groupe; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getEndpointHash() { return endpointHash; }
    public void setEndpointHash(String endpointHash) { this.endpointHash = endpointHash; }
    public String getP256dh() { return p256dh; }
    public void setP256dh(String p256dh) { this.p256dh = p256dh; }
    public String getAuth() { return auth; }
    public void setAuth(String auth) { this.auth = auth; }
    public LocalDateTime getCreeLe() { return creeLe; }
    public void setCreeLe(LocalDateTime creeLe) { this.creeLe = creeLe; }
}
