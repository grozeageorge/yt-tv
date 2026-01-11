package com.example.yt_tv.services;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.ai.document.Document;

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
            // Fallback: Use Playlist Name
            List<Document> docs = vectorStore.similaritySearch(
                    SearchRequest.builder().query(playlistName).topK(10).build()
            );
            docs.forEach(d -> communityChannels.add((String) d.getMetadata().get("channel")));
        }

        // Remove channels we already have
        communityChannels.removeAll(currentChannels);

        // 3. ASK GEMMA
        String categoriesStr = identifiedCategories.isEmpty() ? "Unknown" : String.join(", ", identifiedCategories);
        String communityHint = communityChannels.isEmpty() ? "None found in DB" : String.join(", ", communityChannels);

        String prompt = """
                You are a YouTube Expert.
                
                Context:
                - Playlist Name: "%s"
                - User's Current Channels: %s
                - Detected Categories: %s
                
                Community Library Suggestions (Channels found in your local database): [%s]
                
                Task: Recommend exactly 3 NEW YouTube channels.
                
                STRATEGY:
                1. If "Community Library Suggestions" has channels that fit the Detected Categories, RECOMMEND THEM FIRST. (This helps the user reuse existing data).
                2. If the Community list is empty or unrelated, suggest famous external channels that fit the Categories.
                3. If Mixed Categories (e.g. Pop + Science), suggest a mix (e.g. 1 Pop channel, 1 Science channel).
                
                CRITICAL RULES:
                - Do NOT suggest channels from "Current Channels".
                - Return ONLY comma-separated names.
                - Return "NONE" if you can't find any.
                """.formatted(playlistName, String.join(", ", currentChannels), categoriesStr, communityHint);

        String response = chatClient.prompt(prompt).call().content();
        if (response == null) return "";
        return response.replace("\n", "").trim();
    }
}
