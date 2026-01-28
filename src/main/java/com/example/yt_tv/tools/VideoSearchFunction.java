package com.example.yt_tv.tools;

import com.example.yt_tv.entities.WatchedVideo;
import com.example.yt_tv.repositories.WatchedVideoRepository;
import lombok.Setter;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

// Agentic Tool
public class VideoSearchFunction implements Function<VideoSearchRequest, VideoSearchResponse> {
    private final VectorStore vectorStore;
    private final WatchedVideoRepository watchedVideoRepository;
    private final Long userId;
    @Setter
    private Consumer<List<String>> videoIdsListener;

    public VideoSearchFunction(VectorStore vectorStore, WatchedVideoRepository watchedVideoRepository, Long userId) {
        this.vectorStore = vectorStore;
        this.watchedVideoRepository = watchedVideoRepository;
        this.userId = userId;
    }

    @Override
    public VideoSearchResponse apply(VideoSearchRequest request) {
        String query = request.query();
        String category = request.categoryFilter();

        System.out.println("AI AGENT is calling VideoSearchFunction with query: " + query + " and category: " + category);

        // Search Logic
        List<Document> documents = new ArrayList<>();

        try {
            var searchBuilder = SearchRequest.builder()
                    .query(query)
                    .topK(50); // Get a good pool

            // 1. APPLY STRICT FILTER IF AI REQUESTED IT
            if (category != null && !category.equalsIgnoreCase("null") && !category.isEmpty()) {
                // We use 'contains' logic for flexibility (e.g. 'Music' matches 'Pop Music')
                // But generally, exact match is safer.
                searchBuilder.filterExpression("category == '" + category + "'");
            }

            documents = vectorStore.similaritySearch(searchBuilder.build());

        } catch (Exception e) {
            System.out.println("AI Debug: Search failed: " + e.getMessage());
        }

        if (documents.isEmpty()) {
            return new VideoSearchResponse(List.of(), "No videos found.");
        }

        // Filter watched and shuffle
        List<Document> shuffledDocs = new ArrayList<>(documents);
        Collections.shuffle(shuffledDocs);

        List<String> watchedYtIds = watchedVideoRepository.findByUserId(userId).stream()
                .filter(WatchedVideo::isWatched)
                .map(wv -> wv.getVideo().getYtVideoId())
                .toList();

        List<String> videoIds = shuffledDocs.stream()
                .map(d -> (String) d.getMetadata().get("videoId"))
                .filter(id -> !watchedYtIds.contains(id))
                .distinct()
                .limit(20)
                .toList();

        if (this.videoIdsListener != null) {
            this.videoIdsListener.accept(videoIds);
        }

        // Prepare text context for AI
        String context = shuffledDocs.stream()
                .limit(3)
                .map(Document::getText)
                .map(Object::toString)
                .collect(Collectors.joining(", "));

        return new VideoSearchResponse(videoIds, "Found " + videoIds.size() + "videos including: " + context);
    }
}
