package fr.asso.fle.repository;

import fr.asso.fle.model.PushAbonnement;
import fr.asso.fle.model.UniteLocale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PushAbonnementRepository extends JpaRepository<PushAbonnement, Long> {

    Optional<PushAbonnement> findByEndpointHash(String endpointHash);

    List<PushAbonnement> findByUnite(UniteLocale unite);

    long countByUnite(UniteLocale unite);
}
