package fr.asso.fle.repository;

import fr.asso.fle.model.Presence;
import fr.asso.fle.model.Seance;
import fr.asso.fle.model.UniteLocale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PresenceRepository extends JpaRepository<Presence, Long> {

    Optional<Presence> findBySeance(Seance seance);

    List<Presence> findBySeanceIn(Collection<Seance> seances);

    @Query("""
            select p from Presence p
            where p.seance.groupe.unite = :unite and p.seance.date between :du and :au
            order by p.seance.date desc, p.seance.heureDebut
            """)
    List<Presence> findParUniteEntre(@Param("unite") UniteLocale unite,
                                     @Param("du") LocalDate du,
                                     @Param("au") LocalDate au);

    /** Présences d'une période (toutes unités si uniteId est null), pour les statistiques. */
    @Query("""
            select p from Presence p
            join fetch p.seance s join fetch s.groupe g join fetch g.unite u
            where s.date between :du and :au and (:uniteId is null or u.id = :uniteId)
            """)
    List<Presence> findPourStats(@Param("du") LocalDate du,
                                 @Param("au") LocalDate au,
                                 @Param("uniteId") Long uniteId);
}
