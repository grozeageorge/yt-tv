package com.example.yt_tv.controllers;

import com.example.yt_tv.dtos.ChatResponseDto;
import com.example.yt_tv.dtos.PlaylistChannelDto;
import com.example.yt_tv.dtos.PlaylistDto;
import com.example.yt_tv.entities.User;
import com.example.yt_tv.repositories.UserRepository;
import com.example.yt_tv.services.AiChannelService;
import com.example.yt_tv.services.AiChatService;
import com.example.yt_tv.services.PlaylistService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AiController {
    private final AiChatService aiChatService;
    private final UserRepository userRepository;
    private final AiChannelService aiChannelService;
    private final PlaylistService playlistService;

    // Full RAG Chat (Returns JSON with Video IDs)
    @PostMapping("/api/ai/chat-full")
    public ChatResponseDto chatFull(@RequestParam String message, Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return aiChatService.chatWithData(message, user.getId());
    }

    @PostMapping("/api/ai/suggest-channels")
    public Map<String, List<String>> suggestChannels(@RequestParam Long playlistId) {
        PlaylistDto playlist = playlistService.getPlaylist(playlistId);

        List<String> existingNames = playlist.getChannels().stream()
                .map(c -> c.getChannelName().toLowerCase())
                .toList();

        List<String> currentChannelNames = playlist.getChannels().stream()
                .map(PlaylistChannelDto::getChannelName)
                .toList();

        String aiResponse = aiChannelService.suggestSimilarChannels(currentChannelNames, playlist.getName());

        if (aiResponse.toUpperCase().contains("NONE")) {
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
}
