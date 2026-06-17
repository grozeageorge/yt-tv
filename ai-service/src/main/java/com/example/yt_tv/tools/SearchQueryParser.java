package com.example.yt_tv.tools;

import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import com.example.yt_tv.tools.NormalizationUtils;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SearchQueryParser {
    private static final Pattern CHANNEL_PATTERN = Pattern.compile("(?:from|by|channel|channels)\\s+([^?!.]+)", Pattern.CASE_INSENSITIVE);

    private SearchQueryParser() {
    }

    public static SearchPlan parse(String query) {
        String raw = query == null ? "" : query.trim();
        String lower = raw.toLowerCase(Locale.ROOT);

        Set<String> categories = new LinkedHashSet<>();
        addCategoryIfMatch(lower, categories, "k-pop", "K-Pop");
        addCategoryIfMatch(lower, categories, "kpop", "K-Pop");
        addCategoryIfMatch(lower, categories, "hip hop", "Hip-Hop");
        addCategoryIfMatch(lower, categories, "hip-hop", "Hip-Hop");
        addCategoryIfMatch(lower, categories, "pop", "Pop Music");
        addCategoryIfMatch(lower, categories, "rock", "Rock");
        addCategoryIfMatch(lower, categories, "electronic", "Electronic");
        addMusicCategoriesIfMatch(lower, categories);
        addCategoryIfMatch(lower, categories, "science", "Science");
        addCategoryIfMatch(lower, categories, "tech", "Tech");
        addCategoryIfMatch(lower, categories, "technology", "Tech");
        addCategoryIfMatch(lower, categories, "gaming", "Gaming");
        addCategoryIfMatch(lower, categories, "game", "Gaming");
        addCategoryIfMatch(lower, categories, "cook", "Cooking");
        addCategoryIfMatch(lower, categories, "recipe", "Cooking");
        addCategoryIfMatch(lower, categories, "vlog", "Vlog");
        addCategoryIfMatch(lower, categories, "news", "News");
        addCategoryIfMatch(lower, categories, "education", "Education");
        addCategoryIfMatch(lower, categories, "learn", "Education");
        addCategoryIfMatch(lower, categories, "sport", "Sports");
        addCategoryIfMatch(lower, categories, "movie", "Movies");
        addCategoryIfMatch(lower, categories, "film", "Movies");
        addCategoryIfMatch(lower, categories, "comedy", "Comedy");
        addCategoryIfMatch(lower, categories, "funny", "Comedy");

        Set<String> channels = new LinkedHashSet<>();
        Matcher matcher = CHANNEL_PATTERN.matcher(raw);
        while (matcher.find()) {
            String group = matcher.group(1);
            splitChannels(group).forEach(channels::add);
        }

        return new SearchPlan(raw.isBlank() ? "videos" : raw, List.copyOf(categories), List.copyOf(channels));
    }

    public static boolean matchesPlan(Document document, SearchPlan plan) {
        if (document == null || plan == null) return false;

        String category = Objects.toString(document.getMetadata().get("category"), "").trim();
        String channel = Objects.toString(document.getMetadata().get("channel"), "").trim();

        // Normalize both plan categories and document category so matching is
        // case-insensitive and ignores spaces/punctuation (e.g. "Pop Music" == "popmusic").
        String docCategoryNorm = NormalizationUtils.normalizeCategory(category);
        boolean categoryMatch = plan.categories().isEmpty() || plan.categories().stream()
                .anyMatch(c -> NormalizationUtils.normalizeCategory(c).equals(docCategoryNorm));
        boolean channelMatch = plan.channels().isEmpty()
                || plan.channels().stream().anyMatch(c -> NormalizationUtils.normalizeChannel(c).equals(NormalizationUtils.normalizeChannel(channel)));

        if (!plan.categories().isEmpty() && !plan.channels().isEmpty()) {
            return categoryMatch || channelMatch;
        }
        if (!plan.categories().isEmpty()) {
            return categoryMatch;
        }
        if (!plan.channels().isEmpty()) {
            return channelMatch;
        }
        return true;
    }

    private static void addCategoryIfMatch(String lower, Set<String> categories, String keyword, String category) {
        if (lower.contains(keyword)) {
            categories.add(category);
        }
    }

    private static void addMusicCategoriesIfMatch(String lower, Set<String> categories) {
        if (!lower.contains("music")) {
            return;
        }
        categories.add("Music (General)");
        categories.add("Pop Music");
        categories.add("K-Pop");
        categories.add("Hip-Hop");
        categories.add("Rock");
        categories.add("Electronic");
    }

    private static List<String> splitChannels(String group) {
        if (group == null) return List.of();
        String cleaned = group.replaceAll("(?i)videos?|channels?", " ");
        String[] parts = cleaned.split("(?:,|&| and )");

        List<String> result = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    public record SearchPlan(String query, List<String> categories, List<String> channels) {
    }
}
