package com.example.yt_tv.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ChatResponseDto(
        @JsonProperty("message") String message,
        @JsonProperty("videoIds") List<String> videoIds
) {
}
