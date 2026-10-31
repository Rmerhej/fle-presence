package fr.asso.fle.web;

import fr.asso.fle.model.Utilisateur;
import fr.asso.fle.security.UtilisateurCourant;
import fr.asso.fle.service.AdministrationService;
import fr.asso.fle.service.RegleMetierException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** « Mon compte » : changement de son propre mot de passe (tous les rôles). */
@Controller
public class CompteController {

    private final UtilisateurCourant utilisateurCourant;
    private final AdministrationService administrationService;
    private final PasswordEncoder passwordEncoder;

    public CompteController(UtilisateurCourant utilisateurCourant,
                            AdministrationService administrationService,
                            PasswordEncoder passwordEncoder) {
        this.utilisateurCourant = utilisateurCourant;
        this.administrationService = administrationService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/compte")
    public String compte() {
        return "compte";
    }

    @PostMapping("/compte")
    public String changerMotDePasse(@RequestParam(required = false) String actuel,
                                    @RequestParam(required = false) String nouveau,
                                    @RequestParam(required = false) String confirmation,
                                    RedirectAttributes ra) {
        Utilisateur moi = utilisateurCourant.get();
        try {
            if (actuel == null || !passwordEncoder.matches(actuel, moi.getPassword())) {
                throw new RegleMetierException("Le mot de passe actuel est incorrect.");
            }
            if (nouveau == null || !nouveau.equals(confirmation)) {
                throw new RegleMetierException("Les deux nouveaux mots de passe ne sont pas identiques.");
            }
            administrationService.changerMotDePasse(moi.getId(), nouveau);
            ra.addFlashAttribute("succes", "Votre mot de passe a été modifié.");
        } catch (RegleMetierException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/compte";
    }
}
