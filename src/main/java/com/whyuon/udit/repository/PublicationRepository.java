package com.whyuon.udit.repository;

import com.whyuon.udit.model.Publication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PublicationRepository extends JpaRepository<Publication, Long> {

    boolean existsByUrl(String url);

    @EntityGraph(attributePaths = {"channel", "channel.platform", "author"})
    List<Publication> findAllByOrderByDatePublishedDesc();

    // Sobrescribimos el findAll paginado para cargar channel y author en el
    // mismo SELECT (evita LazyInitializationException al mapear a DTO).
    @Override
    @EntityGraph(attributePaths = {"channel", "channel.platform", "author"})
    Page<Publication> findAll(Pageable pageable);

    /**
     * Filtrado para el informe CSV. Todos los parámetros son opcionales:
     * si vienen null no se aplica esa condición. Devuelve publicaciones
     * ordenadas por fecha descendente, listas para volcar a CSV.
     *
     *   platform:  código exacto ('youtube', 'blog', ...). null = todas.
     *   channel:   coincidencia parcial sobre el nombre del canal,
     *              case-insensitive. null = todos.
     *   from / to: rango por fecha de publicación. Ambos opcionales.
     */
    @EntityGraph(attributePaths = {"channel", "channel.platform", "author"})
    @Query("""
        SELECT p FROM Publication p
        WHERE (:platform IS NULL OR LOWER(p.channel.platform.code) = LOWER(:platform))
          AND (:channel  IS NULL OR LOWER(p.channel.name)         LIKE LOWER(CONCAT('%', :channel, '%')))
          AND (:from     IS NULL OR p.datePublished >= :from)
          AND (:to       IS NULL OR p.datePublished <= :to)
        ORDER BY p.datePublished DESC
    """)
    List<Publication> findForReport(
            @Param("platform") String platform,
            @Param("channel") String channel,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
