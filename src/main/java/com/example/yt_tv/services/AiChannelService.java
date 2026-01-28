package com.example.yt_tv.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
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
                    if (!docs.isEmpty()) {
                        String category = (String) docs.get(0).getMetadata().get("category");
                        if (category != null) identifiedCategories.add(category);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // 2. SEARCH COMMUNITY LIBRARY
        if (!identifiedCategories.isEmpty()) {
            System.out.println("AI Suggest: Looking for channels in categories: " + identifiedCategories);
            for (String category : identifiedCategories) {
                // Find other videos in this category
                List<Document> docs = vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query(category)
                                .topK(10)
                                .filterExpression("category == '" + category + "'")
                                .build()
                );
                // Collect channel names from results
                docs.forEach(d -> communityChannels.add((String) d.getMetadata().get("channel")));
            }
        } else {
            // No categories detected → do NOT guess
            return "";
        }

        // Remove channels we already have
        currentChannels.forEach(communityChannels::remove);

        String categoriesStr = identifiedCategories.isEmpty() ? "Unknown" : String.join(", ", identifiedCategories);
        String communityHint = communityChannels.isEmpty() ? "" : String.join(", ", communityChannels);

        String prompt = """
            You are a recommendation engine.
            
            TASK:
            Suggest EXACTLY 3 YouTube channel names.
            
            CONTEXT:
            Playlist Name: "%s"
            Detected Categories: %s
            Current Channels (DO NOT USE): %s
            Available Channels (ONLY OPTIONS THAT YOU CAN USE): %s
            
            RULES (MANDATORY):
            - You MUST select channels ONLY from Available Channels that are NOT listed in Current Channels.
            - Do NOT invent or rename channels.
            - Respond in VALID JSON ONLY
            - NO explanations
            - NO markdown
            - NO extra text
            - Channel names must be strings
            - Must not include current channels
            
            OUTPUT FORMAT (STRICT):
            {
              "channels": ["Channel 1", "Channel 2", "Channel 3"]
            }
            
            If fewer than 3 exist, return:
            {
              "channels": []
            }
            """.formatted(
                playlistName,
                categoriesStr,
                String.join(", ", currentChannels),
                communityHint
            );

        String response = chatClient.prompt(prompt).call().content();
        if (response == null || response.isBlank()) return "";

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            JsonNode channelsNode = root.get("channels");

            if (channelsNode == null || !channelsNode.isArray()) return "";

            List<String> channels = new ArrayList<>();
            for (JsonNode node : channelsNode) {
                String name = node.asText().trim();
                if (!name.isEmpty()) {
                    channels.add(name);
                }
            }

            Set<String> allowed = communityChannels.stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());

            List<String> finalChannels = channels.stream()
                    .filter(c -> allowed.contains(c.toLowerCase()))
                    .limit(3)
                    .toList();

            return String.join(",", finalChannels);

        } catch (Exception e) {
            e.printStackTrace();
            return "No channels found";
        }
    }
}
