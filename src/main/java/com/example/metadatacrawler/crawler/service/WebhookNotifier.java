package com.example.metadatacrawler.crawler.service;

import com.example.metadatacrawler.api.dto.WebhookPayload;
import com.example.metadatacrawler.crawler.domain.CrawlStatus;
import com.example.metadatacrawler.crawler.domain.CrawlSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;

@Component
public class WebhookNotifier {

    private static final Logger log = LoggerFactory.getLogger(WebhookNotifier.class);
    private final RestClient restClient = RestClient.builder().build();

    public void notifyCompletion(String callbackUrl, String crawlId, Instant completedAt, Instant startedAt, CrawlSummary summary) {
        long elapsedMs = Duration.between(startedAt, completedAt).toMillis();
        WebhookPayload payload = new WebhookPayload(
            crawlId,
            CrawlStatus.COMPLETED.name(),
            completedAt,
            elapsedMs,
            new WebhookPayload.Summary(
                summary.schemas(), summary.tables(), summary.columns(),
                summary.foreignKeys(), summary.indexes()
            )
        );
        try {
            restClient.post()
                .uri(callbackUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
            log.info("[webhook] notified callbackUrl={} crawlId={}", callbackUrl, crawlId);
        } catch (Exception e) {
            log.warn("[webhook] failed callbackUrl={} crawlId={} err={}", callbackUrl, crawlId, e.getMessage());
        }
    }
}
