package com.example.yt_tv.services;

import com.example.yt_tv.dtos.ChatResponseDto;
import com.example.yt_tv.entities.WatchedVideo;
import com.example.yt_tv.repositories.WatchedVideoRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class AiChatService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final WatchedVideoRepository watchedVideoRepository;
    private static final String AI_NAME = "Nova";

    public AiChatService(ChatModel chatModel, VectorStore vectorStore, WatchedVideoRepository watchedVideoRepository) {
        this.chatClient = ChatClient.create(chatModel);
        this.vectorStore = vectorStore;
        this.watchedVideoRepository = watchedVideoRepository;
    }

    public ChatResponseDto chatWithData(String userQuery, Long userId) {
        // STEP 1: CLASSIFY INTENT
        String classificationPrompt = """
                Analyze the user input: "{query}"
                
                Classify it into exactly one of these categories:
                1. SEARCH (User is asking for videos, content, or recommendations like "show me science", "play music", "I'm bored")
                2. IDENTITY (User asks "who are you?", "what is your name?", "what can you do?")
                3. OFF_TOPIC (User asks about weather, math, history, coding, or general knowledge unrelated to the TV library)
                
                Respond with JUST the category name. Nothing else.
                """;

        String intent = chatClient.prompt(new PromptTemplate(classificationPrompt).create(Map.of("query", userQuery)))
                .call().content().trim().toUpperCase();

        System.out.println("AI Intent: " + intent);

        if (intent.contains("SEARCH")) {
            return handleSearchIntent(userQuery, userId);
        } else if (intent.contains("IDENTITY")) {
            return handleIdentityIntent(userQuery);
        } else {
            return handleOffTopicIntent(userQuery);
        }
    }

    // --- 1. SEARCH INTENT (Personal Curator) ---
    private ChatResponseDto handleSearchIntent(String userQuery, Long userId) {
        // --- LOGIC TREE START ---

        String searchCategory = "NONE";
        String aiMessage = "";

        // 1. CHECK FOR CATEGORY (Highest Priority)
        // Does the user explicitly ask for "Science", "Music", "Kpop"?
        String detectedCategory = detectTargetCategory(userQuery);

        if (!"ANY".equals(detectedCategory)) {
            searchCategory = detectedCategory;
            aiMessage = "You asked for " + detectedCategory + ". I've queued up a mix of videos from that category for a true TV experience. Shall we play?";
            System.out.println("AI Debug: Found Direct Category -> " + searchCategory);
        }
        else {
            // 2. CHECK FOR SPECIFIC CHANNEL
            String targetChannel = detectTargetChannel(userQuery);

            if (!"NONE".equals(targetChannel)) {
                // Check if channel exists in DB
                List<Document> channelCheck = vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query(targetChannel)
                                .topK(1)
                                .filterExpression("channel == '" + targetChannel + "'")
                                .build()
                );

                if (channelCheck.isEmpty()) {
                    // STOP. Do not hallucinate.
                    return new ChatResponseDto(
                            "I can't find '" + targetChannel + "' in the database. Please add this channel to a playlist first, or ask for a general category.",
                            List.of()
                    );
                } else {
                    // Channel Found -> Get its Category -> Pivot to Category Mode
                    String foundCategory = (String) channelCheck.get(0).getMetadata().get("category");
                    searchCategory = foundCategory;
                    aiMessage = "I see you asked for " + targetChannel + " (Category: " + foundCategory + "). My goal is to give you a TV experience, so I've created a " + foundCategory + " mix including them. Ready?";
                    System.out.println("AI Debug: Pivot Channel " + targetChannel + " -> Category " + searchCategory);
                }
            }
        }

        // 3. FINAL EXECUTION
        if ("NONE".equals(searchCategory)) {
            return new ChatResponseDto("I couldn't identify a specific category or channel in your request. Try asking for 'Science' or a specific channel name.", List.of());
        }

        // 4. PERFORM QUERY (Strictly by Category)
        List<Document> documents = new ArrayList<>();
        try {
            documents = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(searchCategory) // Search for category name to be safe
                            .topK(100)
                            .filterExpression("category == '" + searchCategory + "'")
                            .build()
            );
        } catch (Exception e) {
            System.out.println("AI Debug: Filter error: " + e.getMessage());
        }

        if (documents.isEmpty()) {
            return new ChatResponseDto("I know category '" + searchCategory + "' exists, but I couldn't find any videos. Try syncing!", List.of());
        }

        // 5. SHUFFLE & FILTER WATCHED
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

        if (videoIds.isEmpty()) {
            return new ChatResponseDto("You've watched all the " + searchCategory + " videos! Sync more channels.", List.of());
        }

        // Return clean message + IDs
        return new ChatResponseDto(aiMessage, videoIds);
    }

    private String detectTargetChannel(String query) {
        String prompt = """
                Extract the exact YouTube Channel Name from this query: "%s"
                Examples: "Play Astrum" -> "Astrum", "Show me Blackpink" -> "Blackpink".
                If no specific channel is named, or it's just a genre, return "NONE".
                """.formatted(query);

        return cleanQuotes(chatClient.prompt(prompt).call().content());
    }

    private String detectTargetCategory(String query) {
        String prompt = """
                Map query "%s" to one of:
                [K-Pop, Pop Music, Hip-Hop, Rock, Science, Education, Tech, Gaming, Cooking, Vlog, News, Nature, Comedy]
                Return ONLY the Category Name or ANY.
                """.formatted(query);

        return cleanQuotes(chatClient.prompt(prompt).call().content());
    }

    // --- 2. IDENTITY INTENT (Nova) ---
    private ChatResponseDto handleIdentityIntent(String userQuery) {
        String prompt = """
                User asked: "{query}"
                Answer that your name is %s. 
                You are their personal AI curator for this YouTube TV app. 
                Your job is to organize their library and help them discover content.
                Be brief and friendly.
                """.formatted(AI_NAME);

        String response = cleanQuotes(chatClient.prompt(new PromptTemplate(prompt).create(Map.of("query", userQuery))).call().content());
        return new ChatResponseDto(response, List.of());
    }

    // --- 3. OFF-TOPIC INTENT (Polite Refusal) ---
    private ChatResponseDto handleOffTopicIntent(String userQuery) {
        String prompt = """
                User asked: "{query}"
                Politely refuse to answer. 
                State that as %s (a video curator), you only have access to their video library, not real-world information.
                Ask them to request a video topic instead.
                """.formatted(AI_NAME);

        String response = cleanQuotes(chatClient.prompt(new PromptTemplate(prompt).create(Map.of("query", userQuery))).call().content());
        return new ChatResponseDto(response, List.of());
    }

    private String cleanQuotes(String text) {
        if (text == null) return "";
        text = text.trim();
        while (text.startsWith("\"") || text.startsWith("“") || text.startsWith("'")) text = text.substring(1).trim();
        while (text.endsWith("\"") || text.endsWith("”") || text.endsWith("'")) text = text.substring(0, text.length() - 1).trim();
        return text;
    }
}