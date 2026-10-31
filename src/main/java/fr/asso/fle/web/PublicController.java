package fr.asso.fle.web;

import fr.asso.fle.model.Groupe;
import fr.asso.fle.model.Seance;
import fr.asso.fle.model.StatutSeance;
import fr.asso.fle.model.UniteLocale;
import fr.asso.fle.repository.UniteLocaleRepository;
import fr.asso.fle.service.AdministrationService;
import fr.asso.fle.service.SeanceService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pages publiques, sans compte : accueil et planning de chaque unité pour les apprenants. */
@Controller
public class PublicController {

    /** Nombre de semaines de planning affichées aux apprenants. */
    private static final int SEMAINES_AFFICHEES = 5;

    private final UniteLocaleRepository uniteRepository;
    private final AdministrationService administrationService;
    private final SeanceService seanceService;

    public PublicController(UniteLocaleRepository uniteRepository,
                            AdministrationService administrationService,
                            SeanceService seanceService) {
        this.uniteRepository = uniteRepository;
        this.administrationService = administrationService;
        this.seanceService = seanceService;
    }

    @GetMapping("/")
    public String accueil(Model model) {
        model.addAttribute("unites", administrationService.unitesActives());
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/plan-du-site")
    public String planDuSite(Model model) {
        model.addAttribute("unites", administrationService.unitesActives());
        return "plan";
    }

    @GetMapping("/confidentialite")
    public String confidentialite() {
        return "confidentialite";
    }

    @GetMapping("/u/{slug}")
    public String planning(@PathVariable String slug,
                           @RequestParam(required = false) Long groupe,
                           Model model) {
        UniteLocale unite = uniteRepository.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        List<Groupe> groupes = administrationService.groupes(unite);
        LocalDate aujourdHui = LocalDate.now();
        List<Seance> seances = seanceService.seancesEntre(unite, aujourdHui, aujourdHui.plusWeeks(SEMAINES_AFFICHEES))
                .stream()
                .filter(s -> groupe == null || s.getGroupe().getId().equals(groupe))
                .toList();

        // Séances regroupées par jour, dans l'ordre chronologique.
        Map<LocalDate, List<Seance>> parJour = new LinkedHashMap<>();
        for (Seance s : seances) {
            parJour.computeIfAbsent(s.getDate(), d -> new java.util.ArrayList<>()).add(s);
        }

        // Alertes : séances annulées ou déplacées à venir → affichées en toast et en bandeau.
        List<Seance> alertes = seances.stream()
                .filter(s -> s.getStatut() != StatutSeance.PREVUE)
                .toList();

        model.addAttribute("unite", unite);
        model.addAttribute("groupes", groupes);
        model.addAttribute("groupeChoisi", groupe);
        model.addAttribute("parJour", parJour);
        model.addAttribute("alertes", alertes);
        model.addAttribute("aujourdHui", aujourdHui);
        model.addAttribute("recemment", LocalDateTime.now().minusDays(7));

        // Abonnement au calendrier (.ics) : agenda du téléphone, Google Agenda, téléchargement
        String ics = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/u/{slug}/calendrier.ics")
                .queryParamIfPresent("groupe", java.util.Optional.ofNullable(groupe))
                .buildAndExpand(unite.getSlug()).toUriString();
        String webcal = ics.replaceFirst("^https?://", "webcal://");
        model.addAttribute("lienIcs", ics);
        model.addAttribute("manifestUrl", "/u/" + unite.getSlug() + "/manifest.json");
        model.addAttribute("lienWebcal", webcal);
        model.addAttribute("lienGoogle", "https://calendar.google.com/calendar/render?cid="
                + java.net.URLEncoder.encode(webcal, java.nio.charset.StandardCharsets.UTF_8));
        return "public/unite";
    }
}
