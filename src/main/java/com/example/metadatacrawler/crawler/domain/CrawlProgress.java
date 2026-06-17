package com.example.metadatacrawler.crawler.domain;

public record CrawlProgress(
    int totalSchemas, int processedSchemas,
    int totalTables, int processedTables,
    double percentage
) {
    public static CrawlProgress empty() {
        return new CrawlProgress(0, 0, 0, 0, 0.0);
    }
}
