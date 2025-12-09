package com.example.yt_tv.services;

import com.example.yt_tv.dtos.ChannelCreateDto;
import com.example.yt_tv.dtos.YoutubeVideoInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Service
public class YoutubeApiClient {

    @Value("${youtube.api.key}")
    private String apiKey;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public YoutubeApiClient(RestClient.Builder builder, ObjectMapper objectMapper) {
        this.restClient = builder.build();
        this.objectMapper = objectMapper;
    }

    // --- 1. SEARCH CHANNEL ---
    public ChannelCreateDto searchChannel(String query) {
        try {
            String response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("www.googleapis.com")
                            .path("/youtube/v3/search")
                            .queryParam("key", apiKey.trim())
                            .queryParam("q", query.trim())
                            .queryParam("part", "snippet")
                            .queryParam("maxResults", "5")
                            .build())
                    .retrieve()
                    .body(String.class);

            return parseChannelSearch(response);
        } catch (Exception e) {
            System.err.println("Error searching channel: " + e.getMessage());
            return null;
        }
    }

    private ChannelCreateDto parseChannelSearch(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode items = root.path("items");
            if (items.isArray()) {
                for (JsonNode item : items) {
                    String kind = item.path("id").path("kind").asText();
                    if ("youtube#channel".equals(kind)) {
                        String channelId = item.path("id").path("channelId").asText();
                        String title = item.path("snippet").path("title").asText();
                        String thumb = item.path("snippet").path("thumbnails").path("medium").path("url").asText();

                        return ChannelCreateDto.builder()
                                .ytChannelId(channelId)
                                .name(title)
                                .thumbnailUrl(thumb)
                                .build();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // --- 2. FETCH VIDEOS (Hybrid Strategy) ---
    public List<YoutubeVideoInfo> fetchLatestVideos(String ytChannelId) {
        // Strategy A: Try to get the "Uploads" playlist (Best for Creators)
        List<YoutubeVideoInfo> videos = fetchFromUploadsPlaylist(ytChannelId);

        // Strategy B: If that failed (returns empty), try Search (Best for Topic Channels)
        if (videos.isEmpty()) {
            System.out.println("Playlist method failed or empty. Falling back to Search method for: " + ytChannelId);
            videos = fetchFromSearchFallback(ytChannelId);
        }

        return videos;
    }

    // STRATEGY A: Get videos via "Uploads" playlist
    private List<YoutubeVideoInfo> fetchFromUploadsPlaylist(String channelId) {
        // 1. Get the Playlist ID using a helper method (This keeps the variable final)
        String uploadsPlaylistId = getUploadsPlaylistId(channelId);

        if (uploadsPlaylistId == null) {
            return new ArrayList<>();
        }

        // 2. Now 'uploadsPlaylistId' is effectively final, so we can use it in the lambda
        try {
            String playlistResponse = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("www.googleapis.com")
                            .path("/youtube/v3/playlistItems")
                            .queryParam("key", apiKey.trim())
                            .queryParam("playlistId", uploadsPlaylistId)
                            .queryParam("part", "snippet")
                            .queryParam("maxResults", "20")
                            .build())
                    .retrieve()
                    .body(String.class);

            return parseVideoList(playlistResponse, true); // true = parsing playlistItems
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // Helper to get the ID (keeps the code clean)
    private String getUploadsPlaylistId(String channelId) {
        try {
            String response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("www.googleapis.com")
                            .path("/youtube/v3/channels")
                            .queryParam("key", apiKey.trim())
                            .queryParam("id", channelId.trim())
                            .queryParam("part", "contentDetails")
                            .build())
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode items = root.path("items");

            if (items.isArray() && items.size() > 0) {
                return items.get(0)
                        .path("contentDetails")
                        .path("relatedPlaylists")
                        .path("uploads")
                        .asText();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // STRATEGY B: Get videos via Search (Fallback)
    private List<YoutubeVideoInfo> fetchFromSearchFallback(String channelId) {
        try {
            String response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("www.googleapis.com")
                            .path("/youtube/v3/search")
                            .queryParam("key", apiKey.trim())
                            .queryParam("channelId", channelId.trim())
                            .queryParam("part", "snippet,id")
                            .queryParam("order", "date")
                            .queryParam("maxResults", "20")
                            .queryParam("type", "video")
                            .build())
                    .retrieve()
                    .body(String.class);

            return parseVideoList(response, false); // false = parsing search results
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // Universal Parser
    private List<YoutubeVideoInfo> parseVideoList(String jsonResponse, boolean isPlaylist) {
        List<YoutubeVideoInfo> videos = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode items = root.path("items");

            if (items.isArray()) {
                for (JsonNode item : items) {
                    JsonNode snippet = item.path("snippet");
                    String videoId;

                    if (isPlaylist) {
                        videoId = snippet.path("resourceId").path("videoId").asText();
                    } else {
                        videoId = item.path("id").path("videoId").asText();
                    }

                    String title = snippet.path("title").asText();
                    String thumb = snippet.path("thumbnails").path("medium").path("url").asText();

                    if (videoId != null && !videoId.isEmpty()) {
                        videos.add(new YoutubeVideoInfo(videoId, title, thumb));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return videos;
    }
}