package com.whyuon.udit.dto;

import java.time.LocalDateTime;

public record PublicationResponse(
        Long id,
        String title,
        String channelName,
        String platform,
        String authorName,
        String authorImageUrl,
        String url,
        String imageUrl,
        String description,
        String format,
        Integer durationSeconds,
        LocalDateTime datePublished
) {
}
