DROP TABLE IF EXISTS catalog_columns;
DROP TABLE IF EXISTS catalog_foreign_keys;
DROP TABLE IF EXISTS catalog_indexes;
DROP TABLE IF EXISTS catalog_tables;
DROP TABLE IF EXISTS catalog_schemas;
DROP TABLE IF EXISTS crawl_jobs;

CREATE TABLE catalog_schemas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    schema_name VARCHAR(255) NOT NULL,
    crawl_id VARCHAR(36) NOT NULL,
    crawled_at TIMESTAMP NOT NULL,
    UNIQUE (schema_name, crawl_id)
);

CREATE TABLE catalog_tables (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    schema_id BIGINT NOT NULL,
    table_name VARCHAR(255) NOT NULL,
    table_comment VARCHAR(1024),
    FOREIGN KEY (schema_id) REFERENCES catalog_schemas(id)
);

CREATE TABLE catalog_columns (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    table_id BIGINT NOT NULL,
    column_name VARCHAR(255) NOT NULL,
    data_type VARCHAR(64) NOT NULL,
    is_nullable BOOLEAN NOT NULL,
    column_comment VARCHAR(1024),
    ordinal_position INT NOT NULL,
    FOREIGN KEY (table_id) REFERENCES catalog_tables(id)
);

CREATE TABLE catalog_foreign_keys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_table_id BIGINT NOT NULL,
    from_column VARCHAR(255) NOT NULL,
    to_schema VARCHAR(255) NOT NULL,
    to_table VARCHAR(255) NOT NULL,
    to_column VARCHAR(255) NOT NULL,
    constraint_name VARCHAR(255),
    FOREIGN KEY (from_table_id) REFERENCES catalog_tables(id)
);

CREATE TABLE catalog_indexes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    table_id BIGINT NOT NULL,
    index_name VARCHAR(255) NOT NULL,
    is_unique BOOLEAN NOT NULL,
    column_list VARCHAR(1024) NOT NULL,
    FOREIGN KEY (table_id) REFERENCES catalog_tables(id)
);

CREATE TABLE crawl_jobs (
    id VARCHAR(36) PRIMARY KEY,
    status VARCHAR(16) NOT NULL,
    target_schemas TEXT,
    callback_url VARCHAR(512),
    progress TEXT,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    error_message TEXT
);

CREATE INDEX idx_tables_schema ON catalog_tables(schema_id);
CREATE INDEX idx_columns_table ON catalog_columns(table_id);
CREATE INDEX idx_fk_from_table ON catalog_foreign_keys(from_table_id);
CREATE INDEX idx_indexes_table ON catalog_indexes(table_id);
CREATE INDEX idx_jobs_status ON crawl_jobs(status);
