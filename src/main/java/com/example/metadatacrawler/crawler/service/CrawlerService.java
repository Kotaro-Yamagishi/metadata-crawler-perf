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

            // Step 0: 同じ schema のテーブル一覧を再度取得
            List<TableMetadata> tables = sourceClient.listTables(schemaName);
            for (TableMetadata table : tables) {
                long tableId = catalogWriter.insertTable(schemaId, table);
                summaryTables++;

                // Step 0: テーブル単位の N+1 クエリ
                for (ColumnMetadata col : sourceClient.listColumns(schemaName, table.tableName())) {
                    catalogWriter.insertColumn(tableId, col);
                    summaryCols++;
                }
                for (ForeignKeyMetadata fk : sourceClient.listForeignKeys(schemaName, table.tableName())) {
                    catalogWriter.insertForeignKey(tableId, fk);
                    summaryFks++;
                }
                for (IndexMetadata idx : sourceClient.listIndexes(schemaName, table.tableName())) {
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
