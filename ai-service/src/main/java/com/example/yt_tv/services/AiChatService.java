package com.example.yt_tv.services;

import com.example.yt_tv.clients.PlaylistClient;
import com.example.yt_tv.dtos.ChatResponseDto;
import com.example.yt_tv.tools.SearchQueryParser;
import com.example.yt_tv.tools.SearchQueryParser.SearchPlan;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Profile("!aws")
public class AiChatService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final PlaylistClient playlistClient;
    private ChatClient mcpChatClient; // Optional: tool-enabled client for DB queries

    public AiChatService(ChatModel chatModel, VectorStore vectorStore, PlaylistClient playlistClient) {
        this.chatClient = ChatClient.create(chatModel);
        this.vectorStore = vectorStore;
        this.playlistClient = playlistClient;
        this.mcpChatClient = this.chatClient; // default fallback
    }

    // If a specialized MCP-enabled ChatClient bean exists, prefer it.
    @Autowired(required = false)
    public void setMcpChatClient(@Qualifier("mcpChatClient") ChatClient mcpChatClient) {
        if (mcpChatClient != null) {
            this.mcpChatClient = mcpChatClient;
        }
    }

    public ChatResponseDto chatWithData(String userQuery, Long userId) {
        String lowerQuery = userQuery == null ? "" : userQuery.toLowerCase().trim();

        if (isPlaylistCountQuery(lowerQuery)) {
            if (isRequestingAnotherUser(lowerQuery)) {
                return new ChatResponseDto(
                        "I can only access your own account data. Ask me to count your playlists.",
                        List.of()
                );
            }
            return handlePlaylistCount(userId);
        }

        if (lowerQuery.matches("^(yes|yeah|yep|sure|ok|okay|alright|y)$")) {
            return new ChatResponseDto(
                    "If you want to play, click the Play Found Videos button. You can also ask for another category.",
                    List.of()
            );
        }

        Intent intent = classifyIntent(userQuery, userId);

        return switch (intent) {
            case IDENTITY -> identityResponse(userId);
            case OFF_TOPIC -> offTopicResponse(userId);
            case DATABASE_QUERY -> handleDatabaseQuery(userQuery, userId);
            case SEARCH -> handleSearch(userQuery, userId);
        };
    }

    // ROUTER AI
    private Intent classifyIntent(String query, Long userId) {
        if (query == null || query.isBlank()) {
            return Intent.OFF_TOPIC;
        }
        String lowerQuery = query.toLowerCase().trim();

        // Handle simple responses that don't needs AI classification
        if (lowerQuery.matches("^(yes|yeah|yep|sure|ok|okay|alright|y)$")) {
            return Intent.OFF_TOPIC;
        }
        if (lowerQuery.matches("^(no|nope|nah|n)$")) {
            return Intent.OFF_TOPIC; // User declined, treat as conversation end
        }

        // Heuristic: database/account-type questions
        if (lowerQuery.contains("how many") || lowerQuery.contains("count") || lowerQuery.contains("total")) {
            if (lowerQuery.contains("playlist") || lowerQuery.contains("playlists")
                    || lowerQuery.contains("channel") || lowerQuery.contains("channels")
                    || lowerQuery.contains("watched") || lowerQuery.contains("unwatched")) {
                return Intent.DATABASE_QUERY;
            }
        }
        if (lowerQuery.startsWith("list my") || lowerQuery.startsWith("show my") || lowerQuery.contains("do i have any")) {
            return Intent.DATABASE_QUERY;
        }

        String prompt = """
                Classify the user input into exactly ONE category:
                
                SEARCH -> asking for videos, playlists, music, topics, channels, or generic affirmations
                IDENTITY -> asking who you are or what you do
                DATABASE_QUERY -> questions about the user's data (e.g., how many playlists/channels, counts, watched/unwatched)
                OFF_TOPIC -> anything unrelated to Youtube Videos.
                
                Respond with only: SEARCH, IDENTITY, DATABASE_QUERY, or OFF_TOPIC.
                Input: "%s"
                """.formatted(query);

        // Ensure the model always receives the current user's id and strict security guidance
        // as a hidden system message so it only answers about the provided USER_ID.
        String userIdSystem = buildUserIdSystemMessage(userId);

        String result = chatClient
                .prompt()
                .system(userIdSystem)
                .user(prompt)
                .call()
                .content();
        if (result == null) return Intent.OFF_TOPIC;

        result = result.trim().toUpperCase();

        try {
            return Intent.valueOf(result);
        } catch (Exception e) {
            return Intent.OFF_TOPIC;
        }
    }

    private ChatResponseDto handleDatabaseQuery(String userQuery, Long userId) {
        String systemPrompt = buildDatabaseSystemPrompt(userId);

        String answer;
        try {
            String userIdSystem = buildUserIdSystemMessage(userId);

            answer = mcpChatClient
                    .prompt()
                    .system(userIdSystem)
                    .system(systemPrompt)
                    .user(userQuery)
                    .call()
                    .content();
        } catch (Exception e) {
            answer = null;
        }

        if (answer == null || answer.isBlank()) {
            answer = "I couldn't answer that right now. Please try again later.";
        }

        return new ChatResponseDto(cleanQuotes(answer), List.of());
    }

    private String buildDatabaseSystemPrompt(Long userId) {
        if (userId == null) {
            return "You are answering a user-data SQL question. The user id is missing, so ask the user to sign in again.";
        }

        return """
                You are answering a user-data SQL question. The user id is %d.
                If the user asks for the number of playlists, you must call the MCP tool 'count_user_playlists' exactly once.
                If you can answer other SQL questions for the user, try to use the other available tools. If not, politely refuse.
                Do not use or trust any different user id. Use only the one provided in this message.
                Return only one short conversational sentence containing the answer.
                Do not output SQL, JSON, tool names, or chain-of-thought.
                If the tools fail, politely refuse and say you cannot do that right now.
                """.formatted(userId);
    }

    private ChatResponseDto identityResponse(Long userId) {
        String prompt = """
                You are Nova, an Youtube TV Ai Assistant.
                Your purpose is to help users find Youtube videos based on a category and put them into a playlist.
                You can access videos from a wide database build by other users, like a 'community library'.
                The user asked about your identity and your job right now is to introduce yourself in one sentence to the user based on the description above.
                """;
        String userIdSystem = buildUserIdSystemMessage(userId);

        String result = chatClient
                .prompt()
                .system(userIdSystem)
                .user(prompt)
                .call()
                .content();
        return new ChatResponseDto(
                cleanQuotes(result),
                List.of()
        );
    }

    private ChatResponseDto offTopicResponse(Long userId) {
        String prompt = """
                You are Nova, an Youtube TV Ai Assistant.
                Your purpose is to help users find Youtube videos based on a category and put them into a playlist.
                The user asked something unrelated to Youtube videos and your job right now is to decline the request in one sentence and say what you were made for.
                """;

        String userIdSystem = buildUserIdSystemMessage(userId);

        String result = chatClient
                .prompt()
                .system(userIdSystem)
                .user(prompt)
                .call()
                .content();

        return new ChatResponseDto(
                cleanQuotes(result),
                List.of()
        );
    }

     private ChatResponseDto handleSearch(String userQuery, Long userId) {
         SearchPlan plan = SearchQueryParser.parse(userQuery);

         List<Document> documents;
         try {
             boolean hasCategory = plan.categories() != null && !plan.categories().isEmpty();
             boolean hasChannel = plan.channels() != null && !plan.channels().isEmpty();

             var builder = SearchRequest.builder()
                     .query(plan.query())
                     // increase topK a bit when applying strict filters to get a good candidate pool
                     .topK(120);

             if (hasCategory || hasChannel) {
                 // Build strict normalized filter expressions. Do NOT fall back to an unfiltered search
                 // when the model provided a specific category or channel — that causes unrelated hits.
                 String categoryExpr = null;
                 String channelExpr = null;

                 if (hasCategory) {
                     // support multiple categories by OR-ing normalizedCategory checks
                     StringBuilder sb = new StringBuilder();
                     for (String c : plan.categories()) {
                         if (sb.length() > 0) sb.append(" || ");
                         sb.append("normalizedCategory == '").append(com.example.yt_tv.tools.NormalizationUtils.normalizeCategory(c)).append("'");
                     }
                     categoryExpr = sb.toString();
                 }

                 if (hasChannel) {
                     StringBuilder sb = new StringBuilder();
                     for (String ch : plan.channels()) {
                         if (sb.length() > 0) sb.append(" || ");
                         sb.append("normalizedChannel == '").append(com.example.yt_tv.tools.NormalizationUtils.normalizeChannel(ch)).append("'");
                     }
                     channelExpr = sb.toString();
                 }

                 String expr;
                 if (categoryExpr != null && channelExpr != null) {
                     expr = "(" + categoryExpr + ") || (" + channelExpr + ")";
                 } else if (categoryExpr != null) {
                     expr = categoryExpr;
                 } else {
                     expr = channelExpr;
                 }

                 builder.filterExpression(expr);
             }

             documents = vectorStore.similaritySearch(builder.build());
         } catch (Exception e) {
             return new ChatResponseDto(
                     "I couldn't reach the video database. Try again in a moment.",
                     List.of()
             );
         }

         if (documents.isEmpty()) {
             return new ChatResponseDto(
                     "I couldn't find any videos from that category. Try another search!",
                     List.of()
             );
         }

         List<Document> filtered = documents.stream()
                 .filter(d -> SearchQueryParser.matchesPlan(d, plan))
                 .toList();

         if (filtered.isEmpty()) {
             filtered = documents; // fallback: no strict matches, use the best semantic hits
         }

         List<String> watchedYtIds = playlistClient.getWatchedYtVideoIds(userId);

         List<String> videoIds = filtered.stream()
                 .map(d -> (String) d.getMetadata().get("videoId"))
                 .filter(Objects::nonNull)
                 .filter(id -> !watchedYtIds.contains(id))
                 .distinct()
                 .limit(20)
                 .toList();

         if (videoIds.isEmpty()) {
             return new ChatResponseDto(
                     "I couldn't find any unwatched videos for that request. Try another search!",
                     List.of()
             );
         }

         String summaryContext = filtered.stream()
                 .limit(6)
                 .map(Document::getText)
                 .collect(Collectors.joining("\n"));

         String summaryPrompt = """
                 You are Nova, a YouTube TV assistant.
                 Based ONLY on the items below, write 2-3 short sentences summarizing what you found.
                 End with a question asking if the user wants to play the videos.

                 ITEMS:
                 %s
                 """.formatted(summaryContext);

          // Inject hidden user id system message + security guidance so tools that require userId always receive it.
          String userIdSystem = buildUserIdSystemMessage(userId);

          String summary = chatClient
                  .prompt()
                  .system(userIdSystem)
                  .user(summaryPrompt)
                  .call()
                  .content();
         String cleaned = sanitizeAiOutput(cleanQuotes(summary));
         if (cleaned.isBlank()) {
             cleaned = "I found videos related to your request. Want to play them?";
         }

         return new ChatResponseDto(cleaned, videoIds);
     }

    private String cleanQuotes(String text) {
        if (text == null) return "";
        return text.replaceAll("^[\"']+|[\"']+$", "").trim();
    }

    private String sanitizeAiOutput(String text) {
        if (text == null || text.isBlank()) return "";
        // Strip tool-call JSON blocks if the model echoes them.
        String sanitized = text.replaceAll("(?s)\\{\\s*\\\"name\\\"\\s*:\\s*\\\"searchVideos\\\".*?\\}\\s*", "");
        return sanitized.replaceAll("(?m)^\\s*\\{.*\\\"searchVideos\\\".*\\}\\s*$", "").trim();
    }

    // Build a hidden system message that always supplies the current user's id and
    // strict security guidance. This prevents the model from answering about other users
    // and ensures that tools requiring a userId receive it.
    private String buildUserIdSystemMessage(Long userId) {
        if (userId == null) {
            return """
                    SECURITY: The current user id is unavailable.
                    Do not query user-specific data. Ask the user to sign in again.
                    """;
        }
        String msg = """
                USER_ID=%d
                SECURITY: For any question about user-specific data, ONLY use the USER_ID provided above.
                When the user refers to 'I', 'me', or 'my', treat it as the provided USER_ID. DO NOT query,
                reveal, or make inferences about other users' data. If the user requests data for another
                user, decline and ask for explicit authorization. Do not output raw SQL, internal tool
                responses, or chain-of-thought. Use tools only when necessary and return a concise final
                conversational answer.
                """.formatted(userId);
        return msg;
    }

    private boolean isPlaylistCountQuery(String lowerQuery) {
        boolean asksCount = lowerQuery.contains("how many") || lowerQuery.contains("count") || lowerQuery.contains("total");
        boolean asksPlaylists = lowerQuery.contains("playlist") || lowerQuery.contains("playlists");
        return asksCount && asksPlaylists;
    }

    private boolean isRequestingAnotherUser(String lowerQuery) {
        return lowerQuery.contains("user with id") || lowerQuery.contains("for user id") || lowerQuery.matches(".*\\buser\\s+\\d+.*");
    }

    private ChatResponseDto handlePlaylistCount(Long userId) {
        if (userId == null) {
            return new ChatResponseDto("I can't determine your account right now. Please sign in again.", List.of());
        }

        try {
            int count = playlistClient.getPlaylistsByUserId(userId).size();
            String message = count == 1 ? "You have 1 playlist." : "You have " + count + " playlists.";
            return new ChatResponseDto(message, List.of());
        } catch (Exception e) {
            return new ChatResponseDto("Cannot retrieve the playlist count right now.", List.of());
        }
    }
}
