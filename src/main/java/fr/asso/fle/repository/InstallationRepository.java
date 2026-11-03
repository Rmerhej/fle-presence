package fr.asso.fle.repository;

import fr.asso.fle.model.Installation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface InstallationRepository extends JpaRepository<Installation, Long> {

    long countByInstalleeLeGreaterThanEqual(LocalDateTime depuis);

    @Query("select i.plateforme, count(i) from Installation i group by i.plateforme")
    List<Object[]> compterParPlateforme();
}
