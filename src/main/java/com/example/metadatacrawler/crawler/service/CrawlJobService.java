package com.example.metadatacrawler.crawler.service;

import com.example.metadatacrawler.api.CrawlNotFoundException;
import com.example.metadatacrawler.catalog.repository.CrawlJobRepository;
import com.example.metadatacrawler.crawler.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CrawlJobService {

    private static final Logger log = LoggerFactory.getLogger(CrawlJobService.class);

    private final CrawlJobRepository jobRepo;
    private final CrawlerService crawlerService;
    private final WebhookNotifier webhookNotifier;

    public CrawlJobService(CrawlJobRepository jobRepo, CrawlerService crawlerService, WebhookNotifier webhookNotifier) {
        this.jobRepo = jobRepo;
        this.crawlerService = crawlerService;
        this.webhookNotifier = webhookNotifier;
    }

    public CrawlJob startCrawl(List<String> targetSchemas, String callbackUrl) {
        String crawlId = UUID.randomUUID().toString();
        Instant startedAt = Instant.now();

        CrawlJob initial = new CrawlJob(
            crawlId,
            CrawlStatus.PENDING,
            targetSchemas,
            callbackUrl,
            CrawlProgress.empty(),
            startedAt,
            null,
            null
        );
        jobRepo.save(initial);

        // Step 0: 同期で実行（Step 5 で @Async 化）
        try {
            jobRepo.updateStatus(crawlId, CrawlStatus.RUNNING);

            CrawlerService.ProgressCallback callback = (total, processed, totalT, processedT) -> {
                double pct = totalT > 0 ? (processedT * 1000.0 / totalT) / 10.0 : 0.0;
                jobRepo.updateProgress(crawlId, new CrawlProgress(total, processed, totalT, processedT, pct));
            };

            CrawlSummary summary = crawlerService.crawl(crawlId, targetSchemas, callback);
            Instant completedAt = Instant.now();
            jobRepo.complete(crawlId, completedAt);

            if (callbackUrl != null && !callbackUrl.isBlank()) {
                webhookNotifier.notifyCompletion(callbackUrl, crawlId, completedAt, startedAt, summary);
            }
        } catch (Exception e) {
            log.error("[crawl] failed crawlId={}", crawlId, e);
            jobRepo.fail(crawlId, e.getMessage(), Instant.now());
            throw e;
        }

        return jobRepo.findById(crawlId);
    }

    public CrawlJob getStatus(String crawlId) {
        CrawlJob job = jobRepo.findById(crawlId);
        if (job == null) {
            throw new CrawlNotFoundException(crawlId);
        }
        return job;
    }
}
