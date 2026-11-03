package fr.asso.fle.service;

import fr.asso.fle.model.Installation;
import fr.asso.fle.model.UniteLocale;
import fr.asso.fle.repository.InstallationRepository;
import fr.asso.fle.repository.UniteLocaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Compteur anonyme des installations de l'application. */
@Service
public class InstallationService {

    /** Chiffres affichés sur le tableau de bord. */
    public record Compteur(long total, long ceMois, Map<String, Long> parPlateforme) {
    }

    private final InstallationRepository installations;
    private final UniteLocaleRepository unites;

    public InstallationService(InstallationRepository installations, UniteLocaleRepository unites) {
        this.installations = installations;
        this.unites = unites;
    }

    /** Enregistre une installation ; une plateforme inconnue est comptée comme « ordinateur ». */
    @Transactional
    public void enregistrer(String slugUnite, String plateforme) {
        String p = plateforme == null ? "" : plateforme.trim().toLowerCase(Locale.ROOT);
        if (!Installation.PLATEFORMES.contains(p)) {
            p = "ordinateur";
        }
        UniteLocale unite = (slugUnite == null || slugUnite.isBlank())
                ? null
                : unites.findBySlugAndActiveTrue(slugUnite.trim()).orElse(null);
        installations.save(new Installation(unite, p, LocalDateTime.now()));
    }

    @Transactional(readOnly = true)
    public Compteur compteur() {
        Map<String, Long> parPlateforme = new LinkedHashMap<>();
        Installation.PLATEFORMES.forEach(p -> parPlateforme.put(p, 0L));
        for (Object[] ligne : installations.compterParPlateforme()) {
            parPlateforme.merge((String) ligne[0], (Long) ligne[1], Long::sum);
        }
        LocalDateTime debutMois = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        return new Compteur(installations.count(),
                installations.countByInstalleeLeGreaterThanEqual(debutMois),
                parPlateforme);
    }
}
