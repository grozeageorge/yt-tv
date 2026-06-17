package com.example.yt_tv.controllers;

import com.example.yt_tv.clients.PlaylistClient;
import com.example.yt_tv.dtos.ChatResponseDto;
import com.example.yt_tv.dtos.ChannelCategorizeRequest;
import com.example.yt_tv.dtos.ChannelCategorizeResponse;
import com.example.yt_tv.dtos.PlaylistChannelDto;
import com.example.yt_tv.dtos.PlaylistDto;
import com.example.yt_tv.services.AiChannelService;
import com.example.yt_tv.services.AiChatService;
import com.example.yt_tv.services.AiCategorizerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {
    private final AiChatService aiChatService;
    private final AiChannelService aiChannelService;
    private final PlaylistClient playlistClient;
    private final AiCategorizerService aiCategorizerService;

    // Full RAG Chat (Returns JSON with Video IDs)
    @PostMapping("/chat-full")
    public ChatResponseDto chatFull(@RequestParam String message, @RequestParam Long userId) {
        return aiChatService.chatWithData(message, userId);
    }

    @PostMapping("/suggest-channels")
    public Map<String, List<String>> suggestChannels(@RequestParam Long playlistId, @RequestParam Long userId) {
        // Verify playlist ownership via playlist-service
        PlaylistDto playlist = playlistClient.getPlaylist(playlistId, userId);

        List<String> existingNames = playlist.getChannels().stream()
                .map(c -> c.getChannelName().toLowerCase())
                .toList();

        List<String> currentChannelNames = playlist.getChannels().stream()
                .map(PlaylistChannelDto::getChannelName)
                .toList();

        String aiResponse = aiChannelService.suggestSimilarChannels(currentChannelNames, playlist.getName());

        if (aiResponse == null || aiResponse.isBlank()) {
            return Map.of("suggestions", List.of());
        }

        if (aiResponse.toUpperCase().contains("NONE") || aiResponse.toLowerCase().contains("no channel")) {
            return Map.of("suggestions", List.of()); // Return empty list
        }

        List<String> suggestions = List.of(aiResponse.split(","));
        List<String> cleanSuggestions = suggestions.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .filter(s -> !existingNames.contains(s.toLowerCase()))
                .toList();

        return Map.of("suggestions", cleanSuggestions);
    }

    // Categorize a channel using LLM
    @PostMapping("/categorize-channel")
    public ChannelCategorizeResponse categorizeChannel(@RequestBody ChannelCategorizeRequest request) {
        if (request == null || request.getChannelName() == null || request.getChannelName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "channelName is required");
        }

        // Ensure hints include description if provided
        List<String> hints = request.getYoutubeHints() != null ? request.getYoutubeHints() : List.of();
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            hints = new java.util.ArrayList<>(hints);
            hints.add(request.getDescription());
        }

        String category = aiCategorizerService.categorizeChannel(
                request.getChannelName(),
                request.getSampleVideos() != null ? request.getSampleVideos() : List.of(),
                hints
        );

        return ChannelCategorizeResponse.builder().category(category).build();
    }
}
