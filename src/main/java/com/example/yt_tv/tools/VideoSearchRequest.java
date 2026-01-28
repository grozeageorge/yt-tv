package com.example.yt_tv.tools;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

@JsonClassDescription("Search request for the video database.")
public record VideoSearchRequest(
        @JsonPropertyDescription("The search term. If the user asks for a specific Channel (e.g. 'Astrum'), put the channel name here. If they ask for a Topic (e.g. 'Space'), put the topic here.")
        String query,

        @JsonPropertyDescription("OPTIONAL: Strict Category Filter. Use this ONLY if the user request maps to one of these EXACT values: [K-Pop, Hip-Hop, Pop Music, Rock, Electronic, Music (General), Science, Tech, Gaming, Cooking, Vlog, News, Education, Sports, Movies, Comedy, Nature]. If searching for a specific Channel Name, leave this NULL.")
        String categoryFilter
) {}
