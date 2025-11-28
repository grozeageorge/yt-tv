package com.example.yt_tv.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChannelCreateDto {
    private String ytChannelId;
    private String name;
    private String thumbnailUrl;
}
