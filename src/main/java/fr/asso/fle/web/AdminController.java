package fr.asso.fle.web;

import fr.asso.fle.model.Role;
import fr.asso.fle.security.UtilisateurCourant;
import fr.asso.fle.service.AdministrationService;
import fr.asso.fle.service.RegleMetierException;
import fr.asso.fle.service.StatsService;
import fr.asso.fle.service.StatsService.LigneStat;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/** Espace administrateur : tableau de bord, unités locales, comptes. */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final StatsService statsService;
    private final AdministrationService administrationService;
    private final UtilisateurCourant utilisateurCourant;

    public AdminController(StatsService statsService,
                           AdministrationService administrationService,
                           UtilisateurCourant utilisateurCourant) {
        this.statsService = statsService;
        this.administrationService = administrationService;
        this.utilisateurCourant = utilisateurCourant;
    }

    // ================================================================ tableau de bord

    @GetMapping
    public String tableauDeBord(@RequestParam(required = false) Integer annee,
                                @RequestParam(required = false) Long uniteId,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
                                Model model) {
        LocalDate aujourdHui = LocalDate.now();
        int an = annee != null ? annee : aujourdHui.getYear();
        LocalDate debut = du != null ? du : aujourdHui.withDayOfMonth(1);
        LocalDate fin = au != null ? au : aujourdHui;
        if (fin.isBefore(debut)) {
            LocalDate t = debut;
            debut = fin;
            fin = t;
        }

        List<LigneStat> parMois = statsService.parMois(an, uniteId);
        List<LigneStat> parJour = statsService.parJour(debut, fin, uniteId);

        model.addAttribute("annee", an);
        model.addAttribute("annees", List.of(an + 1, an, an - 1, an - 2, an - 3).stream()
                .filter(a -> a <= aujourdHui.getYear() + 1).toList());
        model.addAttribute("uniteId", uniteId);
        model.addAttribute("du", debut);
        model.addAttribute("au", fin);
        model.addAttribute("unites", administrationService.toutesLesUnites());
        model.addAttribute("totauxAnnee", statsService.totaux(parMois));
        model.addAttribute("parMois", parMois);
        model.addAttribute("parJour", parJour);
        model.addAttribute("totauxPeriode", statsService.totaux(parJour));
        model.addAttribute("parAnnee", statsService.parAnnee(uniteId));
        model.addAttribute("graphique", statsService.graphiqueMensuel(an, uniteId));
        return "admin/tableau";
    }

    /** Export CSV (séparateur « ; » pour Excel en français). type = jour | mois | annee */
    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue = "mois") String type,
                                         @RequestParam(required = false) Integer annee,
                                         @RequestParam(required = false) Long uniteId,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au) {
        LocalDate aujourdHui = LocalDate.now();
        int an = annee != null ? annee : aujourdHui.getYear();
        List<LigneStat> lignes = switch (type) {
            case "jour" -> statsService.parJour(du != null ? du : aujourdHui.withDayOfMonth(1),
                    au != null ? au : aujourdHui, uniteId);
            case "annee" -> statsService.parAnnee(uniteId);
            default -> statsService.parMois(an, uniteId);
        };

        StringBuilder csv = new StringBuilder("﻿"); // BOM : accents corrects dans Excel
        csv.append("Période;Unité;Séances;Bénévoles présents;Apprenants présents;Moyenne apprenants/séance\n");
        for (LigneStat l : lignes) {
            csv.append(l.periode()).append(';')
               .append(l.unite().replace(";", ",")).append(';')
               .append(l.seances()).append(';')
               .append(l.benevoles()).append(';')
               .append(l.apprenants()).append(';')
               .append(String.format(Locale.FRANCE, "%.1f", l.moyenneApprenants())).append('\n');
        }
        String nom = "presences-" + type + "-" + aujourdHui + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nom + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    // ================================================================ unités

    @GetMapping("/unites")
    public String unites(Model model) {
        model.addAttribute("unites", administrationService.toutesLesUnites());
        return "admin/unites";
    }

    @PostMapping("/unites")
    public String creerUnite(@RequestParam(required = false) String nom,
                             @RequestParam(required = false) String adresse,
                             RedirectAttributes ra) {
        return executer(ra, "/admin/unites", "Unité créée.",
                () -> administrationService.creerUnite(nom, adresse));
    }

    @PostMapping("/unites/{id}/basculer")
    public String basculerUnite(@PathVariable Long id, RedirectAttributes ra) {
        return executer(ra, "/admin/unites", "Unité mise à jour.", () -> administrationService.basculerUnite(id));
    }

    // ================================================================ comptes

    @GetMapping("/utilisateurs")
    public String utilisateurs(Model model) {
        model.addAttribute("utilisateurs", administrationService.tousLesUtilisateurs());
        model.addAttribute("unites", administrationService.unitesActives());
        model.addAttribute("roles", Role.values());
        return "admin/utilisateurs";
    }

    @PostMapping("/utilisateurs")
    public String creerUtilisateur(@RequestParam(required = false) String username,
                                   @RequestParam(required = false) String nomAffiche,
                                   @RequestParam(required = false) String motDePasse,
                                   @RequestParam(required = false) Role role,
                                   @RequestParam(required = false) Long uniteId,
                                   RedirectAttributes ra) {
        return executer(ra, "/admin/utilisateurs", "Compte créé.",
                () -> administrationService.creerUtilisateur(username, nomAffiche, motDePasse, role, uniteId));
    }

    @PostMapping("/utilisateurs/{id}/basculer")
    public String basculerUtilisateur(@PathVariable Long id, RedirectAttributes ra) {
        String moi = utilisateurCourant.get().getUsername();
        return executer(ra, "/admin/utilisateurs", "Compte mis à jour.",
                () -> administrationService.basculerUtilisateur(id, moi));
    }

    @PostMapping("/utilisateurs/{id}/motdepasse")
    public String motDePasse(@PathVariable Long id, @RequestParam(required = false) String motDePasse,
                             RedirectAttributes ra) {
        return executer(ra, "/admin/utilisateurs", "Mot de passe modifié.",
                () -> administrationService.changerMotDePasse(id, motDePasse));
    }

    private static String executer(RedirectAttributes ra, String page, String succes, Runnable action) {
        try {
            action.run();
            ra.addFlashAttribute("succes", succes);
        } catch (RegleMetierException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:" + page;
    }
}
