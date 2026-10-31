package fr.asso.fle.service;

import fr.asso.fle.model.*;
import fr.asso.fle.repository.GroupeRepository;
import fr.asso.fle.repository.UniteLocaleRepository;
import fr.asso.fle.repository.UtilisateurRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/** Gestion des unités locales, des groupes et des comptes. */
@Service
public class AdministrationService {

    private static final int MDP_MIN = 8;

    private final UniteLocaleRepository uniteRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final GroupeRepository groupeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JournalService journal;

    public AdministrationService(UniteLocaleRepository uniteRepository,
                                 UtilisateurRepository utilisateurRepository,
                                 GroupeRepository groupeRepository,
                                 PasswordEncoder passwordEncoder,
                                 JournalService journal) {
        this.uniteRepository = uniteRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.groupeRepository = groupeRepository;
        this.passwordEncoder = passwordEncoder;
        this.journal = journal;
    }

    // ------------------------------------------------------------------ unités

    @Transactional(readOnly = true)
    public List<UniteLocale> toutesLesUnites() {
        return uniteRepository.findAllByOrderByNomAsc();
    }

    @Transactional(readOnly = true)
    public List<UniteLocale> unitesActives() {
        return uniteRepository.findByActiveTrueOrderByNomAsc();
    }

    @Transactional
    public UniteLocale creerUnite(String nom, String adresse) {
        if (nom == null || nom.isBlank()) {
            throw new RegleMetierException("Le nom de l'unité est obligatoire.");
        }
        String slug = slug(nom);
        if (slug.isEmpty()) {
            throw new RegleMetierException("Nom d'unité invalide.");
        }
        if (uniteRepository.existsBySlug(slug)) {
            throw new RegleMetierException("Une unité porte déjà ce nom.");
        }
        UniteLocale u = uniteRepository.save(new UniteLocale(nom.strip(), slug, SeanceService.nettoyer(adresse)));
        journal.noter(u, TypeAction.UNITE_CREEE, u.getNom());
        return u;
    }

    @Transactional
    public void basculerUnite(Long id) {
        UniteLocale u = uniteRepository.findById(id)
                .orElseThrow(() -> new RegleMetierException("Unité introuvable."));
        u.setActive(!u.isActive());
        journal.noter(u, TypeAction.UNITE_MODIFIEE, u.getNom() + (u.isActive() ? " réactivée" : " désactivée"));
    }

    // ------------------------------------------------------------------ groupes

    @Transactional(readOnly = true)
    public List<Groupe> groupes(UniteLocale unite) {
        return groupeRepository.findByUniteAndActifTrueOrderByNomAsc(unite);
    }

    @Transactional
    public Groupe creerGroupe(UniteLocale unite, String nom, String niveau) {
        if (nom == null || nom.isBlank()) {
            throw new RegleMetierException("Le nom du groupe est obligatoire.");
        }
        Groupe g = groupeRepository.save(new Groupe(nom.strip(), SeanceService.nettoyer(niveau), unite));
        journal.noter(unite, TypeAction.GROUPE_CREE, g.getNom());
        return g;
    }

    @Transactional
    public void desactiverGroupe(UniteLocale unite, Long groupeId) {
        Groupe g = groupeRepository.findById(groupeId)
                .orElseThrow(() -> new RegleMetierException("Groupe introuvable."));
        if (!g.getUnite().getId().equals(unite.getId())) {
            throw new RegleMetierException("Ce groupe n'appartient pas à votre unité.");
        }
        g.setActif(false);
        journal.noter(unite, TypeAction.GROUPE_RETIRE, g.getNom());
    }

    // ------------------------------------------------------------------ comptes

    @Transactional(readOnly = true)
    public List<Utilisateur> tousLesUtilisateurs() {
        return utilisateurRepository.findAllByOrderByRoleAscUsernameAsc();
    }

    @Transactional(readOnly = true)
    public List<Utilisateur> benevolesDe(UniteLocale unite) {
        return utilisateurRepository.findAllByOrderByRoleAscUsernameAsc().stream()
                .filter(u -> u.getRole() == Role.BENEVOLE && u.getUnite() != null
                        && u.getUnite().getId().equals(unite.getId()))
                .toList();
    }

    @Transactional
    public Utilisateur creerUtilisateur(String username, String nomAffiche, String motDePasse,
                                        Role role, Long uniteId) {
        if (role == null) {
            throw new RegleMetierException("Choisissez un rôle.");
        }
        String identifiant = username == null ? "" : username.strip().toLowerCase(Locale.ROOT);
        if (!identifiant.matches("[a-z0-9._-]{3,50}")) {
            throw new RegleMetierException(
                    "Identifiant : 3 à 50 caractères (lettres minuscules, chiffres, point, tiret).");
        }
        if (utilisateurRepository.existsByUsername(identifiant)) {
            throw new RegleMetierException("Cet identifiant est déjà utilisé.");
        }
        if (nomAffiche == null || nomAffiche.isBlank()) {
            throw new RegleMetierException("Le nom affiché est obligatoire.");
        }
        verifierMotDePasse(motDePasse);

        UniteLocale unite = null;
        if (role != Role.ADMIN) {
            unite = uniteRepository.findById(uniteId == null ? -1L : uniteId)
                    .orElseThrow(() -> new RegleMetierException("Choisissez l'unité locale de ce compte."));
        }
        if (role == Role.RESPONSABLE && utilisateurRepository.existsByUniteAndRoleAndActifTrue(unite, Role.RESPONSABLE)) {
            throw new RegleMetierException("Cette unité a déjà un responsable actif. Désactivez-le d'abord.");
        }
        Utilisateur cree = utilisateurRepository.save(new Utilisateur(identifiant,
                passwordEncoder.encode(motDePasse), nomAffiche.strip(), role, unite));
        journal.noter(unite, TypeAction.COMPTE_CREE, cree.getUsername() + " (" + role.getLibelle() + ")");
        return cree;
    }

    /** Création d'un compte bénévole par le responsable, dans sa propre unité. */
    @Transactional
    public Utilisateur creerBenevole(UniteLocale unite, String username, String nomAffiche, String motDePasse) {
        return creerUtilisateur(username, nomAffiche, motDePasse, Role.BENEVOLE, unite.getId());
    }

    @Transactional
    public void basculerUtilisateur(Long id, String usernameCourant) {
        Utilisateur u = compte(id);
        if (u.getUsername().equals(usernameCourant)) {
            throw new RegleMetierException("Vous ne pouvez pas désactiver votre propre compte.");
        }
        if (!u.isActif() && u.getRole() == Role.RESPONSABLE
                && utilisateurRepository.existsByUniteAndRoleAndActifTrue(u.getUnite(), Role.RESPONSABLE)) {
            throw new RegleMetierException("Cette unité a déjà un responsable actif.");
        }
        u.setActif(!u.isActif());
        journal.noter(u.getUnite(), TypeAction.COMPTE_MODIFIE, u.getUsername() + (u.isActif() ? " réactivé" : " désactivé"));
    }

    @Transactional
    public void basculerBenevole(UniteLocale unite, Long id) {
        Utilisateur u = compte(id);
        if (u.getRole() != Role.BENEVOLE || u.getUnite() == null || !u.getUnite().getId().equals(unite.getId())) {
            throw new RegleMetierException("Ce bénévole n'appartient pas à votre unité.");
        }
        u.setActif(!u.isActif());
        journal.noter(unite, TypeAction.COMPTE_MODIFIE, u.getUsername() + (u.isActif() ? " réactivé" : " désactivé"));
    }

    @Transactional
    public void changerMotDePasse(Long id, String motDePasse) {
        verifierMotDePasse(motDePasse);
        Utilisateur u = compte(id);
        u.setPassword(passwordEncoder.encode(motDePasse));
        journal.noter(u.getUnite(), TypeAction.COMPTE_MODIFIE, u.getUsername() + " : mot de passe changé");
    }

    private Utilisateur compte(Long id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new RegleMetierException("Compte introuvable."));
    }

    private static void verifierMotDePasse(String motDePasse) {
        if (motDePasse == null || motDePasse.length() < MDP_MIN) {
            throw new RegleMetierException("Le mot de passe doit contenir au moins " + MDP_MIN + " caractères.");
        }
    }

    /** « Le Vésinet » → « le-vesinet » */
    static String slug(String nom) {
        String sansAccents = Normalizer.normalize(nom, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sansAccents.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
