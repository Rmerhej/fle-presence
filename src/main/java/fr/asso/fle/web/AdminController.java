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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Espace administrateur : tableau de bord, unités locales, comptes. */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final StatsService statsService;
    private final AdministrationService administrationService;
    private final UtilisateurCourant utilisateurCourant;
    private final fr.asso.fle.service.JournalService journalService;
    private final fr.asso.fle.service.InstallationService installationService;

    public AdminController(StatsService statsService,
                           AdministrationService administrationService,
                           UtilisateurCourant utilisateurCourant,
                           fr.asso.fle.service.JournalService journalService,
                           fr.asso.fle.service.InstallationService installationService) {
        this.statsService = statsService;
        this.administrationService = administrationService;
        this.utilisateurCourant = utilisateurCourant;
        this.journalService = journalService;
        this.installationService = installationService;
    }

    /** Historique de toutes les unités (ou d'une seule). */
    @GetMapping("/journal")
    public String journal(@RequestParam(required = false) Long uniteId, Model model) {
        model.addAttribute("uniteId", uniteId);
        model.addAttribute("unites", administrationService.toutesLesUnites());
        model.addAttribute("historique", journalService.tout(uniteId, 300));
        return "admin/journal";
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
        model.addAttribute("comparaison", statsService.comparaison(an, uniteId));
        model.addAttribute("parMois", parMois);
        model.addAttribute("parJour", parJour);
        model.addAttribute("totauxPeriode", statsService.totaux(parJour));
        model.addAttribute("parAnnee", statsService.parAnnee(uniteId));
        model.addAttribute("graphique", statsService.graphiqueMensuel(an, uniteId));
        model.addAttribute("installations", installationService.compteur());
        return "admin/tableau";
    }

    /** Export Excel (.xlsx) : type = jour | mois | annee */
    @GetMapping("/export.xlsx")
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue = "mois") String type,
                                         @RequestParam(required = false) Integer annee,
                                         @RequestParam(required = false) Long uniteId,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au) {
        LocalDate aujourdHui = LocalDate.now();
        int an = annee != null ? annee : aujourdHui.getYear();
        LocalDate debut = du != null ? du : aujourdHui.withDayOfMonth(1);
        LocalDate fin = au != null ? au : aujourdHui;
        DateTimeFormatter jj = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        List<LigneStat> lignes;
        String titre, onglet, nom;
        switch (type) {
            case "jour" -> {
                lignes = statsService.parJour(debut, fin, uniteId);
                titre = "Présences du " + debut.format(jj) + " au " + fin.format(jj);
                onglet = "Par date";
                nom = "presences-" + debut + "-au-" + fin;
            }
            case "annee" -> {
                lignes = statsService.parAnnee(uniteId);
                titre = "Présences par année";
                onglet = "Par année";
                nom = "presences-par-annee";
            }
            default -> {
                lignes = statsService.parMois(an, uniteId);
                titre = "Présences par mois — " + an;
                onglet = "Par mois " + an;
                nom = "presences-par-mois-" + an;
            }
        }
        String perimetre = uniteId == null ? "Toutes les unités locales"
                : administrationService.toutesLesUnites().stream()
                    .filter(u -> u.getId().equals(uniteId)).map(u -> "Unité de " + u.getNom())
                    .findFirst().orElse("");

        List<ClasseurExcel.Colonne> colonnes = List.of(
                new ClasseurExcel.Colonne("Période", ClasseurExcel.Format.TEXTE, 16),
                new ClasseurExcel.Colonne("Unité", ClasseurExcel.Format.TEXTE, 20),
                new ClasseurExcel.Colonne("Séances", ClasseurExcel.Format.ENTIER, 10),
                new ClasseurExcel.Colonne("Bénévoles présents", ClasseurExcel.Format.ENTIER, 12),
                new ClasseurExcel.Colonne("Apprenants présents", ClasseurExcel.Format.ENTIER, 12),
                new ClasseurExcel.Colonne("Moyenne apprenants / séance", ClasseurExcel.Format.DECIMAL, 14),
                new ClasseurExcel.Colonne("Heures de bénévolat", ClasseurExcel.Format.DECIMAL, 12),
                new ClasseurExcel.Colonne("Heures de cours suivies", ClasseurExcel.Format.DECIMAL, 12));
        List<List<Object>> valeurs = new java.util.ArrayList<>();
        for (LigneStat l : lignes) {
            valeurs.add(List.<Object>of(l.periode(), l.unite(), l.seances(), l.benevoles(), l.apprenants(),
                    l.moyenneApprenants(), l.heuresBenevolat(), l.heuresApprenants()));
        }
        StatsService.Totaux t = statsService.totaux(lignes);
        List<Object> total = List.<Object>of("Total", "", t.seances(), t.benevoles(), t.apprenants(),
                t.moyenneApprenants(), t.heuresBenevolat(), t.heuresApprenants());

        byte[] fichier = ClasseurExcel.creer(onglet, titre,
                perimetre + " · généré le " + aujourdHui.format(jj), colonnes, valeurs, total);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nom + ".xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(fichier);
    }

    /** Bilan d'activité annuel, mis en page pour l'impression (« Enregistrer en PDF »). */
    @GetMapping("/bilan")
    public String bilan(@RequestParam(required = false) Integer annee,
                        @RequestParam(required = false) Long uniteId,
                        Model model) {
        int an = annee != null ? annee : LocalDate.now().getYear();
        List<LigneStat> parUnite = statsService.parUnite(an, uniteId);
        model.addAttribute("annee", an);
        model.addAttribute("uniteId", uniteId);
        model.addAttribute("perimetre", uniteId == null ? "Toutes les unités locales"
                : administrationService.toutesLesUnites().stream()
                    .filter(u -> u.getId().equals(uniteId)).map(u -> "Unité de " + u.getNom())
                    .findFirst().orElse(""));
        model.addAttribute("comparaison", statsService.comparaison(an, uniteId));
        model.addAttribute("parUnite", parUnite);
        model.addAttribute("totaux", statsService.totaux(parUnite));
        model.addAttribute("parMois", statsService.parMoisToutesUnites(an, uniteId));
        model.addAttribute("graphique", statsService.graphiqueMensuel(an, uniteId));
        model.addAttribute("genereLe", java.time.LocalDateTime.now());
        return "admin/bilan";
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
