package com.whyuon.udit.dto;

public record ImportResultResponse(
        String source,
        int inserted,
        int skipped
) {
}
