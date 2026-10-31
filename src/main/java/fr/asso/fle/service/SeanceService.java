package fr.asso.fle.service;

import fr.asso.fle.model.*;
import fr.asso.fle.repository.CreneauRepository;
import fr.asso.fle.repository.GroupeRepository;
import fr.asso.fle.repository.PresenceRepository;
import fr.asso.fle.repository.SeanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Gestion du planning : génération, ajout, annulation et report des séances. */
@Service
public class SeanceService {

    private static final Logger log = LoggerFactory.getLogger(SeanceService.class);

    /** Nombre de semaines générées à l'avance automatiquement. */
    public static final int SEMAINES_AVANCE = 8;

    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRANCE);

    /** Événement publié quand des séances sont annulées ou déplacées (pour les notifications). */
    public record SeancesModifiees(List<Long> seanceIds) {
    }

    /** Résultat d'une annulation sur une période. */
    public record ResultatAnnulation(int annulees, int ignorees) {
    }

    private final SeanceRepository seanceRepository;
    private final CreneauRepository creneauRepository;
    private final GroupeRepository groupeRepository;
    private final PresenceRepository presenceRepository;
    private final JournalService journal;
    private final ApplicationEventPublisher evenements;

    public SeanceService(SeanceRepository seanceRepository,
                         CreneauRepository creneauRepository,
                         GroupeRepository groupeRepository,
                         PresenceRepository presenceRepository,
                         JournalService journal,
                         ApplicationEventPublisher evenements) {
        this.seanceRepository = seanceRepository;
        this.creneauRepository = creneauRepository;
        this.groupeRepository = groupeRepository;
        this.presenceRepository = presenceRepository;
        this.journal = journal;
        this.evenements = evenements;
    }

    // ------------------------------------------------------------------ lecture

    @Transactional(readOnly = true)
    public List<Seance> seancesEntre(UniteLocale unite, LocalDate du, LocalDate au) {
        return seanceRepository.findParUniteEntre(unite, du, au);
    }

    @Transactional(readOnly = true)
    public List<Creneau> creneauxTries(UniteLocale unite) {
        return creneauRepository.findActifsParUnite(unite).stream()
                .sorted(Comparator.comparing((Creneau c) -> c.getGroupe().getNom())
                        .thenComparing(Creneau::getJourSemaine)
                        .thenComparing(Creneau::getHeureDebut))
                .toList();
    }

    @Transactional(readOnly = true)
    public Seance seanceDeLUnite(Long seanceId, UniteLocale unite) {
        Seance seance = seanceRepository.findById(seanceId)
                .orElseThrow(() -> new RegleMetierException("Séance introuvable."));
        if (unite == null || !seance.getGroupe().getUnite().getId().equals(unite.getId())) {
            throw new RegleMetierException("Cette séance n'appartient pas à votre unité.");
        }
        return seance;
    }

    // ------------------------------------------------------------- génération

    /**
     * Crée les séances manquantes entre deux dates à partir des créneaux actifs.
     * Les séances déjà existantes (même groupe, date et heure) ne sont pas dupliquées.
     */
    @Transactional
    public int genererDepuisCreneaux(List<Creneau> creneaux, LocalDate du, LocalDate au) {
        int crees = 0;
        for (Creneau creneau : creneaux) {
            LocalDate date = du.with(TemporalAdjusters.nextOrSame(creneau.getJourSemaine()));
            while (!date.isAfter(au)) {
                if (!seanceRepository.existsByGroupeAndDateAndHeureDebut(
                        creneau.getGroupe(), date, creneau.getHeureDebut())) {
                    Seance s = new Seance(creneau.getGroupe(), date,
                            creneau.getHeureDebut(), creneau.getHeureFin(), creneau.getSalle());
                    s.setCreneau(creneau);
                    seanceRepository.save(s);
                    crees++;
                }
                date = date.plusWeeks(1);
            }
        }
        return crees;
    }

    @Transactional
    public int genererPourUnite(UniteLocale unite, int semaines) {
        if (semaines < 1 || semaines > 26) {
            throw new RegleMetierException("Choisissez entre 1 et 26 semaines.");
        }
        LocalDate debut = LocalDate.now();
        int n = genererDepuisCreneaux(creneauRepository.findActifsParUnite(unite),
                debut, debut.plusWeeks(semaines));
        if (n > 0) {
            journal.noter(unite, TypeAction.PLANNING_GENERE, n + " séance(s) ajoutée(s) sur " + semaines + " semaines");
        }
        return n;
    }

    /** Chaque nuit à 3h, prolonge le planning de toutes les unités. */
    @Scheduled(cron = "0 0 3 * * *", zone = "Europe/Paris")
    @Transactional
    public void generationAutomatique() {
        LocalDate debut = LocalDate.now();
        int n = genererDepuisCreneaux(creneauRepository.findTousActifs(), debut, debut.plusWeeks(SEMAINES_AVANCE));
        log.info("Génération automatique du planning : {} séance(s) créée(s)", n);
    }

    // ------------------------------------------------------------- créneaux

    @Transactional
    public Creneau ajouterCreneau(UniteLocale unite, Long groupeId, java.time.DayOfWeek jour,
                                  LocalTime debut, LocalTime fin, String salle) {
        Groupe groupe = groupeDeLUnite(groupeId, unite);
        verifierHoraires(debut, fin);
        Creneau c = creneauRepository.save(new Creneau(groupe, jour, debut, fin, nettoyer(salle)));
        // Le planning des prochaines semaines est créé tout de suite.
        genererDepuisCreneaux(List.of(c), LocalDate.now(), LocalDate.now().plusWeeks(SEMAINES_AVANCE));
        journal.noter(unite, TypeAction.CRENEAU_AJOUTE, groupe.getNom() + " · " + jourSemaine(jour)
                + " " + heure(debut) + " – " + heure(fin));
        return c;
    }

    /**
     * Désactive un créneau : les séances futures non saisies issues de ce créneau
     * sont supprimées du planning.
     */
    @Transactional
    public void supprimerCreneau(UniteLocale unite, Long creneauId) {
        Creneau c = creneauRepository.findById(creneauId)
                .orElseThrow(() -> new RegleMetierException("Créneau introuvable."));
        if (!c.getGroupe().getUnite().getId().equals(unite.getId())) {
            throw new RegleMetierException("Ce créneau n'appartient pas à votre unité.");
        }
        c.setActif(false);
        seanceRepository.findParUniteEntre(unite, LocalDate.now().plusDays(1), LocalDate.now().plusYears(1))
                .stream()
                .filter(s -> s.getCreneau() != null && s.getCreneau().getId().equals(c.getId()))
                .filter(s -> s.getStatut() == StatutSeance.PREVUE)
                .forEach(seanceRepository::delete);
        journal.noter(unite, TypeAction.CRENEAU_SUPPRIME, c.getGroupe().getNom() + " · "
                + jourSemaine(c.getJourSemaine()) + " " + heure(c.getHeureDebut()));
    }

    // ------------------------------------------------------------- séances

    @Transactional
    public Seance ajouterSeancePonctuelle(UniteLocale unite, Long groupeId, LocalDate date,
                                          LocalTime debut, LocalTime fin, String salle) {
        Groupe groupe = groupeDeLUnite(groupeId, unite);
        verifierHoraires(debut, fin);
        if (date == null) {
            throw new RegleMetierException("La date est obligatoire.");
        }
        if (seanceRepository.existsByGroupeAndDateAndHeureDebut(groupe, date, debut)) {
            throw new RegleMetierException("Une séance existe déjà pour ce groupe à cette date et cette heure.");
        }
        Seance s = new Seance(groupe, date, debut, fin, nettoyer(salle));
        s.setModifieeLe(LocalDateTime.now());
        s = seanceRepository.save(s);
        journal.noter(unite, TypeAction.SEANCE_AJOUTEE, decrire(s));
        return s;
    }

    @Transactional
    public void annuler(UniteLocale unite, Long seanceId, String motif) {
        Seance s = seanceDeLUnite(seanceId, unite);
        if (s.getStatut() != StatutSeance.PREVUE) {
            throw new RegleMetierException("Seule une séance maintenue peut être annulée.");
        }
        verifierSansPresence(s);
        s.setStatut(StatutSeance.ANNULEE);
        s.setMotif(nettoyer(motif));
        s.setModifieeLe(LocalDateTime.now());
        journal.noter(unite, TypeAction.SEANCE_ANNULEE, decrire(s) + (s.getMotif() != null ? " · " + s.getMotif() : ""));
        evenements.publishEvent(new SeancesModifiees(List.of(s.getId())));
    }

    @Transactional
    public Seance reporter(UniteLocale unite, Long seanceId, LocalDate nouvelleDate,
                           LocalTime debut, LocalTime fin, String motif) {
        Seance s = seanceDeLUnite(seanceId, unite);
        if (s.getStatut() != StatutSeance.PREVUE) {
            throw new RegleMetierException("Seule une séance maintenue peut être reportée.");
        }
        verifierSansPresence(s);
        if (nouvelleDate == null) {
            throw new RegleMetierException("Indiquez la nouvelle date.");
        }
        LocalTime d = debut != null ? debut : s.getHeureDebut();
        LocalTime f = fin != null ? fin : s.getHeureFin();
        verifierHoraires(d, f);
        if (nouvelleDate.isEqual(s.getDate()) && d.equals(s.getHeureDebut())) {
            throw new RegleMetierException("La nouvelle date est identique à l'actuelle.");
        }
        if (seanceRepository.existsByGroupeAndDateAndHeureDebut(s.getGroupe(), nouvelleDate, d)) {
            throw new RegleMetierException("Le groupe a déjà une séance à cette date et cette heure.");
        }
        Seance nouvelle = new Seance(s.getGroupe(), nouvelleDate, d, f, s.getSalle());
        nouvelle.setModifieeLe(LocalDateTime.now());
        nouvelle = seanceRepository.save(nouvelle);

        s.setStatut(StatutSeance.REPORTEE);
        s.setMotif(nettoyer(motif));
        s.setReporteeVers(nouvelle);
        s.setModifieeLe(LocalDateTime.now());
        journal.noter(unite, TypeAction.SEANCE_REPORTEE, decrire(s) + " → " + JOUR.format(nouvelleDate)
                + " " + heure(d) + (s.getMotif() != null ? " · " + s.getMotif() : ""));
        evenements.publishEvent(new SeancesModifiees(List.of(s.getId())));
        return nouvelle;
    }

    /** Remet une séance annulée en « maintenue » (erreur de saisie, finalement maintenue...). */
    @Transactional
    public void retablir(UniteLocale unite, Long seanceId) {
        Seance s = seanceDeLUnite(seanceId, unite);
        if (s.getStatut() != StatutSeance.ANNULEE) {
            throw new RegleMetierException("Seule une séance annulée peut être rétablie.");
        }
        s.setStatut(StatutSeance.PREVUE);
        s.setMotif(null);
        s.setModifieeLe(LocalDateTime.now());
        journal.noter(unite, TypeAction.SEANCE_RETABLIE, decrire(s));
    }

    /**
     * Annule toutes les séances maintenues d'une période (vacances, fermeture de la salle...).
     * Les séances qui ont déjà des présences sont laissées telles quelles.
     *
     * @param groupeIds groupes concernés ; vide ou null = tous les groupes de l'unité
     */
    @Transactional
    public ResultatAnnulation annulerPeriode(UniteLocale unite, LocalDate du, LocalDate au,
                                             List<Long> groupeIds, String motif) {
        if (du == null || au == null) {
            throw new RegleMetierException("Indiquez les dates de début et de fin.");
        }
        if (au.isBefore(du)) {
            throw new RegleMetierException("La date de fin doit être après la date de début.");
        }
        if (ChronoUnit.DAYS.between(du, au) > 366) {
            throw new RegleMetierException("La période ne peut pas dépasser un an.");
        }
        boolean tousLesGroupes = groupeIds == null || groupeIds.isEmpty();
        List<Long> annulees = new ArrayList<>();
        int ignorees = 0;
        for (Seance s : seanceRepository.findParUniteEntre(unite, du, au)) {
            if (s.getStatut() != StatutSeance.PREVUE
                    || (!tousLesGroupes && !groupeIds.contains(s.getGroupe().getId()))) {
                continue;
            }
            if (presenceRepository.findBySeance(s).isPresent()) {
                ignorees++;
                continue;
            }
            s.setStatut(StatutSeance.ANNULEE);
            s.setMotif(nettoyer(motif));
            s.setModifieeLe(LocalDateTime.now());
            annulees.add(s.getId());
        }
        if (!annulees.isEmpty()) {
            journal.noter(unite, TypeAction.ANNULATION_GROUPEE, annulees.size() + " séance(s) du "
                    + JOUR.format(du) + " au " + JOUR.format(au)
                    + (motif != null && !motif.isBlank() ? " · " + motif.strip() : ""));
            evenements.publishEvent(new SeancesModifiees(annulees));
        }
        return new ResultatAnnulation(annulees.size(), ignorees);
    }

    // ------------------------------------------------------------- outils

    private Groupe groupeDeLUnite(Long groupeId, UniteLocale unite) {
        Groupe g = groupeRepository.findById(groupeId == null ? -1L : groupeId)
                .orElseThrow(() -> new RegleMetierException("Choisissez un groupe."));
        if (!g.getUnite().getId().equals(unite.getId()) || !g.isActif()) {
            throw new RegleMetierException("Ce groupe n'appartient pas à votre unité.");
        }
        return g;
    }

    private void verifierSansPresence(Seance s) {
        if (presenceRepository.findBySeance(s).isPresent()) {
            throw new RegleMetierException("Des présences ont déjà été saisies pour cette séance : elle a donc eu lieu.");
        }
    }

    private static void verifierHoraires(LocalTime debut, LocalTime fin) {
        if (debut == null || fin == null) {
            throw new RegleMetierException("Les heures de début et de fin sont obligatoires.");
        }
        if (!fin.isAfter(debut)) {
            throw new RegleMetierException("L'heure de fin doit être après l'heure de début.");
        }
    }

    /** « Débutants · mardi 4 novembre 14h00 » */
    static String decrire(Seance s) {
        return s.getGroupe().getNom() + " · " + JOUR.format(s.getDate()) + " " + heure(s.getHeureDebut());
    }

    static String heure(LocalTime t) {
        return String.format("%dh%02d", t.getHour(), t.getMinute());
    }

    private static String jourSemaine(java.time.DayOfWeek j) {
        return j.getDisplayName(java.time.format.TextStyle.FULL, Locale.FRANCE);
    }

    static String nettoyer(String texte) {
        if (texte == null || texte.isBlank()) {
            return null;
        }
        String t = texte.strip();
        return t.length() > 255 ? t.substring(0, 255) : t;
    }
}
