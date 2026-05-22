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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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
                    publication.setExtraData(buildExtraData(item));

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

    /**
     * Construye el Map de atributos específicos de la plataforma a partir
     * de los campos del JSON que NO son los comunes (platform, channel_id,
     * channel_name, url, image_url, date_published, description, author).
     *
     * Para añadir una plataforma nueva (Instagram, Twitch, X...), basta con
     * añadir un nuevo `case` aquí con sus campos. El esquema de BD no
     * cambia: todo entra como JSON en la columna `extra_data`.
     */
    private Map<String, Object> buildExtraData(JsonNode item) {
        Map<String, Object> extras = new LinkedHashMap<>();
        String platform = item.path("platform").asText();

        switch (platform.toLowerCase()) {
            case "youtube" -> {
                putIfPresent(extras, item, "publication_id");
                putIfPresent(extras, item, "format");
                if (item.hasNonNull("duration")) {
                    extras.put("duration_seconds", item.get("duration").asInt());
                }
            }
            case "blog" -> {
                // Hoy los blogs no traen campos extra. Si en el futuro
                // tuviesen (ej. reading_time), los añadiriamos aqui.
            }
            // Patron a seguir para nuevas plataformas:
            // case "instagram" -> { putIfPresent(extras, item, "likes_count"); ... }
            // case "twitch"    -> { putIfPresent(extras, item, "game_category"); ... }
            // case "x"         -> { putIfPresent(extras, item, "retweets"); ... }
            default -> {
                // Plataforma desconocida: guardamos todos los campos no comunes
                // tal cual, para no perder informacion mientras se ajusta el codigo.
                item.fields().forEachRemaining(entry -> {
                    String key = entry.getKey();
                    if (!COMMON_FIELDS.contains(key)) {
                        extras.put(key, entry.getValue());
                    }
                });
            }
        }
        return extras;
    }

    private static final Set<String> COMMON_FIELDS = Set.of(
            "platform", "channel_id", "channel_name", "url", "image_url",
            "date_published", "description", "author"
    );

    private static void putIfPresent(Map<String, Object> target, JsonNode item, String field) {
        if (item.hasNonNull(field)) {
            JsonNode value = item.get(field);
            if (value.isTextual()) {
                target.put(field, value.asText());
            } else if (value.isInt() || value.isLong()) {
                target.put(field, value.asLong());
            } else if (value.isNumber()) {
                target.put(field, value.asDouble());
            } else if (value.isBoolean()) {
                target.put(field, value.asBoolean());
            } else {
                target.put(field, value);
            }
        }
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
