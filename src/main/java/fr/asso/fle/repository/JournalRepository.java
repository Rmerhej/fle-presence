package fr.asso.fle.repository;

import fr.asso.fle.model.JournalEntree;
import fr.asso.fle.model.UniteLocale;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JournalRepository extends JpaRepository<JournalEntree, Long> {

    List<JournalEntree> findByUniteOrderByDateActionDescIdDesc(UniteLocale unite, Pageable page);

    List<JournalEntree> findAllByOrderByDateActionDescIdDesc(Pageable page);
}
