package fr.asso.fle.service;

import fr.asso.fle.repository.PresenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

/** Statistiques du tableau de bord administrateur. */
@Service
@Transactional(readOnly = true)
public class StatsService {

    private static final Locale FR = Locale.FRANCE;
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("EEE dd/MM/yyyy", FR);

    /** Une ligne de tableau : une période pour une unité. */
    public record LigneStat(String periode, String unite, long seances, long benevoles, long apprenants) {
        public double moyenneApprenants() {
            return seances == 0 ? 0 : (double) apprenants / seances;
        }

        public double moyenneBenevoles() {
            return seances == 0 ? 0 : (double) benevoles / seances;
        }
    }

    public record Totaux(long seances, long benevoles, long apprenants) {
        public double moyenneApprenants() {
            return seances == 0 ? 0 : (double) apprenants / seances;
        }

        public double moyenneBenevoles() {
            return seances == 0 ? 0 : (double) benevoles / seances;
        }
    }

    /** Une courbe du graphique mensuel (une par unité). */
    public record Serie(String unite, List<Long> apprenants, List<Long> benevoles) {
    }

    public record GraphiqueMensuel(List<String> mois, List<Serie> series) {
    }

    private final PresenceRepository presenceRepository;

    public StatsService(PresenceRepository presenceRepository) {
        this.presenceRepository = presenceRepository;
    }

    public List<LigneStat> parJour(LocalDate du, LocalDate au, Long uniteId) {
        return presenceRepository.totauxParJour(du, au, uniteId).stream()
                .map(r -> new LigneStat(JOUR.format((LocalDate) r[0]), (String) r[2],
                        num(r[3]), num(r[4]), num(r[5])))
                .toList();
    }

    public List<LigneStat> parMois(int annee, Long uniteId) {
        return presenceRepository.totauxParMois(annee, uniteId).stream()
                .map(r -> new LigneStat(nomMois((int) num(r[0])) + " " + annee, (String) r[2],
                        num(r[3]), num(r[4]), num(r[5])))
                .toList();
    }

    public List<LigneStat> parAnnee(Long uniteId) {
        return presenceRepository.totauxParAnnee(uniteId).stream()
                .map(r -> new LigneStat(String.valueOf(num(r[0])), (String) r[2],
                        num(r[3]), num(r[4]), num(r[5])))
                .toList();
    }

    public Totaux totaux(List<LigneStat> lignes) {
        long s = 0, b = 0, a = 0;
        for (LigneStat l : lignes) {
            s += l.seances();
            b += l.benevoles();
            a += l.apprenants();
        }
        return new Totaux(s, b, a);
    }

    /** Données du graphique : apprenants et bénévoles par mois, une série par unité. */
    public GraphiqueMensuel graphiqueMensuel(int annee, Long uniteId) {
        Map<String, long[][]> parUnite = new TreeMap<>();
        for (Object[] r : presenceRepository.totauxParMois(annee, uniteId)) {
            int mois = (int) num(r[0]);
            long[][] valeurs = parUnite.computeIfAbsent((String) r[2], k -> new long[2][12]);
            valeurs[0][mois - 1] = num(r[5]); // apprenants
            valeurs[1][mois - 1] = num(r[4]); // bénévoles
        }
        List<Serie> series = new ArrayList<>();
        parUnite.forEach((unite, v) -> series.add(new Serie(unite,
                Arrays.stream(v[0]).boxed().toList(),
                Arrays.stream(v[1]).boxed().toList())));
        List<String> mois = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            mois.add(Month.of(m).getDisplayName(TextStyle.SHORT, FR));
        }
        return new GraphiqueMensuel(mois, series);
    }

    private static String nomMois(int m) {
        String nom = Month.of(m).getDisplayName(TextStyle.FULL, FR);
        return nom.substring(0, 1).toUpperCase(FR) + nom.substring(1);
    }

    private static long num(Object o) {
        return o == null ? 0 : ((Number) o).longValue();
    }
}
