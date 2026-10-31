package fr.asso.fle.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Une ligne de l'historique : qui a fait quoi, et quand. */
@Entity
@Table(name = "journal")
public class JournalEntree {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_action", nullable = false)
    private LocalDateTime dateAction;

    @Column(nullable = false, length = 50)
    private String auteur;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "unite_id")
    private UniteLocale unite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TypeAction action;

    @Column(length = 500)
    private String details;

    public JournalEntree() {
    }

    public JournalEntree(String auteur, UniteLocale unite, TypeAction action, String details) {
        this.dateAction = LocalDateTime.now();
        this.auteur = auteur;
        this.unite = unite;
        this.action = action;
        this.details = details == null ? null : details.substring(0, Math.min(500, details.length()));
    }

    public Long getId() { return id; }
    public LocalDateTime getDateAction() { return dateAction; }
    public String getAuteur() { return auteur; }
    public UniteLocale getUnite() { return unite; }
    public TypeAction getAction() { return action; }
    public String getDetails() { return details; }
}
