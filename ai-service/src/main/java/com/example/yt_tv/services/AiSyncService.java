package com.example.yt_tv.services;

import com.example.yt_tv.dtos.YoutubeVideoInfo;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import com.example.yt_tv.tools.NormalizationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiSyncService {

    private final VectorStore vectorStore;

    public void addVideosToVectorDb(List<YoutubeVideoInfo> videos, String channelName, String category) {
        log.debug("AI ingestion received {} videos for channel={}, category={}", videos.size(), channelName, category);
        List<Document> documents = new ArrayList<>();

        for (YoutubeVideoInfo video : videos) {
            Document doc = getDocument(channelName, category, video);
            documents.add(doc);
        }

        if (!documents.isEmpty()) {
            try {
                vectorStore.add(documents);
                log.info("AI ingestion updated {} videos in ChromaDB", documents.size());
            } catch (Exception e) {
                log.warn("AI ingestion failed: {}", e.getMessage());
            }
        }
    }

    private static @NonNull Document getDocument(String channelName, String category, YoutubeVideoInfo video) {
        String title = video.title() != null ? video.title() : "Unknown Title";
        String id = video.videoId() != null ? video.videoId() : "Unknown Id";
        String safeCategory = category != null ? category : "Uncategorized";

        String contentToEmbed = String.format("Channel: %s. Category: %s. Title: %s", channelName, safeCategory, title);

        return new Document(
                id,
                contentToEmbed,
                Map.of(
                        "videoId", id,
                        "channel", channelName,
                        "category", safeCategory,
                        // normalizedCategory stores a normalized, punctuation-free, lower-case
                        // form used for robust, case-insensitive and punctuation-insensitive
                        // filtering in vector store queries
                        "normalizedCategory", NormalizationUtils.normalizeCategory(safeCategory),
                        // Store a normalized channel value so searches can strictly filter
                        // by channel in a case/punctuation-insensitive way.
                        "normalizedChannel", NormalizationUtils.normalizeChannel(channelName)
                )
        );
    }
}