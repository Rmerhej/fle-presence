package fr.asso.fle.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Paramètre technique clé / valeur (ex. clés des notifications). */
@Entity
@Table(name = "parametre")
public class Parametre {

    @Id
    @Column(length = 100)
    private String cle;

    @Column(nullable = false, length = 2000)
    private String valeur;

    public Parametre() {
    }

    public Parametre(String cle, String valeur) {
        this.cle = cle;
        this.valeur = valeur;
    }

    public String getCle() { return cle; }
    public String getValeur() { return valeur; }
    public void setValeur(String valeur) { this.valeur = valeur; }
}
