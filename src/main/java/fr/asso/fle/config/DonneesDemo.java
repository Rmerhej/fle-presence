package fr.asso.fle.config;

import fr.asso.fle.model.*;
import fr.asso.fle.repository.*;
import fr.asso.fle.service.SeanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Random;

/**
 * Données d'exemple, chargées UNIQUEMENT avec le profil « dev ».
 * Comptes : admin / admin1234, resp.chatou / responsable, benevole.chatou / benevole1 ...
 */
@Component
@Profile("dev")
@Order(0)
public class DonneesDemo implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DonneesDemo.class);

    private final UniteLocaleRepository uniteRepo;
    private final GroupeRepository groupeRepo;
    private final CreneauRepository creneauRepo;
    private final SeanceRepository seanceRepo;
    private final PresenceRepository presenceRepo;
    private final UtilisateurRepository userRepo;
    private final SeanceService seanceService;
    private final PasswordEncoder encoder;

    public DonneesDemo(UniteLocaleRepository uniteRepo, GroupeRepository groupeRepo,
                       CreneauRepository creneauRepo, SeanceRepository seanceRepo,
                       PresenceRepository presenceRepo, UtilisateurRepository userRepo,
                       SeanceService seanceService, PasswordEncoder encoder) {
        this.uniteRepo = uniteRepo;
        this.groupeRepo = groupeRepo;
        this.creneauRepo = creneauRepo;
        this.seanceRepo = seanceRepo;
        this.presenceRepo = presenceRepo;
        this.userRepo = userRepo;
        this.seanceService = seanceService;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (uniteRepo.count() > 0) {
            return;
        }
        UniteLocale chatou = uniteRepo.save(new UniteLocale("Chatou", "chatou", "Maison des associations, Chatou"));
        UniteLocale sartrouville = uniteRepo.save(new UniteLocale("Sartrouville", "sartrouville", "Salle Jean Moulin, Sartrouville"));
        UniteLocale houilles = uniteRepo.save(new UniteLocale("Houilles", "houilles", "Centre social, Houilles"));

        Groupe chDeb = groupeRepo.save(new Groupe("Débutants", "A1", chatou));
        Groupe chInt = groupeRepo.save(new Groupe("Intermédiaires", "A2", chatou));
        Groupe sart = groupeRepo.save(new Groupe("Groupe unique", "A1-A2", sartrouville));
        Groupe hou = groupeRepo.save(new Groupe("Alphabétisation", "Alpha", houilles));

        List<Creneau> creneaux = List.of(
                creneauRepo.save(new Creneau(chDeb, DayOfWeek.TUESDAY, LocalTime.of(14, 0), LocalTime.of(16, 0), "Salle 2")),
                creneauRepo.save(new Creneau(chDeb, DayOfWeek.THURSDAY, LocalTime.of(14, 0), LocalTime.of(16, 0), "Salle 2")),
                creneauRepo.save(new Creneau(chInt, DayOfWeek.WEDNESDAY, LocalTime.of(18, 30), LocalTime.of(20, 30), "Salle 1")),
                creneauRepo.save(new Creneau(sart, DayOfWeek.MONDAY, LocalTime.of(9, 30), LocalTime.of(11, 30), null)),
                creneauRepo.save(new Creneau(hou, DayOfWeek.SATURDAY, LocalTime.of(10, 0), LocalTime.of(12, 0), "Bibliothèque")));

        userRepo.save(new Utilisateur("admin", encoder.encode("admin1234"), "Administrateur", Role.ADMIN, null));
        userRepo.save(new Utilisateur("resp.chatou", encoder.encode("responsable"), "Marie (resp. Chatou)", Role.RESPONSABLE, chatou));
        userRepo.save(new Utilisateur("benevole.chatou", encoder.encode("benevole1"), "Paul", Role.BENEVOLE, chatou));
        userRepo.save(new Utilisateur("resp.sartrouville", encoder.encode("responsable"), "Karim (resp. Sartrouville)", Role.RESPONSABLE, sartrouville));
        userRepo.save(new Utilisateur("benevole.sartrouville", encoder.encode("benevole1"), "Sophie", Role.BENEVOLE, sartrouville));
        userRepo.save(new Utilisateur("resp.houilles", encoder.encode("responsable"), "Jeanne (resp. Houilles)", Role.RESPONSABLE, houilles));

        LocalDate today = LocalDate.now();
        seanceService.genererDepuisCreneaux(creneaux, today.minusDays(300), today.plusWeeks(SeanceService.SEMAINES_AVANCE));

        // Présences aléatoires sur les séances passées (sauf les 7 derniers jours, laissés « à saisir »).
        Random r = new Random(42);
        for (UniteLocale u : List.of(chatou, sartrouville, houilles)) {
            for (Seance s : seanceRepo.findParUniteEntre(u, today.minusDays(300), today.minusDays(7))) {
                if (r.nextInt(12) == 0) {
                    s.setStatut(StatutSeance.ANNULEE);
                    s.setMotif("Salle indisponible");
                    continue;
                }
                Presence p = new Presence();
                p.setSeance(s);
                p.setNbBenevoles(1 + r.nextInt(3));
                p.setNbApprenants(4 + r.nextInt(11));
                p.setSaisiPar("benevole." + u.getSlug());
                p.setSaisiLe(LocalDateTime.of(s.getDate(), s.getHeureFin()));
                presenceRepo.save(p);
            }
        }

        // Une annulation et un report à venir pour Chatou, pour voir les alertes sur la page publique.
        List<Seance> aVenir = seanceRepo.findParUniteEntre(chatou, today.plusDays(1), today.plusWeeks(3));
        aVenir.stream().filter(s -> s.getGroupe().getId().equals(chDeb.getId())).findFirst()
                .ifPresent(s -> seanceService.annuler(chatou, s.getId(), "Formation des bénévoles"));
        aVenir.stream().filter(s -> s.getGroupe().getId().equals(chInt.getId())).findFirst()
                .ifPresent(s -> seanceService.reporter(chatou, s.getId(), s.getDate().plusDays(1),
                        null, null, "Réunion dans la salle"));

        log.info("Données de démonstration chargées (profil dev).");
    }
}
