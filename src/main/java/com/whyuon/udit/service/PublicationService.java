package com.whyuon.udit.service;

import com.whyuon.udit.dto.ChannelSummaryResponse;
import com.whyuon.udit.dto.PaginatedPublicationsResponse;
import com.whyuon.udit.dto.PublicationResponse;
import com.whyuon.udit.dto.StatsResponse;
import com.whyuon.udit.model.Channel;
import com.whyuon.udit.model.Publication;
import com.whyuon.udit.repository.PublicationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PublicationService {

    private final PublicationRepository publicationRepository;

    public PublicationService(PublicationRepository publicationRepository) {
        this.publicationRepository = publicationRepository;
    }

    public List<PublicationResponse> getPublications() {
        return publicationRepository.findAllByOrderByDatePublishedDesc().stream()
                .map(this::toPublicationResponse)
                .toList();
    }

    /**
     * Devuelve una página de publicaciones ordenadas por fecha descendente.
     * Pensado para el listado de la home: el frontend solo pide la página
     * que está mostrando, no toda la BD.
     */
    public PaginatedPublicationsResponse getPublicationsPage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "datePublished"));
        Page<Publication> result = publicationRepository.findAll(pageable);

        List<PublicationResponse> content = result.getContent().stream()
                .map(this::toPublicationResponse)
                .toList();

        return new PaginatedPublicationsResponse(
                content,
                result.getNumber(),
                result.getTotalPages(),
                result.getTotalElements(),
                result.getSize()
        );
    }

    public List<ChannelSummaryResponse> getChannelSummaries() {
        Map<Long, ChannelAccumulator> channels = new LinkedHashMap<>();

        for (Publication publication : publicationRepository.findAllByOrderByDatePublishedDesc()) {
            Channel channel = publication.getChannel();
            ChannelAccumulator accumulator = channels.computeIfAbsent(
                    channel.getId(),
                    ignored -> new ChannelAccumulator(channel)
            );

            accumulator.publicationsCount++;
            if (accumulator.latestPublicationDate == null
                    || publication.getDatePublished().isAfter(accumulator.latestPublicationDate)) {
                accumulator.latestPublicationDate = publication.getDatePublished();
                accumulator.latestPublicationUrl = publication.getUrl();
                accumulator.latestPublicationId = publication.getId();
            }
            if (accumulator.imageUrl == null && publication.getImageUrl() != null) {
                accumulator.imageUrl = publication.getImageUrl();
            }
            if (accumulator.description == null) {
                accumulator.description = summarize(publication.getDescription());
            }
        }

        List<ChannelSummaryResponse> response = new ArrayList<>();
        for (ChannelAccumulator accumulator : channels.values()) {
            response.add(new ChannelSummaryResponse(
                    accumulator.channel.getName(),
                    accumulator.channel.getPlatform().getCode(),
                    accumulator.channel.getExternalId(),
                    buildChannelUrl(accumulator.channel),
                    accumulator.imageUrl,
                    accumulator.description,
                    accumulator.publicationsCount,
                    accumulator.latestPublicationDate,
                    accumulator.latestPublicationUrl,
                    accumulator.latestPublicationId
            ));
        }
        return response;
    }

    /**
     * Construye el CSV aplicando filtros opcionales. Si todos los
     * parámetros son null o cadena vacía, se devuelve el listado
     * completo (comportamiento equivalente al endpoint sin parámetros).
     *
     *   platform: coincidencia exacta del código ('youtube', 'blog', ...).
     *   channel:  coincidencia parcial sobre el nombre del canal,
     *             case-insensitive.
     *   from/to:  rango por fecha de publicación.
     */
    public String buildCsvReport(String platform, String channel, LocalDate from, LocalDate to) {
        LocalDateTime fromDateTime = from != null ? from.atStartOfDay()       : null;
        LocalDateTime toDateTime   = to   != null ? to.atTime(LocalTime.MAX)   : null;
        String platformFilter      = isBlank(platform) ? null : platform;
        String channelFilter       = isBlank(channel)  ? null : channel;

        List<PublicationResponse> publications = publicationRepository
                .findForReport(platformFilter, channelFilter, fromDateTime, toDateTime)
                .stream()
                .map(this::toPublicationResponse)
                .toList();

        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF'); // BOM UTF-8 para que Excel reconozca tildes y eñes
        csv.append(String.join(";",
                escapeCsv("Plataforma"),
                escapeCsv("Canal"),
                escapeCsv("Autor"),
                escapeCsv("Título"),
                escapeCsv("Fecha de publicación"),
                escapeCsv("Formato"),
                escapeCsv("Duración"),
                escapeCsv("URL")
        )).append('\n');
        for (PublicationResponse publication : publications) {
            Map<String, Object> extras = publication.extraData();
            String format = extras != null && extras.get("format") != null
                    ? extras.get("format").toString() : "";
            Integer durationSeconds = extras != null && extras.get("duration_seconds") instanceof Number n
                    ? n.intValue() : null;

            csv.append(escapeCsv(capitalizePlatform(publication.platform()))).append(';')
                    .append(escapeCsv(publication.channelName())).append(';')
                    .append(escapeCsv(publication.authorName())).append(';')
                    .append(escapeCsv(publication.title())).append(';')
                    .append(escapeCsv(formatDate(publication.datePublished()))).append(';')
                    .append(escapeCsv(format)).append(';')
                    .append(escapeCsv(formatDuration(durationSeconds))).append(';')
                    .append(escapeCsv(publication.url()))
                    .append('\n');
        }
        return csv.toString();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /**
     * Devuelve un resumen estadístico aplicando los mismos filtros que el
     * CSV. Pensado para el dashboard: cuando el usuario cambia los filtros
     * y pulsa Aplicar, el frontend pide estos números para pintarlos en
     * tarjetas y barras.
     */
    public StatsResponse getStats(String platform, String channel, LocalDate from, LocalDate to) {
        LocalDateTime fromDateTime = from != null ? from.atStartOfDay()     : null;
        LocalDateTime toDateTime   = to   != null ? to.atTime(LocalTime.MAX) : null;
        String platformFilter      = isBlank(platform) ? null : platform;
        String channelFilter       = isBlank(channel)  ? null : channel;

        List<Publication> publications = publicationRepository.findForReport(
                platformFilter, channelFilter, fromDateTime, toDateTime
        );

        // Conteo por plataforma. LinkedHashMap mantiene orden estable.
        Map<String, Long> byPlatform = new LinkedHashMap<>();
        Map<Long, ChannelCounter> channelCounters = new LinkedHashMap<>();
        LocalDateTime earliest = null;
        LocalDateTime latest   = null;

        for (Publication p : publications) {
            String platformCode = p.getChannel().getPlatform().getCode();
            byPlatform.merge(platformCode, 1L, Long::sum);

            Channel c = p.getChannel();
            channelCounters.computeIfAbsent(
                    c.getId(),
                    ignored -> new ChannelCounter(c.getName(), platformCode)
            ).increment();

            if (earliest == null || p.getDatePublished().isBefore(earliest)) {
                earliest = p.getDatePublished();
            }
            if (latest == null || p.getDatePublished().isAfter(latest)) {
                latest = p.getDatePublished();
            }
        }

        // Ordenamos por cuenta descendente para que el dashboard pinte
        // primero el canal más activo.
        List<StatsResponse.ChannelCount> byChannel = channelCounters.values().stream()
                .sorted((a, b) -> Long.compare(b.count, a.count))
                .map(cc -> new StatsResponse.ChannelCount(cc.name, cc.platformCode, cc.count))
                .toList();

        return new StatsResponse(
                publications.size(),
                byPlatform,
                byChannel,
                earliest,
                latest
        );
    }

    private static final class ChannelCounter {
        private final String name;
        private final String platformCode;
        private long count;

        ChannelCounter(String name, String platformCode) {
            this.name = name;
            this.platformCode = platformCode;
            this.count = 0;
        }

        void increment() {
            count++;
        }
    }

    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    private String formatDuration(Integer seconds) {
        if (seconds == null) return "";
        int h = seconds / 3600;
        int m = (seconds % 3600) / 60;
        int s = seconds % 60;
        if (h > 0) return String.format("%dh %02dm %02ds", h, m, s);
        if (m > 0) return String.format("%dm %02ds", m, s);
        return String.format("%ds", s);
    }

    private String capitalizePlatform(String platform) {
        if (platform == null || platform.isBlank()) return "";
        return switch (platform.toLowerCase()) {
            case "youtube" -> "YouTube";
            case "tiktok" -> "TikTok";
            // Para cualquier plataforma nueva (instagram, twitch, blog…) se capitaliza la inicial.
            default -> Character.toUpperCase(platform.charAt(0)) + platform.substring(1).toLowerCase();
        };
    }

    private PublicationResponse toPublicationResponse(Publication publication) {
        return new PublicationResponse(
                publication.getId(),
                buildTitle(publication),
                publication.getChannel().getName(),
                publication.getChannel().getPlatform().getCode(),
                publication.getAuthor() == null ? null : publication.getAuthor().getName(),
                publication.getAuthor() == null ? null : publication.getAuthor().getImageUrl(),
                publication.getUrl(),
                publication.getImageUrl(),
                publication.getDescription(),
                publication.getExtraData(),
                publication.getDatePublished()
        );
    }

    private String buildTitle(Publication publication) {
        String description = publication.getDescription();
        if (description == null || description.isBlank()) {
            return publication.getChannel().getName() + " · " + publication.getDatePublished().toLocalDate();
        }
        String normalized = description.replace("\r", "\n").trim();
        String firstLine = normalized.contains("\n")
                ? normalized.substring(0, normalized.indexOf('\n')).trim()
                : normalized;
        if (firstLine.length() > 120) {
            return firstLine.substring(0, 117) + "...";
        }
        return firstLine;
    }

    private String summarize(String description) {
        if (description == null || description.isBlank()) {
            return "Sin descripcion disponible.";
        }
        String normalized = description.replace("\r", " ").replace("\n", " ").trim();
        if (normalized.length() > 180) {
            return normalized.substring(0, 177) + "...";
        }
        return normalized;
    }

    private String buildChannelUrl(Channel channel) {
        if ("youtube".equalsIgnoreCase(channel.getPlatform().getCode())) {
            return "https://www.youtube.com/channel/" + channel.getExternalId();
        }
        return channel.getExternalId();
    }

    private String escapeCsv(String value) {
        String normalized = value == null ? "" : value;
        return "\"" + normalized.replace("\"", "\"\"") + "\"";
    }

    private static final class ChannelAccumulator {
        private final Channel channel;
        private String imageUrl;
        private String description;
        private long publicationsCount;
        private LocalDateTime latestPublicationDate;
        private String latestPublicationUrl;
        private Long latestPublicationId;

        private ChannelAccumulator(Channel channel) {
            this.channel = channel;
        }
    }
}
