package com.example.yt_tv.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Profile("!aws")
@Slf4j
public class AiChannelService {
    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public AiChannelService(ChatModel chatModel, VectorStore vectorStore) {
        this.chatClient = ChatClient.create(chatModel);
        this.vectorStore = vectorStore;
    }

    public String suggestSimilarChannels(List<String> currentChannels, String playlistName) {
        Set<String> identifiedCategories = new HashSet<>();
        Set<String> communityChannels = new HashSet<>();

        // 1. IDENTIFY CATEGORIES
        if (!currentChannels.isEmpty()) {
            for (String channel : currentChannels) {
                try {
                    List<Document> docs = vectorStore.similaritySearch(
                            SearchRequest.builder()
                                    .query(channel)
                                    .topK(1)
                                    .filterExpression("channel == '" + channel + "'")
                                    .build()
                    );
                    log.debug("[AI Suggest][Identify] Query='{}' results={} ", channel, docs.size());
                    if (!docs.isEmpty()) {
                        // Normalize category metadata: guard nulls and trim whitespace
                        Object metaCat = docs.get(0).getMetadata().get("category");
                        String category = metaCat == null ? "" : metaCat.toString().trim();
                        if (!category.isEmpty()) identifiedCategories.add(category);
                    }
                } catch (Exception e) {
                    log.warn("Error identifying category for channel: {}", channel, e);
                }
            }
        }

        // 2. SEARCH COMMUNITY LIBRARY
        if (!identifiedCategories.isEmpty()) {
            log.info("AI Suggest: Looking for channels in categories: {}", identifiedCategories);
            for (String category : identifiedCategories) {
                // Find other videos in this category
                List<Document> docs = vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query(category)
                                .topK(50)
                                .build()
                );
                log.info("[AI Suggest][Search By Category] category='{}' rawResults={}", category, docs.size());

                int addedCount = 0;
                int rejectedCount = 0;

                // Collect channel names from results that actually belong to the category
                for (int i = 0; i < docs.size(); i++) {
                    Document d = docs.get(i);
                    Object metaCat = d.getMetadata().get("category");
                    Object metaChannel = d.getMetadata().get("channel");
                    String docCategory = metaCat == null ? "" : metaCat.toString().trim();
                    String docChannel = metaChannel == null ? "" : metaChannel.toString().trim();

                    // Be permissive: exact match OR substring match (handles small variations/trimming in stored metadata)
                    boolean categoryMatch = false;
                    if (!docCategory.isEmpty()) {
                        String a = category.toLowerCase();
                        String b = docCategory.toLowerCase();
                        categoryMatch = a.equals(b) || a.contains(b) || b.contains(a);
                    }

                    if (categoryMatch && !docChannel.isEmpty()) {
                        communityChannels.add(docChannel);
                        addedCount++;
                        log.debug("[AI Suggest][Added] doc#{} channel='{}' category='{}'", i, docChannel, docCategory);
                    } else {
                        rejectedCount++;
                        log.debug("[AI Suggest][Rejected] doc#{} reason=[categoryMatch={}, channelEmpty={}] docCategory='{}' channel='{}'",
                                i, categoryMatch, docChannel.isEmpty(), docCategory, docChannel);
                    }
                }
                log.info("[AI Suggest][Category Summary] category='{}' added={} rejected={}", category, addedCount, rejectedCount);
            }
        } else {
            // No categories detected -> Search by playlist name as fallback
            log.info("AI Suggest: No categories detected, searching by playlist name: {}", playlistName);
            List<Document> docs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(playlistName)
                            .topK(50)
                            .build()
            );
            log.debug("[AI Suggest][Search By Playlist] query='{}' rawResults={}", playlistName, docs.size());
            docs.forEach(d -> communityChannels.add((String) d.getMetadata().get("channel")));
        }

        // Remove channels we already have
        currentChannels.forEach(communityChannels::remove);

        log.info("[AI Suggest][After Dedup] pool size before shuffle={}", communityChannels.size());
        List<String> availableChannels = new ArrayList<>(communityChannels);
        Collections.shuffle(availableChannels);
        // Keep a diverse but compact context (max 20)
        availableChannels = availableChannels.stream().distinct().limit(20).toList();

        String categoriesStr = identifiedCategories.isEmpty() ? "Unknown" : String.join(", ", identifiedCategories);
        String communityHint = availableChannels.isEmpty() ? "" : String.join(", ", availableChannels);
        log.debug("[AI Suggest] Candidate channel pool: total={}, sampledForPrompt={}", communityChannels.size(), availableChannels.size());

        // Tight system/user prompts to ensure compact JSON output only (avoid markdown or prose)
        String system = "You are a strict recommendation engine. Return ONLY a compact JSON array of channel names. " +
                "NO explanations. NO markdown. NO code fences. If none, return [].";

        String user = ("""
                TASK: Suggest EXACTLY 3 YouTube channel names.

                CONTEXT:
                - Playlist Name: "%s"
                - Detected Categories: %s
                - Current Channels (EXCLUDE): %s
                - Available Channels (ONLY CHOOSE FROM): %s

                RULES:
                - Choose ONLY from Available Channels and EXCLUDE Current Channels.
                - Do NOT invent or rename channels.
                - Output must be ONLY a JSON array of strings, e.g.: ["Channel 1","Channel 2","Channel 3"].
                - If fewer than 3 exist, return a smaller array or [].
                """
        ).formatted(
                playlistName,
                categoriesStr,
                String.join(", ", currentChannels),
                communityHint
        );

        String response = chatClient
                .prompt()
                .system(system)
                .user(user)
                .call()
                .content();
        if (response == null || response.isBlank()) return "";

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.trim());
            JsonNode channelsNode;
            if (root.isArray()) {
                channelsNode = root; // New compact array format
            } else {
                channelsNode = root.get("channels"); // Backward compatibility
            }

            if (channelsNode == null || !channelsNode.isArray()) return "";

            List<String> channels = new ArrayList<>();
            for (JsonNode node : channelsNode) {
                String name = node.asText().trim();
                if (!name.isEmpty()) {
                    channels.add(name);
                }
            }

            // Only allow picks from the sampled set we actually showed the model
            Set<String> allowed = availableChannels.stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());

            List<String> finalChannels = channels.stream()
                    .filter(c -> allowed.contains(c.toLowerCase()))
                    .limit(3)
                    .toList();

            return String.join(",", finalChannels);

        } catch (JsonProcessingException jpe) {
            // Explicitly log malformed JSON to help diagnose truncation/EoF issues
            log.error("Malformed AI JSON: {}", response);
            return "";
        } catch (Exception e) {
            log.error("Error parsing AI channel suggestions response", e);
            return "";
        }
    }
}
