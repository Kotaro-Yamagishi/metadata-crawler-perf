package com.example.metadatacrawler.api;

import com.example.metadatacrawler.api.dto.WebhookPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    @PostMapping("/crawl-completed")
    public Map<String, Boolean> crawlCompleted(@RequestBody WebhookPayload payload) {
        log.info("[webhook-mock] received crawlId={} status={} elapsedMs={} summary={}",
            payload.crawlId(), payload.status(), payload.elapsedMs(), payload.summary());
        return Map.of("received", true);
    }
}
