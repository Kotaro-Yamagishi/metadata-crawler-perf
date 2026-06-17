package com.example.metadatacrawler.seeder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * seed プロファイル有効時にのみ起動し、ソース MySQL に 8 スキーマ × 計約 850 テーブルを投入する。
 *
 * <p>実行コマンド例:
 * <pre>
 *   docker compose run --rm app java -jar /app/app.jar --spring.profiles.active=seed
 * </pre>
 *
 * <p>投入フロー:
 * <ol>
 *   <li>スキーマを DROP → CREATE</li>
 *   <li>全テーブルの CREATE TABLE を実行</li>
 *   <li>全インデックスを CREATE INDEX で追加</li>
 *   <li>全 FK を ALTER TABLE ADD CONSTRAINT で追加（失敗は warn のみ）</li>
 * </ol>
 */
@Component
@Profile("seed")
public class DatabaseSeederRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeederRunner.class);

    private final JdbcTemplate sourceJdbcTemplate;

    public DatabaseSeederRunner(@Qualifier("sourceJdbcTemplate") JdbcTemplate sourceJdbcTemplate) {
        this.sourceJdbcTemplate = sourceJdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        long startMs = System.currentTimeMillis();
        log.info("[seeder] Starting database seeding...");

        for (SchemaDefinition def : SchemaDefinitions.ALL) {
            seedSchema(def);
        }

        long elapsedMs = System.currentTimeMillis() - startMs;
        log.info("[seeder] Completed in {} ms", elapsedMs);
    }

    private void seedSchema(SchemaDefinition def) {
        log.info("[seeder] schema={} target={}", def.schemaName(), def.targetTableCount());

        sourceJdbcTemplate.execute("DROP DATABASE IF EXISTS `" + def.schemaName() + "`");
        sourceJdbcTemplate.execute("CREATE DATABASE `" + def.schemaName()
                + "` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");

        List<TableSpec> tables = SchemaGenerator.generate(def);

        // Phase 1: CREATE TABLE + CREATE INDEX（FK なし）
        for (TableSpec table : tables) {
            String ddl = DdlBuilder.buildCreateTable(def.schemaName(), table);
            try {
                sourceJdbcTemplate.execute(ddl);
            } catch (Exception e) {
                log.warn("[seeder] CREATE TABLE failed: {}.{} - {}", def.schemaName(), table.tableName(), e.getMessage());
                continue;
            }

            for (IndexSpec idx : table.indexes()) {
                String idxDdl = DdlBuilder.buildCreateIndex(def.schemaName(), table.tableName(), idx);
                try {
                    sourceJdbcTemplate.execute(idxDdl);
                } catch (Exception e) {
                    log.warn("[seeder] CREATE INDEX failed: {} - {}", idx.indexName(), e.getMessage());
                }
            }
        }

        // Phase 2: ALTER TABLE ADD CONSTRAINT FOREIGN KEY
        int fkCount = 0;
        int fkFailed = 0;
        for (TableSpec table : tables) {
            for (ForeignKeySpec fk : table.foreignKeys()) {
                String fkDdl = DdlBuilder.buildAddForeignKey(def.schemaName(), table.tableName(), fk);
                try {
                    sourceJdbcTemplate.execute(fkDdl);
                    fkCount++;
                } catch (Exception e) {
                    log.warn("[seeder] FK creation failed: {} - {}", fk.constraintName(), e.getMessage());
                    fkFailed++;
                }
            }
        }

        log.info("[seeder] schema={} created tables={} fks_ok={} fks_failed={}",
                def.schemaName(), tables.size(), fkCount, fkFailed);
    }
}
