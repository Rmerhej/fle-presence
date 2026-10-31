package fr.asso.fle.web;

import fr.asso.fle.model.*;
import fr.asso.fle.security.UtilisateurCourant;
import fr.asso.fle.service.PresenceService;
import fr.asso.fle.service.RegleMetierException;
import fr.asso.fle.service.SeanceService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Saisie du nombre de bénévoles et d'apprenants présents à une séance. */
@Controller
@RequestMapping("/saisie")
public class SaisieController {

    private final UtilisateurCourant utilisateurCourant;
    private final SeanceService seanceService;
    private final PresenceService presenceService;

    public SaisieController(UtilisateurCourant utilisateurCourant,
                            SeanceService seanceService,
                            PresenceService presenceService) {
        this.utilisateurCourant = utilisateurCourant;
        this.seanceService = seanceService;
        this.presenceService = presenceService;
    }

    /** Liste des séances récentes de l'unité : à saisir / déjà saisies. */
    @GetMapping
    public String liste(Model model) {
        Utilisateur moi = utilisateurCourant.get();
        UniteLocale unite = moi.getUnite();
        LocalDate aujourdHui = LocalDate.now();

        List<Seance> seances = seanceService
                .seancesEntre(unite, aujourdHui.minusDays(PresenceService.DELAI_SAISIE_BENEVOLE_JOURS), aujourdHui)
                .stream()
                .filter(s -> s.getStatut() == StatutSeance.PREVUE)
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .toList();
        Map<Long, Presence> presences = presenceService.parSeance(seances);

        model.addAttribute("unite", unite);
        model.addAttribute("aSaisir", seances.stream().filter(s -> !presences.containsKey(s.getId())).toList());
        model.addAttribute("saisies", seances.stream().filter(s -> presences.containsKey(s.getId())).toList());
        model.addAttribute("presences", presences);
        model.addAttribute("estResponsable", moi.getRole() == Role.RESPONSABLE);
        model.addAttribute("aujourdHui", aujourdHui);
        return "saisie/liste";
    }

    @GetMapping("/{seanceId}")
    public String formulaire(@PathVariable Long seanceId, Model model) {
        Utilisateur moi = utilisateurCourant.get();
        Seance seance = presenceService.seancePourSaisie(seanceId, moi);
        model.addAttribute("seance", seance);
        model.addAttribute("presence", presenceService.pourSeance(seance).orElse(null));
        return "saisie/formulaire";
    }

    @PostMapping("/{seanceId}")
    public String enregistrer(@PathVariable Long seanceId,
                              @RequestParam(required = false) Integer nbBenevoles,
                              @RequestParam(required = false) Integer nbApprenants,
                              @RequestParam(required = false) String commentaire,
                              @RequestParam(required = false) String retour,
                              RedirectAttributes ra) {
        Utilisateur moi = utilisateurCourant.get();
        try {
            Presence p = presenceService.enregistrer(seanceId, nbBenevoles, nbApprenants, commentaire, moi);
            ra.addFlashAttribute("succes", "Merci ! Présences enregistrées : "
                    + p.getNbBenevoles() + " bénévole(s), " + p.getNbApprenants() + " apprenant(s).");
        } catch (RegleMetierException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/saisie/" + seanceId;
        }
        // Le responsable peut revenir à la page « Présences » de son espace.
        if ("presences".equals(retour) && moi.getRole() == Role.RESPONSABLE) {
            return "redirect:/responsable/presences";
        }
        return "redirect:/saisie";
    }
}
