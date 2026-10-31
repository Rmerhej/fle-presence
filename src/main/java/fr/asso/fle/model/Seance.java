package fr.asso.fle.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Une séance datée d'un groupe. */
@Entity
@Table(name = "seance",
        uniqueConstraints = @UniqueConstraint(name = "uk_seance_groupe_date_heure",
                columnNames = {"groupe_id", "date_seance", "heure_debut"}))
public class Seance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "groupe_id", nullable = false)
    private Groupe groupe;

    /** Créneau d'origine (null pour une séance ponctuelle ou issue d'un report). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creneau_id")
    private Creneau creneau;

    @Column(name = "date_seance", nullable = false)
    private LocalDate date;

    @Column(name = "heure_debut", nullable = false)
    private LocalTime heureDebut;

    @Column(name = "heure_fin", nullable = false)
    private LocalTime heureFin;

    @Column(length = 100)
    private String salle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutSeance statut = StatutSeance.PREVUE;

    /** Motif de l'annulation ou du report, affiché aux apprenants. */
    @Column(length = 255)
    private String motif;

    /** En cas de report : la nouvelle séance créée. */
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reportee_vers_id")
    private Seance reporteeVers;

    @Column(name = "modifiee_le")
    private LocalDateTime modifieeLe;

    public Seance() {
    }

    public Seance(Groupe groupe, LocalDate date, LocalTime heureDebut, LocalTime heureFin, String salle) {
        this.groupe = groupe;
        this.date = date;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
        this.salle = salle;
    }

    public boolean isPassee() {
        return date.isBefore(LocalDate.now());
    }

    public boolean isAujourdhui() {
        return date.isEqual(LocalDate.now());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Groupe getGroupe() { return groupe; }
    public void setGroupe(Groupe groupe) { this.groupe = groupe; }
    public Creneau getCreneau() { return creneau; }
    public void setCreneau(Creneau creneau) { this.creneau = creneau; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public LocalTime getHeureDebut() { return heureDebut; }
    public void setHeureDebut(LocalTime heureDebut) { this.heureDebut = heureDebut; }
    public LocalTime getHeureFin() { return heureFin; }
    public void setHeureFin(LocalTime heureFin) { this.heureFin = heureFin; }
    public String getSalle() { return salle; }
    public void setSalle(String salle) { this.salle = salle; }
    public StatutSeance getStatut() { return statut; }
    public void setStatut(StatutSeance statut) { this.statut = statut; }
    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }
    public Seance getReporteeVers() { return reporteeVers; }
    public void setReporteeVers(Seance reporteeVers) { this.reporteeVers = reporteeVers; }
    public LocalDateTime getModifieeLe() { return modifieeLe; }
    public void setModifieeLe(LocalDateTime modifieeLe) { this.modifieeLe = modifieeLe; }
}
