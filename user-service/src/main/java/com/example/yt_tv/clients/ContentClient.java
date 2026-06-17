package com.example.yt_tv.clients;

import com.example.yt_tv.dtos.ChannelDto;
import com.example.yt_tv.dtos.VideoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "content-service")
public interface ContentClient {
    @GetMapping("/api/channels")
    List<ChannelDto> listChannels();

    @GetMapping("/api/channels/{id}")
    ChannelDto getChannel(@PathVariable("id") Long id);

    @PostMapping("/api/channels/sync/{id}")
    void syncChannel(@PathVariable("id") Long id);

    @PostMapping("/api/channels/create-from-query")
    ChannelDto createChannelFromQuery(@RequestParam("query") String query);

    @DeleteMapping("/api/channels/{id}")
    void deleteChannel(@PathVariable("id") Long id);

    @GetMapping("/api/videos/channel/{channelId}")
    List<VideoDto> listVideosByChannel(@PathVariable("channelId") Long channelId);
    
    @GetMapping("/api/videos/channel/{channelId}/random")
    VideoDto getRandomVideoFromChannel(@PathVariable("channelId") Long channelId);

    @GetMapping("/api/videos/channel/{channelId}/random-batch")
    List<VideoDto> getRandomBatchFromChannel(@PathVariable("channelId") Long channelId, @RequestParam("limit") int limit);
}
