package com.example.yt_tv.services;

import com.example.yt_tv.dtos.YoutubeVideoInfo;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiSyncService {

    private final VectorStore vectorStore;

    public void addVideosToVectorDb(List<YoutubeVideoInfo> videos, String channelName, String category) {
        System.out.println("AI DEBUG: Received " + videos.size() + " videos for ingestion. Category: " + category);
        List<Document> documents = new ArrayList<>();

        for (YoutubeVideoInfo video : videos) {
            Document doc = getDocument(channelName, category, video);
            documents.add(doc);
        }

        if (!documents.isEmpty()) {
            try {
                vectorStore.add(documents);
                System.out.println("AI SUCCESS: Ingested/Updated " + documents.size() + " videos in ChromaDB.");
            } catch (Exception e) {
                System.err.println("AI ERROR: " + e.getMessage());
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
                        "category", safeCategory
                )
        );
    }
}