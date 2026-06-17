package com.example.metadatacrawler.crawler.repository;

import com.example.metadatacrawler.crawler.domain.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;

@Component
public class CatalogWriter {

    private final JdbcTemplate catalogJdbc;

    public CatalogWriter(@Qualifier("catalogJdbcTemplate") JdbcTemplate catalogJdbc) {
        this.catalogJdbc = catalogJdbc;
    }

    public void truncateAll() {
        catalogJdbc.execute("SET REFERENTIAL_INTEGRITY FALSE");
        catalogJdbc.execute("TRUNCATE TABLE catalog_columns");
        catalogJdbc.execute("TRUNCATE TABLE catalog_foreign_keys");
        catalogJdbc.execute("TRUNCATE TABLE catalog_indexes");
        catalogJdbc.execute("TRUNCATE TABLE catalog_tables");
        catalogJdbc.execute("TRUNCATE TABLE catalog_schemas");
        catalogJdbc.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    public long insertSchema(String crawlId, String schemaName, Instant crawledAt) {
        KeyHolder kh = new GeneratedKeyHolder();
        catalogJdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO catalog_schemas (schema_name, crawl_id, crawled_at) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, schemaName);
            ps.setString(2, crawlId);
            ps.setTimestamp(3, Timestamp.from(crawledAt));
            return ps;
        }, kh);
        return kh.getKey().longValue();
    }

    public long insertTable(long schemaId, TableMetadata table) {
        KeyHolder kh = new GeneratedKeyHolder();
        catalogJdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO catalog_tables (schema_id, table_name, table_comment) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, schemaId);
            ps.setString(2, table.tableName());
            ps.setString(3, table.tableComment());
            return ps;
        }, kh);
        return kh.getKey().longValue();
    }

    // Step 0: 1件ずつ INSERT
    public void insertColumn(long tableId, ColumnMetadata col) {
        catalogJdbc.update(
            "INSERT INTO catalog_columns (table_id, column_name, data_type, is_nullable, column_comment, ordinal_position) " +
            "VALUES (?, ?, ?, ?, ?, ?)",
            tableId, col.columnName(), col.dataType(), col.nullable(), col.columnComment(), col.ordinalPosition()
        );
    }

    public void insertForeignKey(long fromTableId, ForeignKeyMetadata fk) {
        catalogJdbc.update(
            "INSERT INTO catalog_foreign_keys (from_table_id, from_column, to_schema, to_table, to_column, constraint_name) " +
            "VALUES (?, ?, ?, ?, ?, ?)",
            fromTableId, fk.fromColumn(), fk.toSchema(), fk.toTable(), fk.toColumn(), fk.constraintName()
        );
    }

    public void insertIndex(long tableId, IndexMetadata idx) {
        catalogJdbc.update(
            "INSERT INTO catalog_indexes (table_id, index_name, is_unique, column_list) VALUES (?, ?, ?, ?)",
            tableId, idx.indexName(), idx.unique(), idx.columnList()
        );
    }
}
