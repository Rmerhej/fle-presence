package fr.asso.fle.service;

import fr.asso.fle.model.*;
import fr.asso.fle.repository.PresenceRepository;
import fr.asso.fle.repository.SeanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Saisie des présences.
 * Règles :
 * - un bénévole saisit une seule fois une séance de son unité (aujourd'hui ou passée) ;
 * - seul le responsable de l'unité peut modifier une saisie existante.
 */
@Service
public class PresenceService {

    /** Un bénévole peut saisir une séance jusqu'à N jours après. */
    public static final int DELAI_SAISIE_BENEVOLE_JOURS = 30;
    private static final int MAX_PERSONNES = 500;

    private final PresenceRepository presenceRepository;
    private final SeanceRepository seanceRepository;
    private final JournalService journal;

    public PresenceService(PresenceRepository presenceRepository, SeanceRepository seanceRepository,
                           JournalService journal) {
        this.presenceRepository = presenceRepository;
        this.seanceRepository = seanceRepository;
        this.journal = journal;
    }

    @Transactional(readOnly = true)
    public Optional<Presence> pourSeance(Seance seance) {
        return presenceRepository.findBySeance(seance);
    }

    /** Présences indexées par identifiant de séance. */
    @Transactional(readOnly = true)
    public Map<Long, Presence> parSeance(List<Seance> seances) {
        if (seances.isEmpty()) {
            return Map.of();
        }
        return presenceRepository.findBySeanceIn(seances).stream()
                .collect(Collectors.toMap(p -> p.getSeance().getId(), Function.identity()));
    }

    @Transactional(readOnly = true)
    public List<Presence> parUniteEntre(UniteLocale unite, LocalDate du, LocalDate au) {
        return presenceRepository.findParUniteEntre(unite, du, au);
    }

    /** Vérifie que l'utilisateur peut ouvrir le formulaire de saisie et renvoie la séance. */
    @Transactional(readOnly = true)
    public Seance seancePourSaisie(Long seanceId, Utilisateur utilisateur) {
        Seance seance = seanceRepository.findById(seanceId)
                .orElseThrow(() -> new RegleMetierException("Séance introuvable."));
        verifierDroits(seance, utilisateur);
        return seance;
    }

    @Transactional
    public Presence enregistrer(Long seanceId, Integer nbBenevoles, Integer nbApprenants,
                                String commentaire, Utilisateur utilisateur) {
        Seance seance = seanceRepository.findById(seanceId)
                .orElseThrow(() -> new RegleMetierException("Séance introuvable."));
        verifierDroits(seance, utilisateur);
        verifierNombre(nbBenevoles, "bénévoles");
        verifierNombre(nbApprenants, "apprenants");
        if (nbBenevoles == 0 && nbApprenants > 0) {
            throw new RegleMetierException("Il faut au moins un bénévole présent s'il y a des apprenants.");
        }

        Optional<Presence> existante = presenceRepository.findBySeance(seance);
        Presence p;
        String avant = null;
        if (existante.isPresent()) {
            p = existante.get();
            avant = p.getNbBenevoles() + " bénévole(s), " + p.getNbApprenants() + " apprenant(s)";
            p.setModifiePar(utilisateur.getUsername());
            p.setModifieLe(LocalDateTime.now());
        } else {
            p = new Presence();
            p.setSeance(seance);
            p.setSaisiPar(utilisateur.getUsername());
            p.setSaisiLe(LocalDateTime.now());
        }
        p.setNbBenevoles(nbBenevoles);
        p.setNbApprenants(nbApprenants);
        p.setCommentaire(commentaire == null || commentaire.isBlank() ? null
                : commentaire.strip().substring(0, Math.min(500, commentaire.strip().length())));
        p = presenceRepository.save(p);
        String apres = nbBenevoles + " bénévole(s), " + nbApprenants + " apprenant(s)";
        if (avant == null) {
            journal.noter(seance.getGroupe().getUnite(), TypeAction.PRESENCE_SAISIE,
                    SeanceService.decrire(seance) + " · " + apres);
        } else {
            journal.noter(seance.getGroupe().getUnite(), TypeAction.PRESENCE_MODIFIEE,
                    SeanceService.decrire(seance) + " · " + avant + " → " + apres);
        }
        return p;
    }

    private void verifierDroits(Seance seance, Utilisateur utilisateur) {
        Role role = utilisateur.getRole();
        if (role != Role.BENEVOLE && role != Role.RESPONSABLE) {
            throw new RegleMetierException("Seuls les bénévoles et responsables saisissent les présences.");
        }
        if (utilisateur.getUnite() == null
                || !seance.getGroupe().getUnite().getId().equals(utilisateur.getUnite().getId())) {
            throw new RegleMetierException("Cette séance n'appartient pas à votre unité.");
        }
        if (seance.getStatut() != StatutSeance.PREVUE) {
            throw new RegleMetierException("Cette séance a été annulée ou déplacée : pas de saisie possible.");
        }
        LocalDate aujourdHui = LocalDate.now();
        if (seance.getDate().isAfter(aujourdHui)) {
            throw new RegleMetierException("La séance n'a pas encore eu lieu.");
        }
        if (role == Role.BENEVOLE) {
            if (presenceRepository.findBySeance(seance).isPresent()) {
                throw new RegleMetierException(
                        "Les présences de cette séance sont déjà saisies. Seul le responsable de l'unité peut les modifier.");
            }
            if (seance.getDate().isBefore(aujourdHui.minusDays(DELAI_SAISIE_BENEVOLE_JOURS))) {
                throw new RegleMetierException(
                        "Séance trop ancienne : demandez au responsable de l'unité d'enregistrer les présences.");
            }
        }
    }

    private static void verifierNombre(Integer n, String quoi) {
        if (n == null) {
            throw new RegleMetierException("Indiquez le nombre de " + quoi + ".");
        }
        if (n < 0 || n > MAX_PERSONNES) {
            throw new RegleMetierException("Le nombre de " + quoi + " doit être entre 0 et " + MAX_PERSONNES + ".");
        }
    }
}
