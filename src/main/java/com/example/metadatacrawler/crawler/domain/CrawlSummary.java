package com.example.metadatacrawler.crawler.domain;

public record CrawlSummary(int schemas, int tables, int columns, int foreignKeys, int indexes) {}
