package fr.asso.fle.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Fabrique un petit fichier Excel (.xlsx) d'une seule feuille, sans bibliothèque externe :
 * un titre, une ligne d'en-têtes en gras (figée, avec filtres), les lignes de données
 * et une ligne de total. Le fichier s'ouvre dans Excel, LibreOffice, Google Sheets et sur téléphone.
 */
public final class ClasseurExcel {

    /** Format d'une colonne. */
    public enum Format { TEXTE, ENTIER, DECIMAL }

    /** Colonne : titre, format et largeur (en caractères). */
    public record Colonne(String titre, Format format, int largeur) {
    }

    // Styles définis dans styles.xml (index des cellXfs)
    private static final int S_TITRE = 1, S_SOUS_TITRE = 2, S_ENTETE = 3,
            S_TEXTE = 0, S_ENTIER = 4, S_DECIMAL = 5,
            S_TOTAL_TEXTE = 6, S_TOTAL_ENTIER = 7, S_TOTAL_DECIMAL = 8;

    private static final int LIGNE_ENTETE = 4; // 1 : titre, 2 : sous-titre, 3 : vide, 4 : en-têtes

    private ClasseurExcel() {
    }

    /**
     * @param nomFeuille nom de l'onglet
     * @param titre      titre affiché en haut de la feuille
     * @param sousTitre  ligne d'information sous le titre (périmètre, date…)
     * @param colonnes   colonnes du tableau
     * @param lignes     valeurs : String pour le texte, Number pour les nombres
     * @param total      ligne de total (ou null)
     */
    public static byte[] creer(String nomFeuille, String titre, String sousTitre, List<Colonne> colonnes,
                               List<List<Object>> lignes, List<Object> total) {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(sortie, StandardCharsets.UTF_8)) {
            ajouter(zip, "[Content_Types].xml", TYPES);
            ajouter(zip, "_rels/.rels", RELATIONS);
            ajouter(zip, "xl/workbook.xml", classeur(nomFeuille));
            ajouter(zip, "xl/_rels/workbook.xml.rels", RELATIONS_CLASSEUR);
            ajouter(zip, "xl/styles.xml", STYLES);
            ajouter(zip, "xl/worksheets/sheet1.xml", feuille(titre, sousTitre, colonnes, lignes, total));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return sortie.toByteArray();
    }

    // ================================================================ feuille

    private static String feuille(String titre, String sousTitre, List<Colonne> colonnes,
                                  List<List<Object>> lignes, List<Object> total) {
        StringBuilder x = new StringBuilder(4096);
        x.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
         .append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ")
         .append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">");

        // En-têtes figés : les titres de colonnes restent visibles quand on fait défiler
        x.append("<sheetViews><sheetView workbookViewId=\"0\">")
         .append("<pane ySplit=\"").append(LIGNE_ENTETE).append("\" topLeftCell=\"A").append(LIGNE_ENTETE + 1)
         .append("\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>");

        x.append("<cols>");
        for (int i = 0; i < colonnes.size(); i++) {
            x.append("<col min=\"").append(i + 1).append("\" max=\"").append(i + 1)
             .append("\" width=\"").append(colonnes.get(i).largeur()).append("\" customWidth=\"1\"/>");
        }
        x.append("</cols><sheetData>");

        x.append("<row r=\"1\" ht=\"24\" customHeight=\"1\">");
        texte(x, "A1", titre, S_TITRE);
        x.append("</row><row r=\"2\">");
        texte(x, "A2", sousTitre, S_SOUS_TITRE);
        x.append("</row>");

        int r = LIGNE_ENTETE;
        x.append("<row r=\"").append(r).append("\">");
        for (int i = 0; i < colonnes.size(); i++) {
            texte(x, ref(i, r), colonnes.get(i).titre(), S_ENTETE);
        }
        x.append("</row>");

        for (List<Object> ligne : lignes) {
            r++;
            ligne(x, r, colonnes, ligne, false);
        }
        int derniere = r;
        if (total != null) {
            r++;
            ligne(x, r, colonnes, total, true);
        }
        x.append("</sheetData>");

        // Filtres sur les en-têtes (tri et filtrage en un clic)
        if (!lignes.isEmpty()) {
            x.append("<autoFilter ref=\"").append(ref(0, LIGNE_ENTETE)).append(':')
             .append(ref(colonnes.size() - 1, derniere)).append("\"/>");
        }
        x.append("<pageMargins left=\"0.5\" right=\"0.5\" top=\"0.6\" bottom=\"0.6\" header=\"0.3\" footer=\"0.3\"/>")
         .append("<pageSetup orientation=\"landscape\" paperSize=\"9\"/>")
         .append("</worksheet>");
        return x.toString();
    }

    private static void ligne(StringBuilder x, int r, List<Colonne> colonnes, List<Object> valeurs, boolean total) {
        x.append("<row r=\"").append(r).append("\">");
        for (int i = 0; i < colonnes.size() && i < valeurs.size(); i++) {
            Object v = valeurs.get(i);
            String cellule = ref(i, r);
            if (v instanceof Number n) {
                boolean decimal = colonnes.get(i).format() == Format.DECIMAL;
                int style = decimal ? (total ? S_TOTAL_DECIMAL : S_DECIMAL) : (total ? S_TOTAL_ENTIER : S_ENTIER);
                String valeur = decimal
                        ? String.format(Locale.ROOT, "%.1f", n.doubleValue())
                        : String.valueOf(n.longValue());
                x.append("<c r=\"").append(cellule).append("\" s=\"").append(style).append("\"><v>")
                 .append(valeur).append("</v></c>");
            } else if (v != null) {
                texte(x, cellule, v.toString(), total ? S_TOTAL_TEXTE : S_TEXTE);
            } else if (total) {
                x.append("<c r=\"").append(cellule).append("\" s=\"").append(S_TOTAL_TEXTE).append("\"/>");
            }
        }
        x.append("</row>");
    }

    private static void texte(StringBuilder x, String cellule, String valeur, int style) {
        x.append("<c r=\"").append(cellule).append("\" t=\"inlineStr\" s=\"").append(style)
         .append("\"><is><t xml:space=\"preserve\">").append(echapper(valeur)).append("</t></is></c>");
    }

    /** Référence de cellule : colonne 0 → A, 1 → B… (au plus 26 colonnes ici). */
    private static String ref(int colonne, int ligne) {
        return (char) ('A' + colonne) + String.valueOf(ligne);
    }

    private static String echapper(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '&' -> b.append("&amp;");
                case '<' -> b.append("&lt;");
                case '>' -> b.append("&gt;");
                case '"' -> b.append("&quot;");
                default -> {
                    if (c >= 0x20 || c == '\t' || c == '\n' || c == '\r') {
                        b.append(c);
                    }
                }
            }
        }
        return b.toString();
    }

    /** Nom d'onglet valide pour Excel : 31 caractères au plus, sans caractères interdits. */
    private static String nomOnglet(String nom) {
        String n = (nom == null || nom.isBlank()) ? "Feuille 1" : nom.replaceAll("[\\[\\]:*?/\\\\']", " ").trim();
        return n.length() > 31 ? n.substring(0, 31) : n;
    }

    private static void ajouter(ZipOutputStream zip, String chemin, String contenu) throws IOException {
        zip.putNextEntry(new ZipEntry(chemin));
        zip.write(contenu.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    // ================================================================ parties fixes du fichier

    private static String classeur(String nomFeuille) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                + "<sheets><sheet name=\"" + echapper(nomOnglet(nomFeuille)) + "\" sheetId=\"1\" r:id=\"rId1\"/></sheets>"
                + "</workbook>";
    }

    private static final String TYPES = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
            + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
            + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
            + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
            + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
            + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
            + "</Types>";

    private static final String RELATIONS = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
            + "</Relationships>";

    private static final String RELATIONS_CLASSEUR = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
            + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>"
            + "</Relationships>";

    /**
     * Styles : 0 texte, 1 titre, 2 sous-titre (gris), 3 en-tête (gras, fond bleu clair, bordure),
     * 4 entier, 5 décimal (0,0), 6-8 ligne de total (gras, bordure en haut).
     * Format 3 = « # ##0 » ; format 164 = « 0,0 » (le séparateur suit la langue d'Excel).
     */
    private static final String STYLES = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
            + "<numFmts count=\"1\"><numFmt numFmtId=\"164\" formatCode=\"#,##0.0\"/></numFmts>"
            + "<fonts count=\"4\">"
            + "<font><sz val=\"11\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
            + "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
            + "<font><b/><sz val=\"14\"/><color rgb=\"FF2B4BB8\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
            + "<font><i/><sz val=\"10\"/><color rgb=\"FF6C757D\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
            + "</fonts>"
            + "<fills count=\"3\">"
            + "<fill><patternFill patternType=\"none\"/></fill>"
            + "<fill><patternFill patternType=\"gray125\"/></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFDCE5FB\"/><bgColor indexed=\"64\"/></patternFill></fill>"
            + "</fills>"
            + "<borders count=\"3\">"
            + "<border><left/><right/><top/><bottom/><diagonal/></border>"
            + "<border><left/><right/><top/><bottom style=\"thin\"><color rgb=\"FF8EA2E0\"/></bottom><diagonal/></border>"
            + "<border><left/><right/><top style=\"medium\"><color rgb=\"FF2B4BB8\"/></top><bottom/><diagonal/></border>"
            + "</borders>"
            + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
            + "<cellXfs count=\"9\">"
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
            + "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>"
            + "<xf numFmtId=\"0\" fontId=\"3\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>"
            + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment wrapText=\"1\" vertical=\"center\"/></xf>"
            + "<xf numFmtId=\"3\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>"
            + "<xf numFmtId=\"164\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>"
            + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"2\" xfId=\"0\" applyFont=\"1\" applyBorder=\"1\"/>"
            + "<xf numFmtId=\"3\" fontId=\"1\" fillId=\"0\" borderId=\"2\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\" applyBorder=\"1\"/>"
            + "<xf numFmtId=\"164\" fontId=\"1\" fillId=\"0\" borderId=\"2\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\" applyBorder=\"1\"/>"
            + "</cellXfs>"
            + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
            + "</styleSheet>";
}
