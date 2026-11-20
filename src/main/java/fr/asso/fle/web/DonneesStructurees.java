package fr.asso.fle.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.asso.fle.config.Liens;
import fr.asso.fle.model.UniteLocale;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Données structurées (JSON-LD, vocabulaire schema.org) lues par les moteurs de recherche :
 * elles décrivent le site, l'association et les cours de chaque ville.
 * Le bloc est un script de données (type application/ld+json) : le navigateur ne l'exécute pas,
 * il reste donc compatible avec la politique de sécurité (CSP) du site.
 */
@Component
public class DonneesStructurees {

    private static final String DESCRIPTION_SITE =
            "Cours de français gratuits pour adultes, animés par des bénévoles : dates des cours, annulations et reports, sans inscription.";

    private final ObjectMapper json;
    private final Liens liens;

    public DonneesStructurees(ObjectMapper json, Liens liens) {
        this.json = json;
        this.liens = liens;
    }

    /** Page d'accueil : le site et l'association qui le publie. */
    public String accueil(String site) {
        List<Object> graphe = new ArrayList<>();
        graphe.add(siteWeb(site));
        Map<String, Object> asso = association(site);
        if (asso != null) {
            graphe.add(asso);
        }
        return ecrire(graphe);
    }

    /** Page d'une ville : fil d'Ariane + les cours de français proposés à cette adresse. */
    public String unite(String site, UniteLocale unite) {
        String url = site + "/u/" + unite.getSlug();

        Map<String, Object> filAriane = objet("BreadcrumbList");
        filAriane.put("itemListElement", List.of(
                etape(1, "Accueil", site + "/"),
                etape(2, unite.getNom(), url)));

        Map<String, Object> cours = objet("Course");
        cours.put("@id", url + "#cours");
        cours.put("name", "Cours de français à " + unite.getNom());
        cours.put("description", "Cours de français gratuits pour adultes à " + unite.getNom()
                + ", animés par des bénévoles. Sans inscription : les dates, annulations et reports sont sur cette page.");
        cours.put("url", url);
        cours.put("inLanguage", "fr");
        cours.put("isAccessibleForFree", true);
        Map<String, Object> asso = association(site);
        if (asso != null) {
            cours.put("provider", Map.of("@id", asso.get("@id")));
        }
        Map<String, Object> offre = objet("Offer");
        offre.put("category", "Free");
        offre.put("price", 0);
        offre.put("priceCurrency", "EUR");
        cours.put("offers", offre);

        Map<String, Object> session = objet("CourseInstance");
        session.put("courseMode", "Onsite");
        Map<String, Object> lieu = objet("Place");
        lieu.put("name", unite.getNom());
        Map<String, Object> adresse = objet("PostalAddress");
        if (unite.getAdresse() != null && !unite.getAdresse().isBlank()) {
            adresse.put("streetAddress", unite.getAdresse());
        }
        adresse.put("addressLocality", unite.getNom());
        adresse.put("addressCountry", "FR");
        lieu.put("address", adresse);
        session.put("location", lieu);
        cours.put("hasCourseInstance", session);

        List<Object> graphe = new ArrayList<>(List.of(filAriane, cours));
        if (asso != null) {
            graphe.add(asso);
        }
        return ecrire(graphe);
    }

    // ------------------------------------------------------------------ briques

    private Map<String, Object> siteWeb(String site) {
        Map<String, Object> s = objet("WebSite");
        s.put("@id", site + "/#site");
        s.put("url", site + "/");
        s.put("name", "FLE Présence");
        s.put("description", DESCRIPTION_SITE);
        s.put("inLanguage", "fr-FR");
        Map<String, Object> asso = association(site);
        if (asso != null) {
            s.put("publisher", Map.of("@id", asso.get("@id")));
        }
        return s;
    }

    /** L'association (nom, site, réseaux sociaux), ou null si elle n'est pas configurée. */
    private Map<String, Object> association(String site) {
        if (vide(liens.associationNom())) {
            return null;
        }
        Map<String, Object> o = objet("Organization");
        o.put("@id", site + "/#association");
        o.put("name", liens.associationNom());
        if (!vide(liens.association())) {
            o.put("url", liens.association());
        }
        List<String> reseaux = Stream.of(liens.facebook(), liens.instagram(), liens.linkedin(), liens.youtube(), liens.x())
                .filter(l -> !vide(l))
                .toList();
        if (!reseaux.isEmpty()) {
            o.put("sameAs", reseaux);
        }
        return o;
    }

    private static Map<String, Object> etape(int position, String nom, String url) {
        Map<String, Object> e = objet("ListItem");
        e.put("position", position);
        e.put("name", nom);
        e.put("item", url);
        return e;
    }

    private static Map<String, Object> objet(String type) {
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("@type", type);
        return o;
    }

    private static boolean vide(String s) {
        return s == null || s.isBlank();
    }

    private String ecrire(List<Object> graphe) {
        Map<String, Object> racine = new LinkedHashMap<>();
        racine.put("@context", "https://schema.org");
        racine.put("@graph", graphe);
        try {
            // « < » échappé : aucune donnée (nom, adresse…) ne peut fermer la balise <script>
            return json.writeValueAsString(racine).replace("<", "\\u003c");
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
