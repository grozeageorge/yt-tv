package com.example.yt_tv.dtos;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoDto {
    private Long id;
    private String ytVideoId;
    private String title;
    private String thumbnailUrl;
    private Long channelId;
}
