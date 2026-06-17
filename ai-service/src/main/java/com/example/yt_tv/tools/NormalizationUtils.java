package com.example.yt_tv.tools;

import java.util.Locale;

/**
 * Utility helpers for normalizing category/channel strings for robust matching.
 * Normalization is: lower-case and remove all non-alphanumeric characters
 * (so spaces and punctuation are ignored).
 */
public final class NormalizationUtils {

    private NormalizationUtils() {
    }

    /**
     * General-purpose normalizer used for both categories and channel names.
     * Lower-cases and strips all non-alphanumeric characters so comparisons
     * ignore spaces and punctuation.
     */
    public static String normalize(String input) {
        if (input == null) return "";
        return input.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    /**
     * Backwards-compatible category normalizer.
     */
    public static String normalizeCategory(String input) {
        return normalize(input);
    }

    /**
     * Normalizer specifically for channel names (delegates to normalize).
     */
    public static String normalizeChannel(String input) {
        return normalize(input);
    }
}

