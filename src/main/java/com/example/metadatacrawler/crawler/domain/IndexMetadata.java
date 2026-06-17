package com.example.metadatacrawler.crawler.domain;

public record IndexMetadata(
    String schemaName, String tableName, String indexName,
    boolean unique, String columnList
) {}
