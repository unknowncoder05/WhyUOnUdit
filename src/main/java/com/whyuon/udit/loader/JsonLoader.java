package com.whyuon.udit.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whyuon.udit.model.Author;
import com.whyuon.udit.model.Channel;
import com.whyuon.udit.model.Publication;
import com.whyuon.udit.repository.AuthorRepository;
import com.whyuon.udit.repository.ChannelRepository;
import com.whyuon.udit.repository.PublicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

/**
 * Lee whyuon.json del classpath y vuelca su contenido en MySQL al arrancar.
 *
 * Si la URL de una publicacion ya existe se salta, asi que se puede arrancar
 * varias veces sin duplicar datos.
 */
@Component
public class JsonLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(JsonLoader.class);

    // Las fechas del JSON vienen en varios formatos: con/sin milisegundos.
    private static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral('T')
            .appendPattern("HH:mm:ss")
            .optionalStart().appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true).optionalEnd()
            .toFormatter();

    private final ChannelRepository channelRepo;
    private final AuthorRepository authorRepo;
    private final PublicationRepository publicationRepo;
    private final ObjectMapper objectMapper;

    public JsonLoader(ChannelRepository channelRepo,
                      AuthorRepository authorRepo,
                      PublicationRepository publicationRepo,
                      ObjectMapper objectMapper) {
        this.channelRepo = channelRepo;
        this.authorRepo = authorRepo;
        this.publicationRepo = publicationRepo;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        try (InputStream in = new ClassPathResource("whyuon.json").getInputStream()) {
            JsonNode root = objectMapper.readTree(in);
            JsonNode items = root.path("data");

            int inserted = 0;
            int skipped = 0;

            for (JsonNode item : items) {
                String url = item.path("url").asText(null);
                if (url == null || url.isBlank()) {
                    log.warn("Item sin url, se ignora: {}", item);
                    continue;
                }
                if (publicationRepo.existsByUrl(url)) {
                    skipped++;
                    continue;
                }

                Channel channel = getOrCreateChannel(item);
                Author author = getOrCreateAuthor(item.path("author"));

                Publication pub = new Publication();
                pub.setChannel(channel);
                pub.setAuthor(author);
                pub.setUrl(url);
                pub.setImageUrl(textOrNull(item, "image_url"));
                pub.setDatePublished(parseDate(item.path("date_published").asText()));
                pub.setDescription(textOrNull(item, "description"));
                pub.setPublicationId(textOrNull(item, "publication_id"));
                pub.setFormat(textOrNull(item, "format"));
                if (item.hasNonNull("duration")) {
                    pub.setDurationSeconds(item.get("duration").asInt());
                }

                publicationRepo.save(pub);
                inserted++;
            }

            log.info("Carga JSON terminada: {} publicaciones insertadas, {} ya existian.",
                    inserted, skipped);
        }
    }

    private Channel getOrCreateChannel(JsonNode item) {
        String platform = item.path("platform").asText();
        String externalId = item.path("channel_id").asText();
        String name = item.path("channel_name").asText();

        return channelRepo.findByPlatformAndExternalId(platform, externalId)
                .orElseGet(() -> channelRepo.save(new Channel(platform, externalId, name)));
    }

    private Author getOrCreateAuthor(JsonNode authorNode) {
        if (authorNode.isMissingNode() || authorNode.isNull()) {
            return null;
        }
        String url = authorNode.path("url").asText(null);
        if (url == null) {
            return null;
        }
        return authorRepo.findByUrl(url).orElseGet(() -> authorRepo.save(new Author(
                authorNode.path("name").asText(),
                url,
                textOrNull(authorNode, "image_url")
        )));
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode v = node.path(field);
        if (v.isMissingNode() || v.isNull()) {
            return null;
        }
        String s = v.asText();
        return s.isBlank() ? null : s;
    }

    private static LocalDateTime parseDate(String raw) {
        return LocalDateTime.parse(raw, DATE_FORMATTER);
    }
}
