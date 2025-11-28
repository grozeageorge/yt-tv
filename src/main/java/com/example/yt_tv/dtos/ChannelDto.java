package com.example.yt_tv.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class ChannelDto {
    private Long id;
    private String ytChannelId;
    private String name;
    private String thumbnailUrl;
    private Instant lastSync;
}
