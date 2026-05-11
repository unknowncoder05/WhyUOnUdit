package com.whyuon.udit.service;

import com.whyuon.udit.dto.ChannelSummaryResponse;
import com.whyuon.udit.dto.PublicationResponse;
import com.whyuon.udit.model.Channel;
import com.whyuon.udit.model.Publication;
import com.whyuon.udit.repository.PublicationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
                    accumulator.channel.getPlatform(),
                    accumulator.channel.getExternalId(),
                    buildChannelUrl(accumulator.channel),
                    accumulator.imageUrl,
                    accumulator.description,
                    accumulator.publicationsCount,
                    accumulator.latestPublicationDate,
                    accumulator.latestPublicationUrl
            ));
        }
        return response;
    }

    public String buildCsvReport() {
        StringBuilder csv = new StringBuilder();
        csv.append("title,channel,platform,author,date_published,url,format,duration_seconds\n");
        for (PublicationResponse publication : getPublications()) {
            csv.append(escapeCsv(publication.title())).append(',')
                    .append(escapeCsv(publication.channelName())).append(',')
                    .append(escapeCsv(publication.platform())).append(',')
                    .append(escapeCsv(publication.authorName())).append(',')
                    .append(escapeCsv(String.valueOf(publication.datePublished()))).append(',')
                    .append(escapeCsv(publication.url())).append(',')
                    .append(escapeCsv(publication.format())).append(',')
                    .append(escapeCsv(publication.durationSeconds() == null ? "" : publication.durationSeconds().toString()))
                    .append('\n');
        }
        return csv.toString();
    }

    private PublicationResponse toPublicationResponse(Publication publication) {
        return new PublicationResponse(
                publication.getId(),
                buildTitle(publication),
                publication.getChannel().getName(),
                publication.getChannel().getPlatform(),
                publication.getAuthor() == null ? null : publication.getAuthor().getName(),
                publication.getUrl(),
                publication.getImageUrl(),
                publication.getDescription(),
                publication.getFormat(),
                publication.getDurationSeconds(),
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
        if ("youtube".equalsIgnoreCase(channel.getPlatform())) {
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

        private ChannelAccumulator(Channel channel) {
            this.channel = channel;
        }
    }
}
