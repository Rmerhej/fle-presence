package fr.asso.fle.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;

/**
 * Freine les tentatives de deviner un mot de passe :
 * après N échecs dans la fenêtre, pour une même adresse IP ou un même identifiant,
 * la connexion est bloquée jusqu'à la fin de la fenêtre.
 */
@Component
public class ProtectionConnexion {

    private final LimiteurRequetes limiteur;
    private final int maxEchecs;
    private final Duration fenetre;

    public ProtectionConnexion(LimiteurRequetes limiteur,
                               @Value("${app.limites.connexion.echecs:5}") int maxEchecs,
                               @Value("${app.limites.connexion.minutes:15}") int minutes) {
        this.limiteur = limiteur;
        this.maxEchecs = maxEchecs;
        this.fenetre = Duration.ofMinutes(minutes);
    }

    public boolean estBloque(String ip, String identifiant) {
        return limiteur.compte(cleIp(ip), fenetre) >= maxEchecs
                || (identifiant != null && limiteur.compte(cleId(identifiant), fenetre) >= maxEchecs);
    }

    public void echec(String ip, String identifiant) {
        limiteur.autoriser(cleIp(ip), Integer.MAX_VALUE, fenetre);
        if (identifiant != null && !identifiant.isBlank()) {
            limiteur.autoriser(cleId(identifiant), Integer.MAX_VALUE, fenetre);
        }
    }

    public void succes(String ip, String identifiant) {
        limiteur.effacer(cleIp(ip));
        if (identifiant != null) {
            limiteur.effacer(cleId(identifiant));
        }
    }

    public int minutes() {
        return (int) fenetre.toMinutes();
    }

    private static String cleIp(String ip) {
        return "connexion-ip:" + ip;
    }

    private static String cleId(String identifiant) {
        return "connexion-id:" + identifiant.strip().toLowerCase(Locale.ROOT);
    }
}
