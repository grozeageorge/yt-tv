package com.example.yt_tv.tools;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

@JsonClassDescription("Search request for the video database. IMPORTANT: When calling this tool the LLM MUST pass explicit filters for precise requests. If the user requests a specific Channel or Artist, set the 'channelFilter' field to the exact channel name. If the user requests a specific Genre or Category (from the allowed list), set the 'categoryFilter' exactly to the canonical category name. Do NOT pass a vague/generic query when a specific channel or category is intended.")
public record VideoSearchRequest(
        @JsonPropertyDescription("The search term. Use this for free-text queries only. If the user asked for a specific Channel, do NOT place the channel name here — instead set 'channelFilter'. If the user asked for a specific Category, prefer setting 'categoryFilter' to the canonical category name and leave this blank or short.")
        String query,

        @JsonPropertyDescription("OPTIONAL: Strict Category Filter. Use this ONLY if the user request maps to one of these EXACT values: [K-Pop, Hip-Hop, Pop Music, Rock, Electronic, Music (General), Science, Tech, Gaming, Cooking, Vlog, News, Education, Sports, Movies, Comedy, Nature]. When provided the search must strictly filter by the normalized category metadata (no fuzzy semantic-only search).")
        String categoryFilter,

        @JsonPropertyDescription("OPTIONAL: Strict Channel Filter. When the user requests a specific channel or artist, populate this field with the exact channel name. The search logic will strictly filter by the channel (case-insensitive normalized match) and must NOT fall back to an unfiltered semantic search.")
        String channelFilter
) {}
