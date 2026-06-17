package com.example.metadatacrawler.api.dto;

import com.example.metadatacrawler.crawler.domain.CrawlJob;
import com.example.metadatacrawler.crawler.domain.CrawlProgress;

import java.time.Duration;
import java.time.Instant;

public record CrawlStatusResponse(
    String crawlId,
    String status,
    Instant startedAt,
    Instant completedAt,
    CrawlProgress progress,
    long elapsedMs
) {
    public static CrawlStatusResponse from(CrawlJob job) {
        Instant end = job.completedAt() != null ? job.completedAt() : Instant.now();
        long elapsed = Duration.between(job.startedAt(), end).toMillis();
        return new CrawlStatusResponse(
            job.id(),
            job.status().name(),
            job.startedAt(),
            job.completedAt(),
            job.progress(),
            elapsed
        );
    }
}
