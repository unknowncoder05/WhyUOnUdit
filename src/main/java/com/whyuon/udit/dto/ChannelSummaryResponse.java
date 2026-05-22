package com.whyuon.udit.dto;

import java.time.LocalDateTime;

public record ChannelSummaryResponse(
        String name,
        String platform,
        String externalId,
        String channelUrl,
        String imageUrl,
        String description,
        long publicationsCount,
        LocalDateTime latestPublicationDate,
        String latestPublicationUrl,
        Long latestPublicationId
) {
}
