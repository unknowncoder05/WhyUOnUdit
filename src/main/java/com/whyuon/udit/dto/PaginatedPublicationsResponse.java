package com.whyuon.udit.dto;

import java.util.List;

/**
 * Respuesta paginada del listado de publicaciones.
 *
 * - content:     publicaciones de la página actual
 * - currentPage: índice de la página actual (0-based)
 * - totalPages:  número total de páginas
 * - totalItems:  número total de publicaciones en BD
 * - pageSize:    tamaño de página solicitado
 */
public record PaginatedPublicationsResponse(
        List<PublicationResponse> content,
        int currentPage,
        int totalPages,
        long totalItems,
        int pageSize
) {
}
