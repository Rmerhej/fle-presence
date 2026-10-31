package fr.asso.fle.web;

import fr.asso.fle.model.Seance;
import fr.asso.fle.model.StatutSeance;
import fr.asso.fle.model.UniteLocale;
import fr.asso.fle.repository.UniteLocaleRepository;
import fr.asso.fle.service.SeanceService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Calendrier au format iCalendar (.ics) : les apprenants s'y abonnent depuis leur téléphone,
 * et les annulations / reports apparaissent automatiquement dans leur agenda.
 * Aucun compte, aucune donnée personnelle.
 */
@RestController
public class CalendrierController {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final DateTimeFormatter UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final int SEMAINES_PASSEES = 2;
    private static final int SEMAINES_FUTURES = 12;

    private final UniteLocaleRepository uniteRepository;
    private final SeanceService seanceService;
    private final Formatage fmt;

    public CalendrierController(UniteLocaleRepository uniteRepository, SeanceService seanceService, Formatage fmt) {
        this.uniteRepository = uniteRepository;
        this.seanceService = seanceService;
        this.fmt = fmt;
    }

    @GetMapping(value = "/u/{slug}/calendrier.ics")
    public ResponseEntity<byte[]> calendrier(@PathVariable String slug,
                                             @RequestParam(required = false) Long groupe) {
        UniteLocale unite = uniteRepository.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        LocalDate aujourdHui = LocalDate.now(PARIS);
        List<Seance> seances = seanceService
                .seancesEntre(unite, aujourdHui.minusWeeks(SEMAINES_PASSEES), aujourdHui.plusWeeks(SEMAINES_FUTURES))
                .stream()
                .filter(s -> groupe == null || s.getGroupe().getId().equals(groupe))
                .toList();

        String nomCalendrier = "Cours de français – " + unite.getNom()
                + (groupe != null && !seances.isEmpty() ? " (" + seances.get(0).getGroupe().getNom() + ")" : "");
        String maintenant = UTC.format(LocalDateTime.now(ZoneOffset.UTC));

        StringBuilder ics = new StringBuilder();
        ligne(ics, "BEGIN:VCALENDAR");
        ligne(ics, "VERSION:2.0");
        ligne(ics, "PRODID:-//FLE Presence//Cours de francais//FR");
        ligne(ics, "CALSCALE:GREGORIAN");
        ligne(ics, "METHOD:PUBLISH");
        ligne(ics, "X-WR-CALNAME:" + echapper(nomCalendrier));
        ligne(ics, "X-WR-TIMEZONE:Europe/Paris");
        // Les agendas relisent le calendrier régulièrement pour voir les annulations
        ligne(ics, "REFRESH-INTERVAL;VALUE=DURATION:PT4H");
        ligne(ics, "X-PUBLISHED-TTL:PT4H");

        for (Seance s : seances) {
            ligne(ics, "BEGIN:VEVENT");
            ligne(ics, "UID:seance-" + s.getId() + "@fle-presence");
            ligne(ics, "DTSTAMP:" + maintenant);
            ligne(ics, "DTSTART:" + enUtc(s.getDate(), s.getHeureDebut()));
            ligne(ics, "DTEND:" + enUtc(s.getDate(), s.getHeureFin()));
            ligne(ics, "SUMMARY:" + echapper(titre(s)));
            String lieu = lieu(s, unite);
            if (!lieu.isEmpty()) {
                ligne(ics, "LOCATION:" + echapper(lieu));
            }
            ligne(ics, "DESCRIPTION:" + echapper(description(s)));
            ligne(ics, "STATUS:" + (s.getStatut() == StatutSeance.PREVUE ? "CONFIRMED" : "CANCELLED"));
            if (s.getModifieeLe() != null) {
                ligne(ics, "LAST-MODIFIED:" + UTC.format(s.getModifieeLe().atZone(PARIS).withZoneSameInstant(ZoneOffset.UTC)));
                // SEQUENCE augmente à chaque modification pour que les agendas mettent l'événement à jour
                ligne(ics, "SEQUENCE:" + s.getModifieeLe().atZone(PARIS).toEpochSecond() / 60 % 100000);
            }
            ligne(ics, "END:VEVENT");
        }
        ligne(ics, "END:VCALENDAR");

        return ResponseEntity.ok()
                .contentType(new MediaType("text", "calendar", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"cours-" + unite.getSlug() + ".ics\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .body(ics.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String titre(Seance s) {
        String base = "Cours de français – " + s.getGroupe().getNom();
        return switch (s.getStatut()) {
            case ANNULEE -> "ANNULÉ · " + base;
            case REPORTEE -> s.getReporteeVers() != null
                    ? "DÉPLACÉ au " + fmt.date(s.getReporteeVers().getDate()) + " · " + base
                    : "DÉPLACÉ · " + base;
            default -> base;
        };
    }

    private String description(Seance s) {
        StringBuilder d = new StringBuilder();
        switch (s.getStatut()) {
            case ANNULEE -> d.append("Pas de cours ce jour-là.");
            case REPORTEE -> {
                d.append("Ce cours est déplacé.");
                if (s.getReporteeVers() != null) {
                    d.append(" Nouveau cours : ").append(fmt.date(s.getReporteeVers().getDate()))
                     .append(" à ").append(fmt.heure(s.getReporteeVers().getHeureDebut())).append('.');
                }
            }
            default -> d.append(fmt.heure(s.getHeureDebut())).append(" – ").append(fmt.heure(s.getHeureFin()));
        }
        if (s.getMotif() != null) {
            d.append("\n").append(s.getMotif());
        }
        return d.toString();
    }

    private static String lieu(Seance s, UniteLocale unite) {
        String salle = s.getSalle() == null ? "" : s.getSalle();
        String adresse = unite.getAdresse() == null ? "" : unite.getAdresse();
        if (!salle.isEmpty() && !adresse.isEmpty()) {
            return salle + ", " + adresse;
        }
        return salle + adresse;
    }

    private static String enUtc(LocalDate date, LocalTime heure) {
        return UTC.format(ZonedDateTime.of(date, heure, PARIS).withZoneSameInstant(ZoneOffset.UTC));
    }

    /** Échappement iCalendar (RFC 5545 §3.3.11). */
    private static String echapper(String texte) {
        return texte.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r", "")
                .replace("\n", "\\n");
    }

    /** Ajoute une ligne, repliée à 74 caractères comme le demande la norme. */
    private static void ligne(StringBuilder ics, String contenu) {
        int max = 74;
        int i = 0;
        while (contenu.length() - i > max) {
            ics.append(contenu, i, i + max).append("\r\n ");
            i += max;
            max = 73; // les lignes de continuation commencent par une espace
        }
        ics.append(contenu.substring(i)).append("\r\n");
    }
}
