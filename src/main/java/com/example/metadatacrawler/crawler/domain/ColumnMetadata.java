package com.example.metadatacrawler.crawler.domain;

public record ColumnMetadata(
    String schemaName, String tableName, String columnName,
    String dataType, boolean nullable, String columnComment,
    int ordinalPosition
) {}
