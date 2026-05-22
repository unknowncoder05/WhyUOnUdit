package com.whyuon.udit.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Respuesta REST de una publicación. Los campos específicos de cada
 * plataforma (id de vídeo, formato, duración, likes, etc.) van todos
 * dentro de `extraData` con sus claves originales del JSON fuente.
 */
public record PublicationResponse(
        Long id,
        String title,
        String channelName,
        String platform,
        String authorName,
        String authorImageUrl,
        String url,
        String imageUrl,
        String description,
        Map<String, Object> extraData,
        LocalDateTime datePublished
) {
}
