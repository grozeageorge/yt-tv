package com.example.yt_tv.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChannelCategorizeRequest {
    private String channelName;
    private String description;
    private List<YoutubeVideoInfo> sampleVideos;
    private List<String> youtubeHints;
}
