package com.whyuon.udit.controller;

import com.whyuon.udit.dto.ChannelSummaryResponse;
import com.whyuon.udit.dto.PaginatedPublicationsResponse;
import com.whyuon.udit.dto.StatsResponse;
import com.whyuon.udit.model.PublicationImage;
import com.whyuon.udit.repository.PublicationImageRepository;
import com.whyuon.udit.service.PublicationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class PublicationController {

    private final PublicationService publicationService;
    private final PublicationImageRepository publicationImageRepository;

    public PublicationController(PublicationService publicationService,
                                 PublicationImageRepository publicationImageRepository) {
        this.publicationService = publicationService;
        this.publicationImageRepository = publicationImageRepository;
    }

    @GetMapping("/publications")
    public PaginatedPublicationsResponse getPublications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return publicationService.getPublicationsPage(page, size);
    }

    @GetMapping("/channels")
    public List<ChannelSummaryResponse> getChannels() {
        return publicationService.getChannelSummaries();
    }

    /**
     * Devuelve los bytes de la imagen archivada de una publicacion. Si no
     * hay imagen guardada, responde 404.
     */
    @GetMapping("/publications/{id}/image")
    public ResponseEntity<byte[]> getPublicationImage(@PathVariable Long id) {
        PublicationImage image = publicationImageRepository.findByPublicationId(id).orElse(null);
        if (image == null) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType = image.getMimeType() != null
                ? MediaType.parseMediaType(image.getMimeType())
                : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .body(image.getImageBytes());
    }

    /**
     * Estadísticas del dashboard, con los mismos filtros que el CSV.
     * El frontend las usa para refrescar las tarjetas y la barra de
     * distribución por canal cuando el usuario cambia los filtros.
     */
    @GetMapping("/stats")
    public StatsResponse getStats(
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return publicationService.getStats(platform, channel, from, to);
    }

    /**
     * Descarga el informe CSV. Todos los filtros son opcionales:
     *   ?platform=youtube              -> solo vídeos de YouTube
     *   ?channel=Stryd                 -> coincidencia parcial en el nombre del canal
     *   ?from=2026-01-01&to=2026-04-30 -> rango por fecha de publicación
     * Sin parámetros, equivale al comportamiento anterior (todas las publicaciones).
     */
    @GetMapping("/reports/publications.csv")
    public ResponseEntity<byte[]> downloadReport(
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        byte[] body = publicationService.buildCsvReport(platform, channel, from, to)
                .getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=publications.csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }
}
