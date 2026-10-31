package fr.asso.fle.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdministrationServiceSlugTest {

    @Test
    void slugSansAccentsNiEspaces() {
        assertThat(AdministrationService.slug("Le Vésinet")).isEqualTo("le-vesinet");
        assertThat(AdministrationService.slug("  Saint-Germain-en-Laye ")).isEqualTo("saint-germain-en-laye");
        assertThat(AdministrationService.slug("Maisons-Laffitte (centre)")).isEqualTo("maisons-laffitte-centre");
    }
}
