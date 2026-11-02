package fr.asso.fle;

import fr.asso.fle.model.*;
import fr.asso.fle.repository.SeanceRepository;
import fr.asso.fle.repository.UniteLocaleRepository;
import fr.asso.fle.repository.UtilisateurRepository;
import fr.asso.fle.service.PresenceService;
import fr.asso.fle.service.RegleMetierException;
import fr.asso.fle.service.SeanceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Tests de bout en bout avec la base H2 et les données de démonstration (profil dev). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class FlePresenceApplicationTests {

    @Autowired MockMvc mvc;
    @Autowired PresenceService presenceService;
    @Autowired SeanceRepository seanceRepository;
    @Autowired UniteLocaleRepository uniteRepository;
    @Autowired UtilisateurRepository utilisateurRepository;
    @Autowired SeanceService seanceService;

    @Test
    void pagePubliqueAccessibleSansCompte() throws Exception {
        mvc.perform(get("/u/chatou"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Prochains cours")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Cours annulé")));
    }

    @Test
    void pagesPubliquesDuPiedDePage() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("secours-catholique.org")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("modal-installer")));
        mvc.perform(get("/plan-du-site")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Cours à Chatou")));
        mvc.perform(get("/confidentialite")).andExpect(status().isOk());
    }

    @Test
    void calendrierEtManifesteDeLUnite() throws Exception {
        mvc.perform(get("/u/chatou/calendrier.ics"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/calendar"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("BEGIN:VCALENDAR")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("STATUS:CANCELLED")));
        mvc.perform(get("/u/chatou/manifest.json"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/u/chatou")));
        mvc.perform(get("/manifest.json")).andExpect(status().isOk());
        mvc.perform(get("/push/cle")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("cle")));
    }

    @Test
    void uniteInconnueDonne404() throws Exception {
        mvc.perform(get("/u/nulle-part")).andExpect(status().isNotFound());
    }

    @Test
    void adminProtegePourLesVisiteurs() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithUserDetails("benevole.chatou")
    void unBenevoleNAccedePasAuxEspacesResponsableEtAdmin() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isForbidden());
        mvc.perform(get("/responsable")).andExpect(status().isForbidden());
        mvc.perform(get("/saisie")).andExpect(status().isOk());
    }

    @Test
    @WithUserDetails("resp.chatou")
    void leResponsableVoitSonEspace() throws Exception {
        mvc.perform(get("/responsable")).andExpect(status().isOk());
        mvc.perform(get("/responsable/presences")).andExpect(status().isOk());
        mvc.perform(get("/responsable/affiche")).andExpect(status().isOk());
    }

    @Test
    @WithUserDetails("admin")
    void tableauDeBordAdmin() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isOk());
        mvc.perform(get("/admin/export.csv").param("type", "mois")).andExpect(status().isOk());
        mvc.perform(get("/admin/unites")).andExpect(status().isOk());
        mvc.perform(get("/admin/utilisateurs")).andExpect(status().isOk());
        mvc.perform(get("/admin/bilan")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Heures de bénévolat")));
        mvc.perform(get("/admin/journal")).andExpect(status().isOk());
    }

    @Test
    @Transactional
    void unBenevoleSaisitUneFoisEtSeulLeResponsableModifie() {
        UniteLocale chatou = uniteRepository.findBySlugAndActiveTrue("chatou").orElseThrow();
        Utilisateur benevole = utilisateurRepository.findByUsername("benevole.chatou").orElseThrow();
        Utilisateur responsable = utilisateurRepository.findByUsername("resp.chatou").orElseThrow();

        // Une séance maintenue récente, sans saisie (les données de démo laissent les 7 derniers jours vides).
        Seance seance = seanceRepository.findNonSaisies(chatou, LocalDate.now().minusDays(6), LocalDate.now())
                .stream().findFirst().orElseThrow();

        presenceService.enregistrer(seance.getId(), 2, 10, null, benevole);

        assertThatThrownBy(() -> presenceService.enregistrer(seance.getId(), 3, 11, null, benevole))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("responsable");

        Presence corrigee = presenceService.enregistrer(seance.getId(), 3, 12, "correction", responsable);
        assertThat(corrigee.getNbApprenants()).isEqualTo(12);
        assertThat(corrigee.getModifiePar()).isEqualTo("resp.chatou");
    }

    @Test
    @Transactional
    void unBenevoleNePeutPasSaisirPourUneAutreUnite() {
        UniteLocale sartrouville = uniteRepository.findBySlugAndActiveTrue("sartrouville").orElseThrow();
        Utilisateur benevoleChatou = utilisateurRepository.findByUsername("benevole.chatou").orElseThrow();
        Seance seance = seanceRepository.findNonSaisies(sartrouville, LocalDate.now().minusDays(30), LocalDate.now())
                .stream().findFirst().orElseThrow();

        assertThatThrownBy(() -> presenceService.enregistrer(seance.getId(), 1, 5, null, benevoleChatou))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("unité");
    }

    @Test
    @Transactional
    void annulationDUnePeriodeEtHistorique() {
        UniteLocale chatou = uniteRepository.findBySlugAndActiveTrue("chatou").orElseThrow();
        LocalDate debut = LocalDate.now().plusWeeks(3);
        LocalDate fin = debut.plusDays(13);

        SeanceService.ResultatAnnulation r = seanceService.annulerPeriode(chatou, debut, fin, null, "Vacances");

        assertThat(r.annulees()).isGreaterThan(0);
        assertThat(seanceRepository.findParUniteEntre(chatou, debut, fin))
                .allMatch(s -> s.getStatut() != StatutSeance.PREVUE);
    }

    @Test
    void connexionBloqueeApresCinqEchecs() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/login").with(csrf()).with(r -> { r.setRemoteAddr("10.9.9.9"); return r; })
                    .param("username", "benevole.chatou").param("password", "mauvais"))
                    .andExpect(status().is3xxRedirection());
        }
        // Même avec le bon mot de passe, la connexion reste bloquée pendant 15 minutes.
        mvc.perform(post("/login").with(csrf()).with(r -> { r.setRemoteAddr("10.9.9.9"); return r; })
                .param("username", "benevole.chatou").param("password", "benevole1"))
                .andExpect(redirectedUrl("/login?bloque"));
    }
}
