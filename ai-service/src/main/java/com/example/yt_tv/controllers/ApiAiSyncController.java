package com.example.yt_tv.controllers;

import com.example.yt_tv.dtos.YoutubeVideoInfo;
import com.example.yt_tv.services.AiSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai/sync")
@RequiredArgsConstructor
public class ApiAiSyncController {
    private final AiSyncService aiSyncService;

    @PostMapping("/videos")
    public void addVideos(@RequestParam String channelName, @RequestParam String category, @RequestBody List<YoutubeVideoInfo> videos) {
        aiSyncService.addVideosToVectorDb(videos, channelName, category);
    }
}
