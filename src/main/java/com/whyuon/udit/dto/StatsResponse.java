package com.whyuon.udit.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Resumen estadístico devuelto por GET /api/stats. Acepta los mismos
 * filtros que el endpoint del CSV, de forma que el dashboard del frontend
 * pinta lo mismo que se descargaría al pulsar "Descargar CSV".
 *
 * - totalPublications:   total de filas que pasan los filtros.
 * - byPlatform:          contador de publicaciones por plataforma.
 * - byChannel:           lista de canales con su cuenta, ordenada desc.
 * - earliestDate/latest: rango temporal de las publicaciones filtradas.
 */
public record StatsResponse(
        long totalPublications,
        Map<String, Long> byPlatform,
        List<ChannelCount> byChannel,
        LocalDateTime earliestDate,
        LocalDateTime latestDate
) {

    public record ChannelCount(String name, String platform, long count) {}
}
