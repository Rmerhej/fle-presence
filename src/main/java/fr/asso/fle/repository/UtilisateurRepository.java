package fr.asso.fle.repository;

import fr.asso.fle.model.Role;
import fr.asso.fle.model.UniteLocale;
import fr.asso.fle.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    Optional<Utilisateur> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByUniteAndRoleAndActifTrue(UniteLocale unite, Role role);

    List<Utilisateur> findAllByOrderByRoleAscUsernameAsc();
}
