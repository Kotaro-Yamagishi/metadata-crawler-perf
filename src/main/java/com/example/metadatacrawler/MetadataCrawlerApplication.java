package com.example.metadatacrawler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class MetadataCrawlerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MetadataCrawlerApplication.class, args);
    }
}
