package com.whyuon.udit.repository;

import com.whyuon.udit.model.Publication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PublicationRepository extends JpaRepository<Publication, Long> {

    boolean existsByUrl(String url);

    @EntityGraph(attributePaths = {"channel", "author"})
    List<Publication> findAllByOrderByDatePublishedDesc();

    // Sobrescribimos el findAll paginado para cargar channel y author en el
    // mismo SELECT (evita LazyInitializationException al mapear a DTO).
    @Override
    @EntityGraph(attributePaths = {"channel", "author"})
    Page<Publication> findAll(Pageable pageable);
}
