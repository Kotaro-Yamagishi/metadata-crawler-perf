package com.example.metadatacrawler.crawler.service;

import com.example.metadatacrawler.common.CrawlerStopWatch;
import com.example.metadatacrawler.crawler.client.SourceMetadataClient;
import com.example.metadatacrawler.crawler.domain.*;
import com.example.metadatacrawler.crawler.repository.CatalogWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class CrawlerService {

    private static final Logger log = LoggerFactory.getLogger(CrawlerService.class);

    private final SourceMetadataClient sourceClient;
    private final CatalogWriter catalogWriter;

    public CrawlerService(SourceMetadataClient sourceClient, CatalogWriter catalogWriter) {
        this.sourceClient = sourceClient;
        this.catalogWriter = catalogWriter;
    }

    @FunctionalInterface
    public interface ProgressCallback {
        void update(int totalSchemas, int processedSchemas, int totalTables, int processedTables);
    }

    // Step 0: 全体を1トランザクションで囲む（巨大トランザクション）
    @Transactional("catalogTransactionManager")
    public CrawlSummary crawl(String crawlId, List<String> targetSchemas, ProgressCallback progressCallback) {
        CrawlerStopWatch sw = new CrawlerStopWatch(CrawlerService.class);
        sw.start("truncate");
        catalogWriter.truncateAll();
        sw.stop();

        sw.start("list-schemas");
        List<String> schemas = sourceClient.listSchemas(targetSchemas);
        sw.stop();

        // Step 0: 総テーブル数を事前に取得（後の処理で再度取得するので無駄）
        sw.start("count-tables");
        int totalTables = 0;
        for (String schema : schemas) {
            totalTables += sourceClient.listTables(schema).size();
        }
        sw.stop();

        progressCallback.update(schemas.size(), 0, totalTables, 0);

        int summarySchemas = 0, summaryTables = 0, summaryCols = 0, summaryFks = 0, summaryIdx = 0;
        int processedSchemas = 0, processedTables = 0;
        Instant now = Instant.now();

        sw.start("crawl");
        for (String schemaName : schemas) {
            log.info("[crawl] schema={}", schemaName);
            long schemaId = catalogWriter.insertSchema(crawlId, schemaName, now);
            summarySchemas++;

            // Step 1: スキーマ単位でメタデータを一括取得（IN句・JOIN相当）
            List<TableMetadata> tables = sourceClient.listTables(schemaName);
            Map<String, List<ColumnMetadata>> columnsByTable = sourceClient.listColumnsBySchema(schemaName);
            Map<String, List<ForeignKeyMetadata>> fksByTable = sourceClient.listForeignKeysBySchema(schemaName);
            Map<String, List<IndexMetadata>> indexesByTable = sourceClient.listIndexesBySchema(schemaName);

            for (TableMetadata table : tables) {
                long tableId = catalogWriter.insertTable(schemaId, table);
                summaryTables++;

                // 取得済みデータをテーブル名で引いて INSERT（書き込み側はナイーブのまま）
                for (ColumnMetadata col : columnsByTable.getOrDefault(table.tableName(), List.of())) {
                    catalogWriter.insertColumn(tableId, col);
                    summaryCols++;
                }
                for (ForeignKeyMetadata fk : fksByTable.getOrDefault(table.tableName(), List.of())) {
                    catalogWriter.insertForeignKey(tableId, fk);
                    summaryFks++;
                }
                for (IndexMetadata idx : indexesByTable.getOrDefault(table.tableName(), List.of())) {
                    catalogWriter.insertIndex(tableId, idx);
                    summaryIdx++;
                }

                processedTables++;
                progressCallback.update(schemas.size(), processedSchemas, totalTables, processedTables);
            }
            processedSchemas++;
        }
        sw.stop();

        log.info("[crawl] {}", sw.prettyPrint());
        return new CrawlSummary(summarySchemas, summaryTables, summaryCols, summaryFks, summaryIdx);
    }
}
