package com.example.yt_tv.controllers;

import com.example.yt_tv.dtos.VideoDto;
import com.example.yt_tv.services.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class ApiVideoController {
    private final VideoService videoService;

    @GetMapping("/channel/{channelId}/random")
    public VideoDto getRandomFromChannel(@PathVariable Long channelId) {
        return videoService.getRandomVideoFromChannel(channelId);
    }

    @GetMapping("/channel/{channelId}/random-batch")
    public List<VideoDto> getRandomBatchFromChannel(@PathVariable Long channelId, @RequestParam(defaultValue = "20") int limit) {
        return videoService.getRandomVideosFromChannel(channelId, limit);
    }

    @GetMapping("/channel/{channelId}")
    public List<VideoDto> listByChannel(@PathVariable Long channelId) {
        return videoService.listByChannel(channelId);
    }

    @GetMapping("/{id}")
    public VideoDto get(@PathVariable Long id) {
        return videoService.get(id);
    }
}
