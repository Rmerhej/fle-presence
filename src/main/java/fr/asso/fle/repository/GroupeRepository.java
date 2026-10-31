package fr.asso.fle.repository;

import fr.asso.fle.model.Groupe;
import fr.asso.fle.model.UniteLocale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupeRepository extends JpaRepository<Groupe, Long> {

    List<Groupe> findByUniteAndActifTrueOrderByNomAsc(UniteLocale unite);
}
