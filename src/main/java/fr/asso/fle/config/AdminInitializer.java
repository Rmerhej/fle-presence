package fr.asso.fle.config;

import fr.asso.fle.model.Role;
import fr.asso.fle.model.Utilisateur;
import fr.asso.fle.repository.UtilisateurRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Au tout premier démarrage (base vide), crée le compte administrateur.
 * Mot de passe : variable d'environnement APP_ADMIN_PASSWORD, sinon un mot de passe
 * aléatoire est généré et affiché une seule fois dans les logs.
 */
@Component
@Order(1)
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UtilisateurRepository repository;
    private final PasswordEncoder encoder;
    private final String username;
    private final String password;

    public AdminInitializer(UtilisateurRepository repository, PasswordEncoder encoder,
                            @Value("${app.admin.username:admin}") String username,
                            @Value("${app.admin.password:}") String password) {
        this.repository = repository;
        this.encoder = encoder;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        String mdp = password;
        if (mdp == null || mdp.isBlank()) {
            byte[] octets = new byte[12];
            new SecureRandom().nextBytes(octets);
            mdp = Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
            log.warn("=== Compte administrateur créé : identifiant « {} », mot de passe « {} » "
                    + "— changez-le après la première connexion ===", username, mdp);
        } else {
            log.info("Compte administrateur « {} » créé", username);
        }
        repository.save(new Utilisateur(username, encoder.encode(mdp), "Administrateur", Role.ADMIN, null));
    }
}
