package fr.asso.fle.web;

import fr.asso.fle.model.UniteLocale;
import fr.asso.fle.repository.UniteLocaleRepository;
import fr.asso.fle.service.NotificationService;
import fr.asso.fle.service.RegleMetierException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Application installable (manifeste) et notifications push.
 * Points d'accès publics : les apprenants n'ont pas de compte.
 */
@RestController
public class PwaController {

    private static final MediaType MANIFESTE = MediaType.parseMediaType("application/manifest+json");

    private final UniteLocaleRepository uniteRepository;
    private final NotificationService notificationService;

    public PwaController(UniteLocaleRepository uniteRepository, NotificationService notificationService) {
        this.uniteRepository = uniteRepository;
        this.notificationService = notificationService;
    }

    // ================================================================ manifestes

    /** Manifeste général : l'application installée s'ouvre sur l'accueil. */
    @GetMapping("/manifest.json")
    public ResponseEntity<Map<String, Object>> manifeste() {
        return ResponseEntity.ok().contentType(MANIFESTE)
                .body(manifeste("FLE Présence – Cours de français", "Cours FLE", "/", "/"));
    }

    /** Manifeste d'une unité : installée depuis la page de Chatou, l'application s'ouvre sur Chatou. */
    @GetMapping("/u/{slug}/manifest.json")
    public ResponseEntity<Map<String, Object>> manifesteUnite(@PathVariable String slug) {
        UniteLocale unite = uniteRepository.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok().contentType(MANIFESTE)
                .body(manifeste("Cours de français – " + unite.getNom(), "Cours " + unite.getNom(),
                        "/u/" + unite.getSlug(), "/u/" + unite.getSlug() + "/"));
    }

    private static Map<String, Object> manifeste(String nom, String nomCourt, String depart, String id) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", nom);
        m.put("short_name", nomCourt.length() > 12 ? "Cours FLE" : nomCourt);
        m.put("description", "Dates des cours de français, annulations et reports.");
        m.put("id", id);
        m.put("start_url", depart);
        m.put("scope", "/");
        m.put("display", "standalone");
        m.put("lang", "fr");
        m.put("background_color", "#f5f7fb");
        m.put("theme_color", "#3459e6");
        m.put("icons", List.of(
                icone("/icons/icon-192.png", "192x192", "any"),
                icone("/icons/icon-512.png", "512x512", "any"),
                icone("/icons/icon-maskable-512.png", "512x512", "maskable")));
        return m;
    }

    private static Map<String, String> icone(String src, String taille, String usage) {
        return Map.of("src", src, "sizes", taille, "type", "image/png", "purpose", usage);
    }

    // ================================================================ notifications

    @GetMapping("/push/cle")
    public Map<String, Object> cle() {
        if (!notificationService.isActives()) {
            return Map.of("actives", false);
        }
        return Map.of("actives", true, "cle", notificationService.clePublique());
    }

    /** Corps envoyé par le navigateur : { unite, groupe, endpoint, keys: { p256dh, auth } } */
    public record DemandeAbonnement(String unite, Long groupe, String endpoint, Map<String, String> keys) {
    }

    public record DemandeDesabonnement(String endpoint) {
    }

    @PostMapping("/push/abonnement")
    public ResponseEntity<Map<String, Object>> abonner(@RequestBody DemandeAbonnement d) {
        try {
            Map<String, String> cles = d.keys() == null ? Map.of() : d.keys();
            notificationService.abonner(d.unite(), d.groupe(), d.endpoint(), cles.get("p256dh"), cles.get("auth"));
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (RegleMetierException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/push/desabonnement")
    public Map<String, Object> desabonner(@RequestBody DemandeDesabonnement d) {
        notificationService.desabonner(d.endpoint());
        return Map.of("ok", true);
    }
}
