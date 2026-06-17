package com.example.metadatacrawler.catalog.repository;

import com.example.metadatacrawler.crawler.domain.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
public class CrawlJobRepository {

    private final JdbcTemplate catalogJdbc;
    private final ObjectMapper objectMapper;

    public CrawlJobRepository(@Qualifier("catalogJdbcTemplate") JdbcTemplate catalogJdbc, ObjectMapper objectMapper) {
        this.catalogJdbc = catalogJdbc;
        this.objectMapper = objectMapper;
    }

    public void save(CrawlJob job) {
        catalogJdbc.update(
            "INSERT INTO crawl_jobs (id, status, target_schemas, callback_url, progress, started_at, completed_at, error_message) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            job.id(),
            job.status().name(),
            toJson(job.targetSchemas()),
            job.callbackUrl(),
            toJson(job.progress()),
            Timestamp.from(job.startedAt()),
            job.completedAt() != null ? Timestamp.from(job.completedAt()) : null,
            job.errorMessage()
        );
    }

    public void updateStatus(String id, CrawlStatus status) {
        catalogJdbc.update("UPDATE crawl_jobs SET status = ? WHERE id = ?", status.name(), id);
    }

    public void updateProgress(String id, CrawlProgress progress) {
        catalogJdbc.update(
            "UPDATE crawl_jobs SET progress = ? WHERE id = ?",
            toJson(progress), id
        );
    }

    public void complete(String id, Instant completedAt) {
        catalogJdbc.update(
            "UPDATE crawl_jobs SET status = ?, completed_at = ? WHERE id = ?",
            CrawlStatus.COMPLETED.name(), Timestamp.from(completedAt), id
        );
    }

    public void fail(String id, String errorMessage, Instant completedAt) {
        catalogJdbc.update(
            "UPDATE crawl_jobs SET status = ?, completed_at = ?, error_message = ? WHERE id = ?",
            CrawlStatus.FAILED.name(), Timestamp.from(completedAt), errorMessage, id
        );
    }

    public CrawlJob findById(String id) {
        try {
            return catalogJdbc.queryForObject(
                "SELECT id, status, target_schemas, callback_url, progress, started_at, completed_at, error_message " +
                "FROM crawl_jobs WHERE id = ?",
                (rs, i) -> new CrawlJob(
                    rs.getString("id"),
                    CrawlStatus.valueOf(rs.getString("status")),
                    fromJson(rs.getString("target_schemas"), new TypeReference<List<String>>(){}),
                    rs.getString("callback_url"),
                    fromJson(rs.getString("progress"), new TypeReference<CrawlProgress>(){}),
                    rs.getTimestamp("started_at").toInstant(),
                    rs.getTimestamp("completed_at") != null ? rs.getTimestamp("completed_at").toInstant() : null,
                    rs.getString("error_message")
                ),
                id
            );
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T fromJson(String json, TypeReference<T> typeRef) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
