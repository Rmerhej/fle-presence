package fr.asso.fle.repository;

import fr.asso.fle.model.UniteLocale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UniteLocaleRepository extends JpaRepository<UniteLocale, Long> {

    Optional<UniteLocale> findBySlugAndActiveTrue(String slug);

    List<UniteLocale> findByActiveTrueOrderByNomAsc();

    List<UniteLocale> findAllByOrderByNomAsc();

    boolean existsBySlug(String slug);
}
