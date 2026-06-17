package com.example.yt_tv.tools;

import java.util.List;

public record VideoSearchResponse(List<String> videoIds, String contextForAi) {
}
