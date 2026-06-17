package com.example.yt_tv.clients;

import com.example.yt_tv.dtos.YoutubeVideoInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "ai-service")
public interface AiSyncClient {
    @PostMapping("/api/ai/sync/videos")
    void addVideos(@RequestParam("channelName") String channelName, 
                   @RequestParam("category") String category, 
                   @RequestBody List<YoutubeVideoInfo> videos);
}
