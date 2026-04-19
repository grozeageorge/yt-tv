package com.example.yt_tv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.example.yt_tv.repositories")
@EntityScan("com.example.yt_tv.entities")
public class YtTvApplication {
    public static void main(String[] args) {
        SpringApplication.run(YtTvApplication.class, args);
    }
}