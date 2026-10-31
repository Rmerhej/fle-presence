package fr.asso.fle.web;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Formatage en français, utilisable dans les templates : ${@fmt.date(seance.date)}
 */
@Component("fmt")
public class Formatage {

    private static final Locale FR = Locale.FRANCE;
    private static final DateTimeFormatter DATE_LONGUE = DateTimeFormatter.ofPattern("EEEE d MMMM", FR);
    private static final DateTimeFormatter DATE_ANNEE = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", FR);
    private static final DateTimeFormatter DATE_COURTE = DateTimeFormatter.ofPattern("dd/MM/yyyy", FR);
    private static final DateTimeFormatter DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH'h'mm", FR);
    private static final DateTimeFormatter MOIS = DateTimeFormatter.ofPattern("MMMM yyyy", FR);

    /** « Mardi 4 novembre » (l'année est ajoutée si ce n'est pas l'année en cours). */
    public String date(LocalDate d) {
        if (d == null) {
            return "";
        }
        return majuscule((d.getYear() == LocalDate.now().getYear() ? DATE_LONGUE : DATE_ANNEE).format(d));
    }

    /** « 04/11/2026 » */
    public String dateCourte(LocalDate d) {
        return d == null ? "" : DATE_COURTE.format(d);
    }

    public String dateHeure(LocalDateTime d) {
        return d == null ? "" : DATE_HEURE.format(d);
    }

    /** « 14h00 » */
    public String heure(LocalTime t) {
        if (t == null) {
            return "";
        }
        return String.format("%dh%02d", t.getHour(), t.getMinute());
    }

    /** « Mardi » */
    public String jour(DayOfWeek j) {
        return j == null ? "" : majuscule(j.getDisplayName(TextStyle.FULL, FR));
    }

    /** « Novembre 2026 » */
    public String mois(YearMonth m) {
        return m == null ? "" : majuscule(MOIS.format(m));
    }

    /** Nombre décimal à la française : « 7,5 » */
    public String decimal(double v) {
        return String.format(FR, "%.1f", v);
    }

    /** Heures : « 1 234 h » ou « 12,5 h » */
    public String heures(double h) {
        double arrondi = Math.round(h * 10) / 10.0;
        return (arrondi == Math.rint(arrondi)
                ? String.format(FR, "%,.0f", arrondi)
                : String.format(FR, "%,.1f", arrondi)) + " h";
    }

    /** Évolution : « +12 % », « −5 % », ou chaîne vide s'il n'y a pas de comparaison. */
    public String evolution(Integer pct) {
        if (pct == null) {
            return "";
        }
        return (pct > 0 ? "+" : pct < 0 ? "−" : "") + Math.abs(pct) + " %";
    }

    /** Classe Bootstrap pour colorer une évolution. */
    public String couleurEvolution(Integer pct) {
        if (pct == null || pct == 0) {
            return "text-doux";
        }
        return pct > 0 ? "text-success" : "text-danger";
    }

    private static String majuscule(String s) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase(FR) + s.substring(1);
    }
}
