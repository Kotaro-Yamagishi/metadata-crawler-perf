package com.example.metadatacrawler.crawler.domain;

import java.time.Instant;
import java.util.List;

public record CrawlJob(
    String id,
    CrawlStatus status,
    List<String> targetSchemas,
    String callbackUrl,
    CrawlProgress progress,
    Instant startedAt,
    Instant completedAt,
    String errorMessage
) {}
