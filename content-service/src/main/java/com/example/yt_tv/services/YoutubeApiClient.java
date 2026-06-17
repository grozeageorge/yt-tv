package com.example.yt_tv.services;

import com.example.yt_tv.dtos.ChannelCreateDto;
import com.example.yt_tv.dtos.YoutubeApiResponse;
import com.example.yt_tv.dtos.YoutubeVideoInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class YoutubeApiClient {

    @Value("${youtube.api.key}")
    private String apiKey;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public YoutubeApiClient(RestClient.Builder builder, ObjectMapper objectMapper) {
        this.restClient = builder.build();
        this.objectMapper = objectMapper;
    }

    public boolean isApiKeyConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty();
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
            log.warn("Error searching channel: {}", e.getMessage());
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
            log.warn("Failed to parse channel search response: {}", e.getMessage());
        }
        return null;
    }

    // --- 2. FETCH VIDEOS (Hybrid Strategy) ---
    public YoutubeApiResponse fetchVideos(String ytChannelId, String uploadsPlaylistId, String pageToken) {
        List<YoutubeVideoInfo> validVideos = new ArrayList<>();
        String currentPageToken = pageToken;
        String finalPlaylistId = uploadsPlaylistId;
        int safetyLoopLimit = 0;

        while (validVideos.size() < 20 && safetyLoopLimit < 5) {
            YoutubeApiResponse rawResponse;
            String playlistIdToCheck = finalPlaylistId != null ? finalPlaylistId : getUploadsPlaylistId(ytChannelId);

            if (playlistIdToCheck != null) {
                finalPlaylistId = playlistIdToCheck;
                rawResponse = fetchFromPlaylist(finalPlaylistId, currentPageToken);
            } else {
                rawResponse = fetchFromSearch(ytChannelId, currentPageToken);
            }

            if (rawResponse.videos().isEmpty()) {
                break;
            }

            List<YoutubeVideoInfo> longVideos = filterOutShorts(rawResponse.videos());
            validVideos.addAll(longVideos);
            currentPageToken = rawResponse.nextPageToken();

            if (currentPageToken == null || currentPageToken.isEmpty()) {
                break;
            }

            safetyLoopLimit++;
        }

        if (validVideos.size() > 20) {
            validVideos = validVideos.subList(0, 20);
        }

        return new YoutubeApiResponse(validVideos, currentPageToken, finalPlaylistId);
    }

    public List<String> getChannelTopics(String channelId) {
        try {
            String response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https").host("www.googleapis.com")
                            .path("/youtube/v3/channels")
                            .queryParam("key", apiKey)
                            .queryParam("id", channelId)
                            .queryParam("part", "topicDetails")
                            .build())
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            List<String> topics = new ArrayList<>();

            if (root.path("items").isArray() && !root.path("items").isEmpty()) {
                JsonNode categories = root.path("items").get(0).path("topicDetails").path("topicCategories");
                if (categories.isArray()) {
                    for (JsonNode category : categories) {
                        String url = category.asText();
                        String name = url.substring(url.lastIndexOf("/") + 1).replace("_", " ");
                        topics.add(name);
                    }
                }
            }
            return topics;
        } catch (Exception e) {
            log.warn("Failed to load channel topics: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public String getChannelDescription(String channelId) {
        try {
            String response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https").host("www.googleapis.com")
                            .path("/youtube/v3/channels")
                            .queryParam("key", apiKey)
                            .queryParam("id", channelId)
                            .queryParam("part", "snippet")
                            .build())
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            if (root.path("items").isArray() && !root.path("items").isEmpty()) {
                return root.path("items").get(0).path("snippet").path("description").asText("");
            }
        } catch (Exception e) {
            log.warn("Failed to load channel description: {}", e.getMessage());
        }
        return "";
    }

    private List<YoutubeVideoInfo> filterOutShorts(List<YoutubeVideoInfo> videos) {
        if (videos.isEmpty()) return videos;

        try {
            String videoIds = videos.stream()
                    .map(YoutubeVideoInfo::videoId)
                    .collect(Collectors.joining(","));

            String response = restClient.get().uri(uriBuilder -> uriBuilder
                    .scheme("https").host("www.googleapis.com")
                    .path("/youtube/v3/videos")
                    .queryParam("key", apiKey)
                    .queryParam("id", videoIds)
                    .queryParam("part", "contentDetails")
                    .build()).retrieve().body(String.class);

            JsonNode root = objectMapper.readTree(response);
            List<String> validIds = new ArrayList<>();

            for (JsonNode item : root.path("items")) {
                String durationStr = item.path("contentDetails").path("duration").asText();
                try {
                    Duration duration = Duration.parse(durationStr);
                    if (duration.getSeconds() > 120) {
                        validIds.add(item.path("id").asText());
                    }
                } catch (Exception ignored) {
                    validIds.add(item.path("id").asText());
                }
            }

            return videos.stream()
                    .filter(v -> validIds.contains(v.videoId()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Error filtering out shorts: {}", e.getMessage());
            return videos;
        }
    }

    // STRATEGY A: Get videos via "Uploads" playlist
    private YoutubeApiResponse fetchFromPlaylist(String playlistId, String pageToken) {
        try {
            var uriSpec = restClient.get().uri(uriBuilder -> {
                uriBuilder.scheme("https").host("www.googleapis.com")
                        .path("/youtube/v3/playlistItems")
                        .queryParam("key", apiKey)
                        .queryParam("playlistId", playlistId)
                        .queryParam("part", "snippet")
                        .queryParam("maxResults", "20");

                if (pageToken != null)
                    uriBuilder.queryParam("pageToken", pageToken);

                return uriBuilder.build();
            });

            String response = uriSpec.retrieve().body(String.class);
            return parseResponse(response, true, playlistId);
        } catch (Exception e) {
            return new YoutubeApiResponse(new ArrayList<>(), null, playlistId);
        }
    }

    // STRATEGY SEARCH

    private YoutubeApiResponse fetchFromSearch(String channelId, String pageToken) {
        try{
            var uniSpec = restClient.get().uri(uriBuilder -> {
                uriBuilder.scheme("https").host("www.googleapis.com")
                        .path("/youtube/v3/search")
                        .queryParam("key", apiKey)
                        .queryParam("channelId", channelId)
                        .queryParam("part", "snippet,id")
                        .queryParam("order", "date")
                        .queryParam("maxResults", "20")
                        .queryParam("type", "video;");
                if (pageToken != null)
                    uriBuilder.queryParam("pageToken", pageToken);

                return uriBuilder.build();
            });

            String response = uniSpec.retrieve().body(String.class);
            return parseResponse(response, false, null);
        } catch (Exception e) {
            log.warn("Search fetch failed: {}", e.getMessage());
            return new YoutubeApiResponse(new ArrayList<>(), null, null);
        }
    }

    // --- PARSER ---

    private YoutubeApiResponse parseResponse(String json, boolean isPlaylist, String playlistId) {
        List<YoutubeVideoInfo> videos = new ArrayList<>();
        String nextToken = null;

        try{
            JsonNode root = objectMapper.readTree(json);
            if (root.has("nextPageToken")) {
                nextToken = root.get("nextPageToken").asText();
            }

            JsonNode items = root.path("items");
            if (items.isArray()) {
                for (JsonNode item : items) {
                    JsonNode snippet = item.path("snippet");
                    String videoId = isPlaylist
                            ? snippet.path("resourceId").path("videoId").asText()
                            : item.path("id").path("videoId").asText();

                    String title = snippet.path("title").asText();
                    String thumb = snippet.path("thumbnails").path("medium").path("url").asText();

                    if (videoId != null && !videoId.isEmpty()) {
                        videos.add(new YoutubeVideoInfo(videoId, title, thumb));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse videos response: {}", e.getMessage());
        }

        return new YoutubeApiResponse(videos, nextToken, playlistId);
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

            if (items.isArray() && !items.isEmpty()) {
                return items.get(0)
                        .path("contentDetails")
                        .path("relatedPlaylists")
                        .path("uploads")
                        .asText();
            }
        } catch (Exception e) {
            log.warn("Failed to resolve uploads playlist ID: {}", e.getMessage());
        }
        return null;
    }
}
