package com.example.metadatacrawler.api.dto;

import java.time.Instant;

public record StartCrawlResponse(
    String crawlId,
    String status,
    Instant startedAt
) {}
