package fr.asso.fle.repository;

import fr.asso.fle.model.Creneau;
import fr.asso.fle.model.UniteLocale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CreneauRepository extends JpaRepository<Creneau, Long> {

    @Query("""
            select c from Creneau c
            where c.actif = true and c.groupe.actif = true and c.groupe.unite.active = true
            """)
    List<Creneau> findTousActifs();

    @Query("""
            select c from Creneau c
            where c.actif = true and c.groupe.actif = true and c.groupe.unite = :unite
            order by c.groupe.nom, c.jourSemaine, c.heureDebut
            """)
    List<Creneau> findActifsParUnite(@Param("unite") UniteLocale unite);
}
