package com.example.metadatacrawler.api;

public class CrawlNotFoundException extends RuntimeException {
    public CrawlNotFoundException(String crawlId) {
        super("Crawl with id '" + crawlId + "' was not found");
    }
}
