package fr.asso.fle.service;

import fr.asso.fle.model.Presence;
import fr.asso.fle.model.Seance;
import fr.asso.fle.repository.PresenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.function.Function;

/**
 * Statistiques du tableau de bord et du bilan annuel.
 * Les calculs sont faits en Java à partir des présences, ce qui permet d'obtenir
 * les heures de bénévolat (bénévoles présents × durée de la séance).
 */
@Service
@Transactional(readOnly = true)
public class StatsService {

    private static final Locale FR = Locale.FRANCE;
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("EEE dd/MM/yyyy", FR);
    private static final LocalDate DEBUT_HISTORIQUE = LocalDate.of(2000, 1, 1);
    private static final LocalDate FIN_HISTORIQUE = LocalDate.of(2100, 12, 31);

    // ======================================================================= types

    /** Une ligne de tableau : une période pour une unité. */
    public record LigneStat(String periode, String unite, long seances, long benevoles, long apprenants,
                            double heuresBenevolat, double heuresApprenants) {
        public double moyenneApprenants() {
            return seances == 0 ? 0 : (double) apprenants / seances;
        }

        public double moyenneBenevoles() {
            return seances == 0 ? 0 : (double) benevoles / seances;
        }
    }

    public record Totaux(long seances, long benevoles, long apprenants,
                         double heuresBenevolat, double heuresApprenants) {
        public double moyenneApprenants() {
            return seances == 0 ? 0 : (double) apprenants / seances;
        }

        public double moyenneBenevoles() {
            return seances == 0 ? 0 : (double) benevoles / seances;
        }
    }

    /** Année (ou période) comparée à la même période de l'année précédente. */
    public record Comparaison(Totaux actuel, Totaux precedent, String libelle) {
        public Integer evoSeances() { return variation(actuel.seances(), precedent.seances()); }
        public Integer evoApprenants() { return variation(actuel.apprenants(), precedent.apprenants()); }
        public Integer evoBenevoles() { return variation(actuel.benevoles(), precedent.benevoles()); }
        public Integer evoHeuresBenevolat() { return variation(actuel.heuresBenevolat(), precedent.heuresBenevolat()); }
        public Integer evoMoyenne() { return variation(actuel.moyenneApprenants(), precedent.moyenneApprenants()); }

        /** Évolution en % arrondie, ou null si pas de point de comparaison. */
        private static Integer variation(double maintenant, double avant) {
            if (avant == 0) {
                return null;
            }
            return (int) Math.round((maintenant - avant) * 100.0 / avant);
        }
    }

    /** Une courbe du graphique mensuel (une par unité). */
    public record Serie(String unite, List<Long> apprenants, List<Long> benevoles, List<Double> heures) {
    }

    public record GraphiqueMensuel(List<String> mois, List<Serie> series) {
    }

    /** Accumulateur interne. */
    private static final class Cumul {
        long seances, benevoles, apprenants;
        double heuresBenevolat, heuresApprenants;

        void ajouter(Presence p) {
            double h = dureeHeures(p.getSeance());
            seances++;
            benevoles += p.getNbBenevoles();
            apprenants += p.getNbApprenants();
            heuresBenevolat += p.getNbBenevoles() * h;
            heuresApprenants += p.getNbApprenants() * h;
        }

        LigneStat ligne(String periode, String unite) {
            return new LigneStat(periode, unite, seances, benevoles, apprenants, heuresBenevolat, heuresApprenants);
        }
    }

    private record Cle(Comparable<?> periode, String unite) {
    }

    // ======================================================================= service

    private final PresenceRepository presenceRepository;

    public StatsService(PresenceRepository presenceRepository) {
        this.presenceRepository = presenceRepository;
    }

    private List<Presence> presences(LocalDate du, LocalDate au, Long uniteId) {
        return presenceRepository.findPourStats(du, au, uniteId);
    }

    /** Totaux par date et par unité, du plus récent au plus ancien. */
    public List<LigneStat> parJour(LocalDate du, LocalDate au, Long uniteId) {
        return grouper(presences(du, au, uniteId), p -> p.getSeance().getDate(), true,
                d -> JOUR.format((LocalDate) d));
    }

    /** Totaux par mois et par unité pour une année. */
    public List<LigneStat> parMois(int annee, Long uniteId) {
        return grouper(presences(LocalDate.of(annee, 1, 1), LocalDate.of(annee, 12, 31), uniteId),
                p -> p.getSeance().getDate().getMonthValue(), false,
                m -> nomMois((Integer) m) + " " + annee);
    }

    /** Totaux par année et par unité (toutes les années). */
    public List<LigneStat> parAnnee(Long uniteId) {
        return grouper(presences(DEBUT_HISTORIQUE, FIN_HISTORIQUE, uniteId),
                p -> p.getSeance().getDate().getYear(), true, String::valueOf);
    }

    /** Une ligne par unité pour l'année (bilan). */
    public List<LigneStat> parUnite(int annee, Long uniteId) {
        Map<String, Cumul> cumuls = new TreeMap<>();
        for (Presence p : presences(LocalDate.of(annee, 1, 1), LocalDate.of(annee, 12, 31), uniteId)) {
            cumuls.computeIfAbsent(p.getSeance().getGroupe().getUnite().getNom(), k -> new Cumul()).ajouter(p);
        }
        List<LigneStat> lignes = new ArrayList<>();
        cumuls.forEach((unite, c) -> lignes.add(c.ligne(String.valueOf(annee), unite)));
        return lignes;
    }

    /** Une ligne par mois, toutes unités confondues (bilan). */
    public List<LigneStat> parMoisToutesUnites(int annee, Long uniteId) {
        Map<Integer, Cumul> cumuls = new TreeMap<>();
        for (Presence p : presences(LocalDate.of(annee, 1, 1), LocalDate.of(annee, 12, 31), uniteId)) {
            cumuls.computeIfAbsent(p.getSeance().getDate().getMonthValue(), k -> new Cumul()).ajouter(p);
        }
        List<LigneStat> lignes = new ArrayList<>();
        cumuls.forEach((mois, c) -> lignes.add(c.ligne(nomMois(mois), "")));
        return lignes;
    }

    public Totaux totaux(List<LigneStat> lignes) {
        long s = 0, b = 0, a = 0;
        double hb = 0, ha = 0;
        for (LigneStat l : lignes) {
            s += l.seances();
            b += l.benevoles();
            a += l.apprenants();
            hb += l.heuresBenevolat();
            ha += l.heuresApprenants();
        }
        return new Totaux(s, b, a, hb, ha);
    }

    /**
     * Compare l'année à l'année précédente. Pour l'année en cours, on compare
     * du 1er janvier à aujourd'hui avec la même période un an plus tôt.
     */
    public Comparaison comparaison(int annee, Long uniteId) {
        LocalDate aujourdHui = LocalDate.now();
        LocalDate debut = LocalDate.of(annee, 1, 1);
        LocalDate fin = LocalDate.of(annee, 12, 31);
        String libelle = "par rapport à " + (annee - 1);
        if (annee == aujourdHui.getYear()) {
            fin = aujourdHui;
            libelle = "par rapport à la même période en " + (annee - 1);
        }
        Totaux actuel = totaux(Collections.singletonList(cumuler(presences(debut, fin, uniteId))));
        Totaux precedent = totaux(Collections.singletonList(
                cumuler(presences(debut.minusYears(1), fin.minusYears(1), uniteId))));
        return new Comparaison(actuel, precedent, libelle);
    }

    /** Données du graphique : apprenants, bénévoles et heures par mois, une série par unité. */
    public GraphiqueMensuel graphiqueMensuel(int annee, Long uniteId) {
        Map<String, Cumul[]> parUnite = new TreeMap<>();
        for (Presence p : presences(LocalDate.of(annee, 1, 1), LocalDate.of(annee, 12, 31), uniteId)) {
            Cumul[] mois = parUnite.computeIfAbsent(p.getSeance().getGroupe().getUnite().getNom(), k -> {
                Cumul[] t = new Cumul[12];
                for (int i = 0; i < 12; i++) {
                    t[i] = new Cumul();
                }
                return t;
            });
            mois[p.getSeance().getDate().getMonthValue() - 1].ajouter(p);
        }
        List<Serie> series = new ArrayList<>();
        parUnite.forEach((unite, mois) -> series.add(new Serie(unite,
                Arrays.stream(mois).map(c -> c.apprenants).toList(),
                Arrays.stream(mois).map(c -> c.benevoles).toList(),
                Arrays.stream(mois).map(c -> Math.round(c.heuresBenevolat * 10) / 10.0).toList())));
        List<String> noms = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            noms.add(Month.of(m).getDisplayName(TextStyle.SHORT, FR));
        }
        return new GraphiqueMensuel(noms, series);
    }

    // ======================================================================= outils

    private static LigneStat cumuler(List<Presence> presences) {
        Cumul c = new Cumul();
        presences.forEach(c::ajouter);
        return c.ligne("", "");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static List<LigneStat> grouper(List<Presence> presences,
                                           Function<Presence, Comparable<?>> periode,
                                           boolean decroissant,
                                           Function<Object, String> libelle) {
        Comparator<Cle> ordre = (x, y) -> {
            int c = ((Comparable) x.periode()).compareTo(y.periode());
            if (decroissant) {
                c = -c;
            }
            return c != 0 ? c : x.unite().compareTo(y.unite());
        };
        Map<Cle, Cumul> cumuls = new TreeMap<>(ordre);
        for (Presence p : presences) {
            Cle cle = new Cle(periode.apply(p), p.getSeance().getGroupe().getUnite().getNom());
            cumuls.computeIfAbsent(cle, k -> new Cumul()).ajouter(p);
        }
        List<LigneStat> lignes = new ArrayList<>();
        cumuls.forEach((cle, c) -> lignes.add(c.ligne(libelle.apply(cle.periode()), cle.unite())));
        return lignes;
    }

    static double dureeHeures(Seance s) {
        long minutes = Duration.between(s.getHeureDebut(), s.getHeureFin()).toMinutes();
        return Math.max(0, minutes) / 60.0;
    }

    private static String nomMois(int m) {
        String nom = Month.of(m).getDisplayName(TextStyle.FULL, FR);
        return nom.substring(0, 1).toUpperCase(FR) + nom.substring(1);
    }
}
