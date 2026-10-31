package fr.asso.fle.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.stream.Stream;

/**
 * Liens affichés dans le pied de page, réglables sans toucher au code
 * (application.properties ou variables d'environnement APP_LIEN_...).
 * Un lien vide n'est pas affiché.
 */
@ConfigurationProperties(prefix = "app.liens")
public record Liens(
        String association,
        String associationNom,
        String benevolat,
        String frenchForBeginners,
        String facebook,
        String instagram,
        String linkedin,
        String youtube,
        String x) {

    public boolean aDesReseaux() {
        return Stream.of(facebook, instagram, linkedin, youtube, x)
                .anyMatch(l -> l != null && !l.isBlank());
    }
}
