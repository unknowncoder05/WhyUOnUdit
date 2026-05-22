package com.whyuon.udit.controller;

import com.whyuon.udit.dto.ChannelSummaryResponse;
import com.whyuon.udit.dto.PaginatedPublicationsResponse;
import com.whyuon.udit.model.PublicationImage;
import com.whyuon.udit.repository.PublicationImageRepository;
import com.whyuon.udit.service.PublicationService;
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

    @GetMapping("/reports/publications.csv")
    public ResponseEntity<byte[]> downloadReport() {
        byte[] body = publicationService.buildCsvReport().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=publications.csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }
}
