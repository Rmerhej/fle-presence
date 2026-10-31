package fr.asso.fle.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Comptage des présents pour une séance : uniquement des nombres,
 * aucune donnée personnelle sur les apprenants.
 */
@Entity
@Table(name = "presence")
public class Presence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "seance_id", nullable = false, unique = true)
    private Seance seance;

    @Column(name = "nb_benevoles", nullable = false)
    private int nbBenevoles;

    @Column(name = "nb_apprenants", nullable = false)
    private int nbApprenants;

    @Column(length = 500)
    private String commentaire;

    @Column(name = "saisi_par", nullable = false, length = 50)
    private String saisiPar;

    @Column(name = "saisi_le", nullable = false)
    private LocalDateTime saisiLe;

    @Column(name = "modifie_par", length = 50)
    private String modifiePar;

    @Column(name = "modifie_le")
    private LocalDateTime modifieLe;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Seance getSeance() { return seance; }
    public void setSeance(Seance seance) { this.seance = seance; }
    public int getNbBenevoles() { return nbBenevoles; }
    public void setNbBenevoles(int nbBenevoles) { this.nbBenevoles = nbBenevoles; }
    public int getNbApprenants() { return nbApprenants; }
    public void setNbApprenants(int nbApprenants) { this.nbApprenants = nbApprenants; }
    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }
    public String getSaisiPar() { return saisiPar; }
    public void setSaisiPar(String saisiPar) { this.saisiPar = saisiPar; }
    public LocalDateTime getSaisiLe() { return saisiLe; }
    public void setSaisiLe(LocalDateTime saisiLe) { this.saisiLe = saisiLe; }
    public String getModifiePar() { return modifiePar; }
    public void setModifiePar(String modifiePar) { this.modifiePar = modifiePar; }
    public LocalDateTime getModifieLe() { return modifieLe; }
    public void setModifieLe(LocalDateTime modifieLe) { this.modifieLe = modifieLe; }
}
