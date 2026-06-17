package com.example.metadatacrawler.crawler.domain;

public record ForeignKeyMetadata(
    String schemaName, String tableName, String fromColumn,
    String toSchema, String toTable, String toColumn,
    String constraintName
) {}
