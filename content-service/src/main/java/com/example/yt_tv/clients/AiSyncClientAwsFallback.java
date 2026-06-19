package com.example.yt_tv.clients;

import com.example.yt_tv.dtos.YoutubeVideoInfo;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * No-op fallback for the AiSyncClient when running with the 'aws' profile.
 */
@Component
@Profile("aws")
public class AiSyncClientAwsFallback implements AiSyncClient {
    @Override
    public void addVideos(String channelName, String category, List<YoutubeVideoInfo> videos) {
        // Intentionally no-op on AWS profile
    }
}

