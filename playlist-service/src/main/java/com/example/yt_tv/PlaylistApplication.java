package com.example.yt_tv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class PlaylistApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlaylistApplication.class, args);
    }
}
