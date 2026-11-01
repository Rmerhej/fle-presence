package fr.asso.fle.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Compteurs en mémoire « N actions par fenêtre de temps », par clé (adresse IP, identifiant...).
 * Simple et suffisant pour une seule instance de l'application.
 */
@Component
public class LimiteurRequetes {

    /** Garde-fou mémoire : au-delà, les compteurs sont remis à zéro. */
    private static final int MAX_CLES = 50_000;

    private record Fenetre(long debut, int compte) {
    }

    private final Map<String, Fenetre> compteurs = new ConcurrentHashMap<>();

    /**
     * Enregistre une action et indique si elle reste dans la limite.
     *
     * @return true si l'action est autorisée, false si la limite est dépassée
     */
    public boolean autoriser(String cle, int limite, Duration fenetre) {
        if (compteurs.size() > MAX_CLES) {
            compteurs.clear();
        }
        long maintenant = System.currentTimeMillis();
        long duree = fenetre.toMillis();
        Fenetre f = compteurs.compute(cle, (k, ancienne) -> {
            if (ancienne == null || maintenant - ancienne.debut() >= duree) {
                return new Fenetre(maintenant, 1);
            }
            return new Fenetre(ancienne.debut(), ancienne.compte() + 1);
        });
        return f.compte() <= limite;
    }

    /** Nombre d'actions comptées dans la fenêtre en cours (0 si elle est terminée). */
    public int compte(String cle, Duration fenetre) {
        Fenetre f = compteurs.get(cle);
        if (f == null || System.currentTimeMillis() - f.debut() >= fenetre.toMillis()) {
            return 0;
        }
        return f.compte();
    }

    public void effacer(String cle) {
        compteurs.remove(cle);
    }

    /** Ménage régulier des fenêtres terminées (la plus longue fenêtre utilisée est d'une heure). */
    @Scheduled(fixedDelay = 10 * 60 * 1000)
    public void nettoyer() {
        long limite = System.currentTimeMillis() - Duration.ofHours(1).toMillis();
        compteurs.entrySet().removeIf(e -> e.getValue().debut() < limite);
    }
}
