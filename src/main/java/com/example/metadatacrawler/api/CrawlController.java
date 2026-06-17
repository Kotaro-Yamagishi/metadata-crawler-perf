package com.example.metadatacrawler.api;

import com.example.metadatacrawler.api.dto.*;
import com.example.metadatacrawler.crawler.domain.CrawlJob;
import com.example.metadatacrawler.crawler.service.CrawlJobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/crawls")
public class CrawlController {

    private final CrawlJobService jobService;

    public CrawlController(CrawlJobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping("/start")
    public ResponseEntity<StartCrawlResponse> start(@Valid @RequestBody StartCrawlRequest request) {
        CrawlJob job = jobService.startCrawl(request.targetSchemas(), request.callbackUrl());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
            new StartCrawlResponse(job.id(), job.status().name(), job.startedAt())
        );
    }

    @GetMapping("/{id}")
    public CrawlStatusResponse get(@PathVariable("id") String id) {
        return CrawlStatusResponse.from(jobService.getStatus(id));
    }
}
