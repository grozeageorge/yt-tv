package com.example.yt_tv.tools;

import com.example.yt_tv.clients.PlaylistClient;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class VideoSearchFunction implements Function<VideoSearchRequest, VideoSearchResponse> {
    private final VectorStore vectorStore;
    private final PlaylistClient playlistClient;
    private final Long userId;
    @Setter
    private Consumer<List<String>> videoIdsListener;

    public VideoSearchFunction(VectorStore vectorStore, PlaylistClient playlistClient, Long userId) {
        this.vectorStore = vectorStore;
        this.playlistClient = playlistClient;
        this.userId = userId;
    }

    @Override
    public VideoSearchResponse apply(VideoSearchRequest request) {
        String query = request.query();
        String category = request.categoryFilter();
        String channelFilter = null;
        try {
            channelFilter = request.channelFilter();
        } catch (Throwable ignored) {
            // backwards compatibility if request class doesn't have channelFilter
        }

        if (category == null || category.isBlank() || "null".equalsIgnoreCase(category)) {
            category = inferCategory(query);
        }

        if (query == null || query.isBlank()) {
            query = category != null ? category : "";
        }

        log.debug("AI agent search query={}, category={}", query, category);

        // Search Logic
        List<Document> documents = new ArrayList<>();

        try {
            var searchBuilder = SearchRequest.builder()
                    .query(query)
                    .topK(50); // Get a good pool

            boolean hasCategory = category != null && !category.equalsIgnoreCase("null") && !category.isEmpty();
            boolean hasChannel = channelFilter != null && !channelFilter.equalsIgnoreCase("null") && !channelFilter.isBlank();

            String categoryExpr = null;
            String channelExpr = null;

            if (hasCategory) {
                // Use normalizedCategory metadata for strict comparisons (case/punct-insensitive)
                String normalizedCategory = NormalizationUtils.normalizeCategory(category);
                categoryExpr = "normalizedCategory == '" + normalizedCategory + "'";
            }

            if (hasChannel) {
                String normalizedChannel = NormalizationUtils.normalizeChannel(channelFilter);
                channelExpr = "normalizedChannel == '" + normalizedChannel + "'";
            }

            // If either channel or category was provided, build a strict filter expression.
            if (categoryExpr != null || channelExpr != null) {
                String expr;
                if (categoryExpr != null && channelExpr != null) {
                    // match either the category OR the channel (consistent with parser behavior)
                    expr = "(" + categoryExpr + ") || (" + channelExpr + ")";
                } else if (categoryExpr != null) {
                    expr = categoryExpr;
                } else {
                    expr = channelExpr;
                }
                searchBuilder.filterExpression(expr);
            }

            documents = vectorStore.similaritySearch(searchBuilder.build());

            if (documents.isEmpty() && hasCategory) {
                // If no results found using normalizedCategory, try a fallback to the
                // legacy 'category' metadata exact match (helps if older documents
                // were inserted without normalizedCategory). Do NOT fall back to an
                // unfiltered search — returning unrelated categories is worse than
                // returning no results for a specific category request.
                try {
                    documents = vectorStore.similaritySearch(
                            SearchRequest.builder()
                                    .query(query)
                                    .topK(50)
                                    .filterExpression("category == '" + category + "'")
                                    .build()
                    );
                } catch (Exception ignore) {
                    // ignore and fallthrough to empty results
                }
            }

        } catch (Exception e) {
            log.warn("AI search failed: {}", e.getMessage());
        }

        if (documents.isEmpty()) {
            return new VideoSearchResponse(List.of(), "No videos found.");
        }

        // Filter watched and shuffle
        List<Document> shuffledDocs = new ArrayList<>(documents);
        Collections.shuffle(shuffledDocs);

        List<String> watchedYtIds = playlistClient.getWatchedYtVideoIds(userId);

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

        return new VideoSearchResponse(videoIds, "Found " + videoIds.size() + " videos including: " + context);
    }

    private String inferCategory(String query) {
        if (query == null) return null;
        String q = query.toLowerCase();

        if (q.contains("k-pop") || q.contains("kpop")) return "K-Pop";
        if (q.contains("hip hop") || q.contains("hip-hop")) return "Hip-Hop";
        if (q.contains("pop")) return "Pop Music";
        if (q.contains("rock")) return "Rock";
        if (q.contains("electronic")) return "Electronic";
        if (q.contains("music")) return "Music (General)";
        if (q.contains("science")) return "Science";
        if (q.contains("tech") || q.contains("technology")) return "Tech";
        if (q.contains("gaming") || q.contains("game")) return "Gaming";
        if (q.contains("cook") || q.contains("recipe")) return "Cooking";
        if (q.contains("vlog")) return "Vlog";
        if (q.contains("news")) return "News";
        if (q.contains("education") || q.contains("learn")) return "Education";
        if (q.contains("sport")) return "Sports";
        if (q.contains("movie") || q.contains("film")) return "Movies";
        if (q.contains("comedy") || q.contains("funny")) return "Comedy";

        return null;
    }
}
