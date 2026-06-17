package com.example.metadatacrawler.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("app.source.datasource")
    public DataSourceProperties sourceDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public DataSource sourceDataSource(
            @Qualifier("sourceDataSourceProperties") DataSourceProperties properties) {
        HikariDataSource ds = properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        // Step 0: 意図的に小さいプール
        ds.setMaximumPoolSize(2);
        ds.setMinimumIdle(1);
        ds.setPoolName("source-pool");
        return ds;
    }

    @Bean
    @Primary
    public JdbcTemplate sourceJdbcTemplate(@Qualifier("sourceDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }

    @Bean
    @ConfigurationProperties("app.catalog.datasource")
    public DataSourceProperties catalogDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource catalogDataSource(
            @Qualifier("catalogDataSourceProperties") DataSourceProperties properties) {
        HikariDataSource ds = properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        // Step 0: 意図的に小さいプール
        ds.setMaximumPoolSize(2);
        ds.setMinimumIdle(1);
        ds.setPoolName("catalog-pool");
        return ds;
    }

    @Bean
    public JdbcTemplate catalogJdbcTemplate(@Qualifier("catalogDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }

    @Bean
    public PlatformTransactionManager catalogTransactionManager(
            @Qualifier("catalogDataSource") DataSource ds) {
        return new DataSourceTransactionManager(ds);
    }
}
