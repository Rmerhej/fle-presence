package fr.asso.fle.model;

import jakarta.persistence.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Créneau récurrent d'un groupe (ex. tous les mardis de 14h à 16h).
 * Les séances datées sont générées à partir des créneaux.
 */
@Entity
@Table(name = "creneau")
public class Creneau {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "groupe_id", nullable = false)
    private Groupe groupe;

    @Enumerated(EnumType.STRING)
    @Column(name = "jour_semaine", nullable = false, length = 10)
    private DayOfWeek jourSemaine;

    @Column(name = "heure_debut", nullable = false)
    private LocalTime heureDebut;

    @Column(name = "heure_fin", nullable = false)
    private LocalTime heureFin;

    @Column(length = 100)
    private String salle;

    @Column(nullable = false)
    private boolean actif = true;

    public Creneau() {
    }

    public Creneau(Groupe groupe, DayOfWeek jourSemaine, LocalTime heureDebut, LocalTime heureFin, String salle) {
        this.groupe = groupe;
        this.jourSemaine = jourSemaine;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
        this.salle = salle;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Groupe getGroupe() { return groupe; }
    public void setGroupe(Groupe groupe) { this.groupe = groupe; }
    public DayOfWeek getJourSemaine() { return jourSemaine; }
    public void setJourSemaine(DayOfWeek jourSemaine) { this.jourSemaine = jourSemaine; }
    public LocalTime getHeureDebut() { return heureDebut; }
    public void setHeureDebut(LocalTime heureDebut) { this.heureDebut = heureDebut; }
    public LocalTime getHeureFin() { return heureFin; }
    public void setHeureFin(LocalTime heureFin) { this.heureFin = heureFin; }
    public String getSalle() { return salle; }
    public void setSalle(String salle) { this.salle = salle; }
    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
}
