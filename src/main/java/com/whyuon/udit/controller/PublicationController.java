package com.whyuon.udit.controller;

import com.whyuon.udit.dto.ChannelSummaryResponse;
import com.whyuon.udit.dto.PublicationResponse;
import com.whyuon.udit.service.PublicationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api")
public class PublicationController {

    private final PublicationService publicationService;

    public PublicationController(PublicationService publicationService) {
        this.publicationService = publicationService;
    }

    @GetMapping("/publications")
    public List<PublicationResponse> getPublications() {
        return publicationService.getPublications();
    }

    @GetMapping("/channels")
    public List<ChannelSummaryResponse> getChannels() {
        return publicationService.getChannelSummaries();
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
