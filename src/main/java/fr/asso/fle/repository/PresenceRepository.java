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

    /**
     * Totaux par date et par unité.
     * Colonnes : date, uniteId, uniteNom, nbSeances, totalBenevoles, totalApprenants
     */
    @Query("""
            select s.date, u.id, u.nom, count(p), sum(p.nbBenevoles), sum(p.nbApprenants)
            from Presence p join p.seance s join s.groupe g join g.unite u
            where s.date between :du and :au and (:uniteId is null or u.id = :uniteId)
            group by s.date, u.id, u.nom
            order by s.date desc, u.nom
            """)
    List<Object[]> totauxParJour(@Param("du") LocalDate du,
                                 @Param("au") LocalDate au,
                                 @Param("uniteId") Long uniteId);

    /**
     * Totaux par mois et par unité pour une année.
     * Colonnes : mois (1-12), uniteId, uniteNom, nbSeances, totalBenevoles, totalApprenants
     */
    @Query("""
            select extract(month from s.date), u.id, u.nom, count(p), sum(p.nbBenevoles), sum(p.nbApprenants)
            from Presence p join p.seance s join s.groupe g join g.unite u
            where extract(year from s.date) = :annee and (:uniteId is null or u.id = :uniteId)
            group by extract(month from s.date), u.id, u.nom
            order by extract(month from s.date), u.nom
            """)
    List<Object[]> totauxParMois(@Param("annee") int annee, @Param("uniteId") Long uniteId);

    /**
     * Totaux par année et par unité.
     * Colonnes : annee, uniteId, uniteNom, nbSeances, totalBenevoles, totalApprenants
     */
    @Query("""
            select extract(year from s.date), u.id, u.nom, count(p), sum(p.nbBenevoles), sum(p.nbApprenants)
            from Presence p join p.seance s join s.groupe g join g.unite u
            where (:uniteId is null or u.id = :uniteId)
            group by extract(year from s.date), u.id, u.nom
            order by extract(year from s.date) desc, u.nom
            """)
    List<Object[]> totauxParAnnee(@Param("uniteId") Long uniteId);
}
