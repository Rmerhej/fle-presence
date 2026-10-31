package fr.asso.fle.web;

import fr.asso.fle.config.Liens;
import fr.asso.fle.model.Utilisateur;
import fr.asso.fle.security.UtilisateurCourant;
import fr.asso.fle.service.RegleMetierException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Données disponibles dans toutes les pages + gestion des erreurs fonctionnelles. */
@ControllerAdvice
public class ModeleGlobal {

    private final UtilisateurCourant utilisateurCourant;
    private final Liens liens;

    public ModeleGlobal(UtilisateurCourant utilisateurCourant, Liens liens) {
        this.utilisateurCourant = utilisateurCourant;
        this.liens = liens;
    }

    /** Liens du pied de page (site de l'association, réseaux sociaux...). */
    @ModelAttribute("liens")
    public Liens liens() {
        return liens;
    }

    /** La personne connectée (null pour un visiteur). */
    @ModelAttribute("moi")
    public Utilisateur moi() {
        return utilisateurCourant.optionnel().orElse(null);
    }

    /**
     * Une règle métier non respectée : retour à la page précédente
     * (ou à l'accueil) avec le message d'erreur affiché.
     */
    @ExceptionHandler(RegleMetierException.class)
    public String regleMetier(RegleMetierException e, HttpServletRequest request, RedirectAttributes ra) {
        ra.addFlashAttribute("erreur", e.getMessage());
        String referer = request.getHeader("Referer");
        String courante = request.getRequestURL().toString();
        if (referer != null && referer.contains(request.getServerName()) && !referer.startsWith(courante)) {
            return "redirect:" + referer;
        }
        String uri = request.getRequestURI().substring(request.getContextPath().length());
        for (String espace : new String[]{"/saisie", "/responsable", "/admin"}) {
            if (uri.startsWith(espace + "/")) {
                return "redirect:" + espace;
            }
        }
        return "redirect:/";
    }
}
