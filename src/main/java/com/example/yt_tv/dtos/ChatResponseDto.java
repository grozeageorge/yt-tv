package com.example.yt_tv.dtos;

import java.util.List;

public record ChatResponseDto(String message, List<String> videoIds) {
}
