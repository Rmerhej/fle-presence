package fr.asso.fle.security;

import fr.asso.fle.model.Utilisateur;
import fr.asso.fle.repository.UtilisateurRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Accès au compte de la personne connectée. */
@Component
public class UtilisateurCourant {

    private final UtilisateurRepository repository;

    public UtilisateurCourant(UtilisateurRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<Utilisateur> optionnel() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return repository.findByUsername(auth.getName());
    }

    public Utilisateur get() {
        return optionnel().orElseThrow(() -> new IllegalStateException("Aucun utilisateur connecté"));
    }
}
