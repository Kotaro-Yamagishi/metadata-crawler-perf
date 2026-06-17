package com.example.metadatacrawler.api.dto;

import java.time.Instant;

public record WebhookPayload(
    String crawlId,
    String status,
    Instant completedAt,
    long elapsedMs,
    Summary summary
) {
    public record Summary(int schemas, int tables, int columns, int foreignKeys, int indexes) {}
}
