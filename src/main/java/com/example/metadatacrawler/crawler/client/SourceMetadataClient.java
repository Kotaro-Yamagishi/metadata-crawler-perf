package com.example.metadatacrawler.crawler.client;

import com.example.metadatacrawler.crawler.domain.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SourceMetadataClient {

    private static final List<String> SYSTEM_SCHEMAS = List.of(
        "information_schema", "mysql", "performance_schema", "sys"
    );

    private final JdbcTemplate sourceJdbc;

    public SourceMetadataClient(@Qualifier("sourceJdbcTemplate") JdbcTemplate sourceJdbc) {
        this.sourceJdbc = sourceJdbc;
    }

    public List<String> listSchemas(List<String> targetSchemas) {
        if (targetSchemas != null && !targetSchemas.isEmpty()) {
            // Step 0: 各 schema を1件ずつ存在確認（N+1）
            return targetSchemas.stream()
                .filter(s -> {
                    Integer count = sourceJdbc.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = ?",
                        Integer.class, s
                    );
                    return count != null && count > 0;
                })
                .toList();
        }
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < SYSTEM_SCHEMAS.size(); i++) {
            if (i > 0) placeholders.append(", ");
            placeholders.append("?");
        }
        String sql = "SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME NOT IN (" + placeholders + ")";
        return sourceJdbc.queryForList(sql, String.class, SYSTEM_SCHEMAS.toArray());
    }

    public List<TableMetadata> listTables(String schemaName) {
        // スキーマ単位の取得（テーブルごとの N+1 ではない）
        return sourceJdbc.query(
            "SELECT TABLE_NAME, TABLE_COMMENT FROM information_schema.TABLES " +
            "WHERE TABLE_SCHEMA = ? AND TABLE_TYPE = 'BASE TABLE'",
            (rs, i) -> new TableMetadata(schemaName, rs.getString("TABLE_NAME"), rs.getString("TABLE_COMMENT")),
            schemaName
        );
    }

    // Step 1: スキーマ単位で全テーブルのカラムを1クエリで取得
    public Map<String, List<ColumnMetadata>> listColumnsBySchema(String schemaName) {
        String sql = "SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_COMMENT, ORDINAL_POSITION " +
                     "FROM information_schema.COLUMNS " +
                     "WHERE TABLE_SCHEMA = ? " +
                     "ORDER BY TABLE_NAME, ORDINAL_POSITION";
        Map<String, List<ColumnMetadata>> result = new HashMap<>();
        sourceJdbc.query(sql, rs -> {
            String tableName = rs.getString("TABLE_NAME");
            result.computeIfAbsent(tableName, k -> new ArrayList<>())
                  .add(new ColumnMetadata(
                      schemaName, tableName,
                      rs.getString("COLUMN_NAME"),
                      rs.getString("DATA_TYPE"),
                      "YES".equalsIgnoreCase(rs.getString("IS_NULLABLE")),
                      rs.getString("COLUMN_COMMENT"),
                      rs.getInt("ORDINAL_POSITION")
                  ));
        }, schemaName);
        return result;
    }

    // Step 1: スキーマ単位で全テーブルの FK を1クエリで取得
    public Map<String, List<ForeignKeyMetadata>> listForeignKeysBySchema(String schemaName) {
        String sql = "SELECT TABLE_NAME, CONSTRAINT_NAME, COLUMN_NAME, " +
                     "REFERENCED_TABLE_SCHEMA, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME " +
                     "FROM information_schema.KEY_COLUMN_USAGE " +
                     "WHERE TABLE_SCHEMA = ? AND REFERENCED_TABLE_NAME IS NOT NULL";
        Map<String, List<ForeignKeyMetadata>> result = new HashMap<>();
        sourceJdbc.query(sql, rs -> {
            String tableName = rs.getString("TABLE_NAME");
            result.computeIfAbsent(tableName, k -> new ArrayList<>())
                  .add(new ForeignKeyMetadata(
                      schemaName, tableName,
                      rs.getString("COLUMN_NAME"),
                      rs.getString("REFERENCED_TABLE_SCHEMA"),
                      rs.getString("REFERENCED_TABLE_NAME"),
                      rs.getString("REFERENCED_COLUMN_NAME"),
                      rs.getString("CONSTRAINT_NAME")
                  ));
        }, schemaName);
        return result;
    }

    // Step 1: スキーマ単位で全テーブルの Index を1クエリで取得
    public Map<String, List<IndexMetadata>> listIndexesBySchema(String schemaName) {
        String sql = "SELECT TABLE_NAME, INDEX_NAME, NON_UNIQUE, " +
                     "GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS COLUMN_LIST " +
                     "FROM information_schema.STATISTICS " +
                     "WHERE TABLE_SCHEMA = ? " +
                     "GROUP BY TABLE_NAME, INDEX_NAME, NON_UNIQUE";
        Map<String, List<IndexMetadata>> result = new HashMap<>();
        sourceJdbc.query(sql, rs -> {
            String tableName = rs.getString("TABLE_NAME");
            result.computeIfAbsent(tableName, k -> new ArrayList<>())
                  .add(new IndexMetadata(
                      schemaName, tableName,
                      rs.getString("INDEX_NAME"),
                      rs.getInt("NON_UNIQUE") == 0,
                      rs.getString("COLUMN_LIST")
                  ));
        }, schemaName);
        return result;
    }
}
