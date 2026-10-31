package fr.asso.fle.web;

import fr.asso.fle.model.*;
import fr.asso.fle.security.UtilisateurCourant;
import fr.asso.fle.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

/**
 * Espace du responsable d'unité : planning (annuler / reporter), groupes et créneaux,
 * correction des présences, comptes bénévoles, affiche avec QR code.
 */
@Controller
@RequestMapping("/responsable")
public class ResponsableController {

    private final UtilisateurCourant utilisateurCourant;
    private final SeanceService seanceService;
    private final PresenceService presenceService;
    private final AdministrationService administrationService;
    private final StatsService statsService;
    private final fr.asso.fle.repository.SeanceRepository seanceRepository;

    public ResponsableController(UtilisateurCourant utilisateurCourant,
                                 SeanceService seanceService,
                                 PresenceService presenceService,
                                 AdministrationService administrationService,
                                 StatsService statsService,
                                 fr.asso.fle.repository.SeanceRepository seanceRepository) {
        this.utilisateurCourant = utilisateurCourant;
        this.seanceService = seanceService;
        this.presenceService = presenceService;
        this.administrationService = administrationService;
        this.statsService = statsService;
        this.seanceRepository = seanceRepository;
    }

    private UniteLocale monUnite() {
        return utilisateurCourant.get().getUnite();
    }

    // ================================================================ pages

    @GetMapping
    public String tableauDeBord(@RequestParam(defaultValue = "planning") String onglet, Model model) {
        UniteLocale unite = monUnite();
        LocalDate aujourdHui = LocalDate.now();

        List<Seance> aVenir = seanceService.seancesEntre(unite, aujourdHui, aujourdHui.plusWeeks(6));
        List<Seance> nonSaisies = seanceRepository.findNonSaisies(unite, aujourdHui.minusDays(60), aujourdHui);

        model.addAttribute("unite", unite);
        model.addAttribute("onglet", onglet);
        model.addAttribute("aVenir", aVenir);
        model.addAttribute("nonSaisies", nonSaisies);
        model.addAttribute("groupes", administrationService.groupes(unite));
        model.addAttribute("creneaux", seanceService.creneauxTries(unite));
        model.addAttribute("benevoles", administrationService.benevolesDe(unite));
        model.addAttribute("jours", DayOfWeek.values());
        model.addAttribute("aujourdHui", aujourdHui);
        model.addAttribute("lienPublic", lienPublic(unite));
        return "responsable/tableau";
    }

    /** Présences saisies pour un mois, avec possibilité de corriger. */
    @GetMapping("/presences")
    public String presences(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mois,
                            Model model) {
        UniteLocale unite = monUnite();
        YearMonth m = mois != null ? mois : YearMonth.now();
        List<Presence> presences = presenceService.parUniteEntre(unite, m.atDay(1), m.atEndOfMonth());
        long benevoles = presences.stream().mapToLong(Presence::getNbBenevoles).sum();
        long apprenants = presences.stream().mapToLong(Presence::getNbApprenants).sum();

        model.addAttribute("unite", unite);
        model.addAttribute("mois", m);
        model.addAttribute("presences", presences);
        model.addAttribute("totaux", new StatsService.Totaux(presences.size(), benevoles, apprenants));
        model.addAttribute("moisPrecedent", m.minusMonths(1));
        model.addAttribute("moisSuivant", m.plusMonths(1));
        return "responsable/presences";
    }

    /** Affiche imprimable avec QR code vers la page publique. */
    @GetMapping("/affiche")
    public String affiche(Model model) {
        UniteLocale unite = monUnite();
        model.addAttribute("unite", unite);
        model.addAttribute("creneaux", seanceService.creneauxTries(unite));
        model.addAttribute("lienPublic", lienPublic(unite));
        return "responsable/affiche";
    }

    // ================================================================ planning

    @PostMapping("/seances/generer")
    public String generer(@RequestParam(defaultValue = "8") int semaines, RedirectAttributes ra) {
        int n = seanceService.genererPourUnite(monUnite(), semaines);
        ra.addFlashAttribute("succes", n == 0
                ? "Le planning était déjà à jour."
                : n + " séance(s) ajoutée(s) au planning.");
        return "redirect:/responsable";
    }

    @PostMapping("/seances")
    public String ajouterSeance(@RequestParam(required = false) Long groupeId,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime debut,
                                @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime fin,
                                @RequestParam(required = false) String salle,
                                RedirectAttributes ra) {
        return executer(ra, "planning", "Séance ajoutée au planning.",
                () -> seanceService.ajouterSeancePonctuelle(monUnite(), groupeId, date, debut, fin, salle));
    }

    @PostMapping("/seances/{id}/annuler")
    public String annuler(@PathVariable Long id, @RequestParam(required = false) String motif, RedirectAttributes ra) {
        return executer(ra, "planning", "Séance annulée : les apprenants verront l'alerte sur la page publique.",
                () -> seanceService.annuler(monUnite(), id, motif));
    }

    @PostMapping("/seances/{id}/reporter")
    public String reporter(@PathVariable Long id,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime debut,
                           @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime fin,
                           @RequestParam(required = false) String motif,
                           RedirectAttributes ra) {
        return executer(ra, "planning", "Séance déplacée : les apprenants verront l'alerte sur la page publique.",
                () -> seanceService.reporter(monUnite(), id, date, debut, fin, motif));
    }

    @PostMapping("/seances/{id}/retablir")
    public String retablir(@PathVariable Long id, RedirectAttributes ra) {
        return executer(ra, "planning", "Séance rétablie.", () -> seanceService.retablir(monUnite(), id));
    }

    // ================================================================ groupes & créneaux

    @PostMapping("/groupes")
    public String ajouterGroupe(@RequestParam(required = false) String nom,
                                @RequestParam(required = false) String niveau,
                                RedirectAttributes ra) {
        return executer(ra, "groupes", "Groupe créé. Ajoutez-lui maintenant un ou plusieurs créneaux.",
                () -> administrationService.creerGroupe(monUnite(), nom, niveau));
    }

    @PostMapping("/groupes/{id}/supprimer")
    public String supprimerGroupe(@PathVariable Long id, RedirectAttributes ra) {
        return executer(ra, "groupes", "Groupe retiré.",
                () -> administrationService.desactiverGroupe(monUnite(), id));
    }

    @PostMapping("/creneaux")
    public String ajouterCreneau(@RequestParam(required = false) Long groupeId,
                                 @RequestParam(required = false) DayOfWeek jour,
                                 @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime debut,
                                 @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime fin,
                                 @RequestParam(required = false) String salle,
                                 RedirectAttributes ra) {
        return executer(ra, "groupes", "Créneau ajouté : les séances des " + SeanceService.SEMAINES_AVANCE
                        + " prochaines semaines sont créées.",
                () -> {
                    if (jour == null) {
                        throw new RegleMetierException("Choisissez le jour de la semaine.");
                    }
                    seanceService.ajouterCreneau(monUnite(), groupeId, jour, debut, fin, salle);
                });
    }

    @PostMapping("/creneaux/{id}/supprimer")
    public String supprimerCreneau(@PathVariable Long id, RedirectAttributes ra) {
        return executer(ra, "groupes", "Créneau supprimé (les séances futures correspondantes sont retirées).",
                () -> seanceService.supprimerCreneau(monUnite(), id));
    }

    // ================================================================ bénévoles

    @PostMapping("/benevoles")
    public String ajouterBenevole(@RequestParam(required = false) String username,
                                  @RequestParam(required = false) String nomAffiche,
                                  @RequestParam(required = false) String motDePasse,
                                  RedirectAttributes ra) {
        return executer(ra, "benevoles", "Compte bénévole créé.",
                () -> administrationService.creerBenevole(monUnite(), username, nomAffiche, motDePasse));
    }

    @PostMapping("/benevoles/{id}/basculer")
    public String basculerBenevole(@PathVariable Long id, RedirectAttributes ra) {
        return executer(ra, "benevoles", "Compte mis à jour.",
                () -> administrationService.basculerBenevole(monUnite(), id));
    }

    // ================================================================ outils

    private String executer(RedirectAttributes ra, String onglet, String messageSucces, Runnable action) {
        try {
            action.run();
            ra.addFlashAttribute("succes", messageSucces);
        } catch (RegleMetierException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/responsable?onglet=" + onglet;
    }

    private static String lienPublic(UniteLocale unite) {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path("/u/{slug}").buildAndExpand(unite.getSlug()).toUriString();
    }
}
