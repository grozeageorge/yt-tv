package com.example.yt_tv.dtos;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ChannelCreateDto {
    private String ytChannelId;
    private String name;
    private String thumbnailUrl;
}
