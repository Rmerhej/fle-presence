package fr.asso.fle.service;

import fr.asso.fle.model.JournalEntree;
import fr.asso.fle.model.TypeAction;
import fr.asso.fle.model.UniteLocale;
import fr.asso.fle.repository.JournalRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Historique des modifications (qui a annulé, déplacé ou corrigé quoi, et quand). */
@Service
public class JournalService {

    private final JournalRepository repository;

    public JournalService(JournalRepository repository) {
        this.repository = repository;
    }

    /** Enregistre une action ; l'auteur est la personne connectée (ou « système »). */
    @Transactional
    public void noter(UniteLocale unite, TypeAction action, String details) {
        repository.save(new JournalEntree(auteurCourant(), unite, action, details));
    }

    @Transactional(readOnly = true)
    public List<JournalEntree> pourUnite(UniteLocale unite, int limite) {
        return repository.findByUniteOrderByDateActionDescIdDesc(unite, PageRequest.of(0, limite));
    }

    @Transactional(readOnly = true)
    public List<JournalEntree> tout(Long uniteId, int limite) {
        if (uniteId == null) {
            return repository.findAllByOrderByDateActionDescIdDesc(PageRequest.of(0, limite));
        }
        // Filtre simple en mémoire : l'historique d'une association reste de taille modeste.
        return repository.findAllByOrderByDateActionDescIdDesc(PageRequest.of(0, limite * 5)).stream()
                .filter(e -> e.getUnite() != null && e.getUnite().getId().equals(uniteId))
                .limit(limite)
                .toList();
    }

    private static String auteurCourant() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return "système";
        }
        return auth.getName();
    }
}
