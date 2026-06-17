package com.example.metadatacrawler.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

@Configuration
public class CatalogSchemaInitializer {

    @Bean
    public DataSourceInitializer catalogDataSourceInitializer(
            @Qualifier("catalogDataSource") DataSource catalogDataSource) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("db/catalog/schema.sql"));
        populator.setIgnoreFailedDrops(true);

        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(catalogDataSource);
        initializer.setDatabasePopulator(populator);
        return initializer;
    }
}
