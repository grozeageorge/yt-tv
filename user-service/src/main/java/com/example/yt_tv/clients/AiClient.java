package com.example.yt_tv.clients;

import com.example.yt_tv.dtos.ChatResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "ai-service")
public interface AiClient {
    @PostMapping("/api/ai/chat-full")
    ChatResponseDto chatFull(@RequestParam("message") String message, @RequestParam("userId") Long userId);

    @PostMapping("/api/ai/suggest-channels")
    Map<String, List<String>> suggestChannels(@RequestParam("playlistId") Long playlistId, @RequestParam("userId") Long userId);
}
