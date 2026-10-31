package fr.asso.fle.repository;

import fr.asso.fle.model.Groupe;
import fr.asso.fle.model.Seance;
import fr.asso.fle.model.StatutSeance;
import fr.asso.fle.model.UniteLocale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface SeanceRepository extends JpaRepository<Seance, Long> {

    boolean existsByGroupeAndDateAndHeureDebut(Groupe groupe, LocalDate date, LocalTime heureDebut);

    @Query("""
            select s from Seance s
            where s.groupe.unite = :unite and s.groupe.actif = true
              and s.date between :du and :au
            order by s.date, s.heureDebut, s.groupe.nom
            """)
    List<Seance> findParUniteEntre(@Param("unite") UniteLocale unite,
                                   @Param("du") LocalDate du,
                                   @Param("au") LocalDate au);

    @Query("""
            select s from Seance s
            where s.groupe.unite = :unite and s.groupe.actif = true
              and s.statut = :statut and s.date between :du and :au
            order by s.date desc, s.heureDebut
            """)
    List<Seance> findParUniteStatutEntre(@Param("unite") UniteLocale unite,
                                         @Param("statut") StatutSeance statut,
                                         @Param("du") LocalDate du,
                                         @Param("au") LocalDate au);

    /** Séances passées maintenues pour lesquelles aucune présence n'a été saisie. */
    @Query("""
            select s from Seance s
            where s.groupe.unite = :unite and s.statut = fr.asso.fle.model.StatutSeance.PREVUE
              and s.date between :du and :au
              and not exists (select p from Presence p where p.seance = s)
            order by s.date desc
            """)
    List<Seance> findNonSaisies(@Param("unite") UniteLocale unite,
                                @Param("du") LocalDate du,
                                @Param("au") LocalDate au);
}
