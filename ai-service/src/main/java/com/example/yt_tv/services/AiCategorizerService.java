package com.example.yt_tv.services;

import com.example.yt_tv.dtos.YoutubeVideoInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Profile("!aws")
@Slf4j
public class AiCategorizerService {
    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            "K-Pop",
            "Hip-Hop",
            "Pop Music",
            "Rock",
            "Electronic",
            "Music (General)",
            "Science",
            "Tech",
            "Gaming",
            "Cooking",
            "Vlog",
            "News",
            "Education",
            "Sports",
            "Movies",
            "Comedy"
    );

    // Precompute normalized -> canonical map for permissive matching of model outputs
    private static final java.util.Map<String, String> NORMALIZED_TO_CANONICAL = ALLOWED_CATEGORIES.stream()
            .collect(Collectors.toUnmodifiableMap(
                    c -> com.example.yt_tv.tools.NormalizationUtils.normalizeCategory(c),
                    c -> c
            ));

    private final ChatClient chatClient;

    public AiCategorizerService(ChatModel chatModel) {
        this.chatClient = ChatClient.create(chatModel);
    }

    public String categorizeChannel(String channelName, List<YoutubeVideoInfo> sampleVideos, List<String> youtubeHints) {
        String videoTitles = sampleVideos.stream()
                .limit(5)
                .map(YoutubeVideoInfo::title)
                .collect(Collectors.joining("\n"));

        String categories = "K-Pop, Hip-Hop, Pop Music, Rock, Electronic, Music (General), Science, Tech, Gaming, Cooking, Vlog, News, Education, Sports, Movies, Comedy";

        String prompt = """
                You are a strict categorizer.
                
                TASK: Classify this YouTube Channel into EXACTLY ONE category from the allowed list.
                
                INPUTS:
                - Channel: "%s"
                - Hints: %s
                - Video Titles (sample):
                %s
                
                ALLOWED CATEGORIES:
                [%s]
                
                OUTPUT RULES (MANDATORY):
                - Respond ONLY in VALID JSON
                - NO extra text, NO markdown
                - Use this exact schema:
                  { "category": "<one of allowed categories>" }
                - If unknown, respond with: { "category": "" }
                
                HELPER HINTS:
                - "Blackpink", "BTS", "NewJeans" -> K-Pop.
                - "The Weeknd", "Taylor Swift" -> Pop Music.
                - "Eminem" -> Hip-Hop.
                - "Veritasium" -> Science.
                """.formatted(channelName, String.join(", ", youtubeHints), videoTitles, categories);

        String response = chatClient.prompt(prompt).call().content();

        if (response == null || response.isBlank()) {
            log.warn("Categorizer returned empty response for channel='{}'", channelName);
            return null; // Let callers decide how to handle unknown category
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.trim());
            JsonNode catNode = root.get("category");
            if (catNode == null) {
                log.warn("Categorizer JSON missing 'category' field for channel='{}'", channelName);
                return null;
            }
            String cleaned = catNode.asText("").trim();
            if (cleaned.isEmpty()) {
                return null;
            }

            String normalized = com.example.yt_tv.tools.NormalizationUtils.normalizeCategory(cleaned);

            // If model drifted to generic 'entertainment', don't coerce; return null so we can surface the issue.
            if ("entertainment".equals(normalized)) {
                log.debug("Categorizer produced 'Entertainment' for '{}'; treating as unknown to avoid masking errors.", channelName);
                return null;
            }

            // If the model produced a near-match (e.g., 'popmusic' or 'Pop Music'), map to canonical.
            if (NORMALIZED_TO_CANONICAL.containsKey(normalized)) {
                return NORMALIZED_TO_CANONICAL.get(normalized);
            }

            // Keep a small music heuristic, but only if the category is otherwise invalid
            if (!ALLOWED_CATEGORIES.contains(cleaned) && isMusicRelated(channelName, youtubeHints, videoTitles)) {
                if (ALLOWED_CATEGORIES.contains("Music (General)")) {
                    return "Music (General)";
                }
            }

            if (!ALLOWED_CATEGORIES.contains(cleaned)) {
                log.warn("Categorizer produced invalid category='{}' for channel='{}'", cleaned, channelName);
                return null;
            }

            return cleaned;
        } catch (Exception e) {
            log.error("Failed to parse categorizer response for channel='{}'", channelName, e);
            return null;
        }
    }

    private boolean isMusicRelated(String channelName, List<String> youtubeHints, String videoTitles) {
        String haystack = String.join(" ",
                safeLower(channelName),
                safeLower(String.join(" ", youtubeHints)),
                safeLower(videoTitles)
        );

        return haystack.contains("music")
                || haystack.contains("song")
                || haystack.contains("songs")
                || haystack.contains("lyrics")
                || haystack.contains("album")
                || haystack.contains("artist")
                || haystack.contains("band")
                || haystack.contains("track")
                || haystack.contains("cover")
                || haystack.contains("playlist")
                || haystack.contains("mix")
                || haystack.contains("live");
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
