package com.whyuon.udit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whyuon.udit.dto.ImportResultResponse;
import com.whyuon.udit.model.Author;
import com.whyuon.udit.model.Channel;
import com.whyuon.udit.model.ImportRun;
import com.whyuon.udit.model.Platform;
import com.whyuon.udit.model.Publication;
import com.whyuon.udit.repository.AuthorRepository;
import com.whyuon.udit.repository.ChannelRepository;
import com.whyuon.udit.repository.ImportRunRepository;
import com.whyuon.udit.repository.PlatformRepository;
import com.whyuon.udit.repository.PublicationRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

@Service
public class JsonImportService {

    private static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral('T')
            .appendPattern("HH:mm:ss")
            .optionalStart().appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true).optionalEnd()
            .toFormatter();

    private final ChannelRepository channelRepository;
    private final AuthorRepository authorRepository;
    private final PublicationRepository publicationRepository;
    private final PlatformRepository platformRepository;
    private final ImportRunRepository importRunRepository;
    private final ImageArchiveService imageArchiveService;
    private final ObjectMapper objectMapper;

    public JsonImportService(ChannelRepository channelRepository,
                             AuthorRepository authorRepository,
                             PublicationRepository publicationRepository,
                             PlatformRepository platformRepository,
                             ImportRunRepository importRunRepository,
                             ImageArchiveService imageArchiveService,
                             ObjectMapper objectMapper) {
        this.channelRepository = channelRepository;
        this.authorRepository = authorRepository;
        this.publicationRepository = publicationRepository;
        this.platformRepository = platformRepository;
        this.importRunRepository = importRunRepository;
        this.imageArchiveService = imageArchiveService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ImportResultResponse importFromSource(String source) throws IOException {
        ImportRun run = importRunRepository.save(new ImportRun(source));
        int inserted = 0;
        int skipped = 0;

        try {
            Resource resource = resolveResource(source);
            try (InputStream inputStream = resource.getInputStream()) {
                JsonNode root = objectMapper.readTree(inputStream);
                JsonNode items = root.path("data");

                for (JsonNode item : items) {
                    String url = item.path("url").asText(null);
                    if (url == null || url.isBlank()) {
                        continue;
                    }
                    if (publicationRepository.existsByUrl(url)) {
                        skipped++;
                        continue;
                    }

                    Channel channel = getOrCreateChannel(item);
                    Author author = getOrCreateAuthor(item.path("author"));

                    Publication publication = new Publication();
                    publication.setChannel(channel);
                    publication.setAuthor(author);
                    publication.setUrl(url);
                    publication.setImageUrl(textOrNull(item, "image_url"));
                    publication.setDatePublished(parseDate(item.path("date_published").asText()));
                    publication.setDescription(textOrNull(item, "description"));
                    publication.setPublicationId(textOrNull(item, "publication_id"));
                    publication.setFormat(textOrNull(item, "format"));
                    if (item.hasNonNull("duration")) {
                        publication.setDurationSeconds(item.get("duration").asInt());
                    }

                    Publication saved = publicationRepository.save(publication);
                    inserted++;

                    // Archivamos los bytes de la imagen para preservarla aunque
                    // el origen la borre. Si falla, no rompe la carga.
                    String imageUrl = textOrNull(item, "image_url");
                    if (imageUrl == null && author != null) {
                        imageUrl = author.getImageUrl();  // fallback al avatar del autor para blogs sin imagen
                    }
                    if (imageUrl != null) {
                        imageArchiveService.archive(saved, imageUrl);
                    }
                }
            }
            run.finishOk(inserted, skipped);
            importRunRepository.save(run);
            return new ImportResultResponse(source, inserted, skipped);
        } catch (RuntimeException | IOException e) {
            run.finishError(e.getMessage());
            importRunRepository.save(run);
            throw e;
        }
    }

    private Resource resolveResource(String source) {
        if (source.startsWith("classpath:")) {
            return new ClassPathResource(source.substring("classpath:".length()));
        }
        return new FileSystemResource(source);
    }

    private Channel getOrCreateChannel(JsonNode item) {
        String platformCode = item.path("platform").asText();
        String externalId = item.path("channel_id").asText();
        String name = item.path("channel_name").asText();

        return channelRepository.findByPlatformCodeAndExternalId(platformCode, externalId)
                .orElseGet(() -> {
                    Platform platform = platformRepository.findByCode(platformCode)
                            .orElseThrow(() -> new IllegalStateException(
                                    "Plataforma desconocida: " + platformCode
                                            + ". Anadela a la tabla platforms (seed inicial en schema.sql)."));
                    return channelRepository.save(new Channel(platform, externalId, name));
                });
    }

    private Author getOrCreateAuthor(JsonNode authorNode) {
        if (authorNode.isMissingNode() || authorNode.isNull()) {
            return null;
        }

        String url = authorNode.path("url").asText(null);
        if (url == null || url.isBlank()) {
            return null;
        }

        return authorRepository.findByUrl(url).orElseGet(() -> authorRepository.save(new Author(
                authorNode.path("name").asText(),
                url,
                textOrNull(authorNode, "image_url")
        )));
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text.isBlank() ? null : text;
    }

    private static LocalDateTime parseDate(String raw) {
        return LocalDateTime.parse(raw, DATE_FORMATTER);
    }
}
