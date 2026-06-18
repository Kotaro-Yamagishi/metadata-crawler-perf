package com.example.metadatacrawler.crawler.client;

import com.example.metadatacrawler.crawler.domain.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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

    // Step 0: 本当に酷いナイーブ実装
    // カラム名一覧を取得 → 各カラムごとに詳細を別クエリで取得（カラム単位 N+1）
    public List<ColumnMetadata> listColumns(String schemaName, String tableName) {
        List<String> columnNames = sourceJdbc.queryForList(
            "SELECT COLUMN_NAME FROM information_schema.COLUMNS " +
            "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION",
            String.class, schemaName, tableName
        );

        List<ColumnMetadata> result = new ArrayList<>(columnNames.size());
        for (String col : columnNames) {
            ColumnMetadata meta = sourceJdbc.queryForObject(
                "SELECT DATA_TYPE, IS_NULLABLE, COLUMN_COMMENT, ORDINAL_POSITION " +
                "FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                (rs, i) -> new ColumnMetadata(
                    schemaName, tableName, col,
                    rs.getString("DATA_TYPE"),
                    "YES".equalsIgnoreCase(rs.getString("IS_NULLABLE")),
                    rs.getString("COLUMN_COMMENT"),
                    rs.getInt("ORDINAL_POSITION")
                ),
                schemaName, tableName, col
            );
            result.add(meta);
        }
        return result;
    }

    // Step 0: テーブル1つにつき1クエリ → N+1
    public List<ForeignKeyMetadata> listForeignKeys(String schemaName, String tableName) {
        return sourceJdbc.query(
            "SELECT CONSTRAINT_NAME, COLUMN_NAME, REFERENCED_TABLE_SCHEMA, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME " +
            "FROM information_schema.KEY_COLUMN_USAGE " +
            "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND REFERENCED_TABLE_NAME IS NOT NULL",
            (rs, i) -> new ForeignKeyMetadata(
                schemaName, tableName,
                rs.getString("COLUMN_NAME"),
                rs.getString("REFERENCED_TABLE_SCHEMA"),
                rs.getString("REFERENCED_TABLE_NAME"),
                rs.getString("REFERENCED_COLUMN_NAME"),
                rs.getString("CONSTRAINT_NAME")
            ),
            schemaName, tableName
        );
    }

    // Step 0: テーブル1つにつき1クエリ → N+1
    public List<IndexMetadata> listIndexes(String schemaName, String tableName) {
        return sourceJdbc.query(
            "SELECT INDEX_NAME, NON_UNIQUE, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS COLUMN_LIST " +
            "FROM information_schema.STATISTICS " +
            "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? " +
            "GROUP BY INDEX_NAME, NON_UNIQUE",
            (rs, i) -> new IndexMetadata(
                schemaName, tableName,
                rs.getString("INDEX_NAME"),
                rs.getInt("NON_UNIQUE") == 0,
                rs.getString("COLUMN_LIST")
            ),
            schemaName, tableName
        );
    }
}
