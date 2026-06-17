package com.example.metadatacrawler.api.dto;

import jakarta.validation.constraints.Pattern;
import java.util.List;

public record StartCrawlRequest(
    List<String> targetSchemas,
    @Pattern(regexp = "^(https?://.+)?$", message = "callbackUrl must be a valid http(s) URL")
    String callbackUrl
) {}
