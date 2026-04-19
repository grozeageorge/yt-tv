package com.example.yt_tv.services;

import com.example.yt_tv.dtos.ChatResponseDto;
import com.example.yt_tv.repositories.WatchedVideoRepository;
import com.example.yt_tv.tools.VideoSearchFunction;
import com.example.yt_tv.tools.VideoSearchRequest;
import com.example.yt_tv.tools.VideoSearchResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

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

        Intent intent = classifyIntent(userQuery);

        return switch (intent) {
            case IDENTITY -> identityResponse();
            case OFF_TOPIC -> offTopicResponse();
            case SEARCH -> handleSearch(userQuery, userId);
        };
    }

    // ROUTER AI
    private Intent classifyIntent(String query) {
        String prompt = """
                Classify the user input into exactly ONE category:
                
                SEARCH -> asking for videos, playlists, music, topics, channnels
                IDENTITY -> asking who you are or what you do
                OFF_TOPIC -> anything unrelated to Youtube Videos.
                
                Respond with only: SEARCH, IDENTITY, or OFF_TOPIC.
                Input: "%s"
                """.formatted(query);

        String result = chatClient.prompt(prompt).call().content().trim().toUpperCase();

        try {
            return Intent.valueOf(result);
        } catch (Exception e) {
            return Intent.OFF_TOPIC;
        }
    }

    private ChatResponseDto identityResponse() {
        String prompt = """
                You are Nova, an Youtube TV Ai Assistant.
                Your purpose is to help users find Youtube videos based on a category and put them into a playlist.
                You can access videos from a wide database build by other users, like a 'community library'.
                The user asked about your identity and your job right now is to introduce yourself in one sentence to the user based on the description above.
                """;

        String result = chatClient.prompt(prompt).call().content();
        return new ChatResponseDto(
                cleanQuotes(result),
                List.of()
        );
    }

    private ChatResponseDto offTopicResponse() {
        String prompt = """
                You are Nova, an Youtube TV Ai Assistant.
                Your purpose is to help users find Youtube videos based on a category and put them into a playlist.
                The user asked something unrelated to Youtube videos and your job right now is to decline the request in one sentence and say what you were made for.
                """;

        String result = chatClient.prompt(prompt).call().content();

        return new ChatResponseDto(
                cleanQuotes(result),
                List.of()
        );
    }

    private ChatResponseDto handleSearch(String userQuery, Long userId) {
        AtomicReference<List<String>> capturedVideoIds = new AtomicReference<>(List.of());

        VideoSearchFunction searchFunction =
                new VideoSearchFunction(vectorStore, watchedVideoRepository, userId);

        searchFunction.setVideoIdsListener(capturedVideoIds::set);

        FunctionToolCallback<VideoSearchRequest, VideoSearchResponse> searchTool =
                FunctionToolCallback.builder("searchVideos", searchFunction)
                        .description("""
                            Search the video database.
                            Use ONLY when the user asks for videos, playlists, music, topics, or channels.
                            Do NOT use for identity or off-topic questions.
                            """)
                        .inputType(VideoSearchRequest.class)
                        .build();

        String categories = "K-Pop, Hip-Hop, Pop Music, Rock, Electronic, Music (General), Science, Tech, Gaming, Cooking, Vlog, News, Education, Sports, Movies, Comedy";

        String agentPrompt = """
            You are %s, an AI video curator.

            USER REQUEST: "{query}"

            INSTRUCTIONS:
            - Decide if a search is needed.
            - If yes, call searchVideos.
            - Use categoryFilter ONLY if the request clearly maps to one of the categories: %s.
            - Otherwise, put everything in query.
            - After tool results, explain briefly what you found and ask if the user wants to play.

            CONSTRAINTS:
            - Never invent videos.
            - Never call tools unnecessarily.
            - No quotes.
            """.formatted(AI_NAME, categories);

        String aiText = chatClient
                .prompt(new PromptTemplate(agentPrompt).create(Map.of("query", userQuery)))
                .toolCallbacks(searchTool)
                .call()
                .content();

        return new ChatResponseDto(cleanQuotes(aiText), capturedVideoIds.get());

    }

    private String cleanQuotes(String text) {
        if (text == null) return "";
        return text.replaceAll("^[\"']+|[\"']+$", "").trim();
    }
}