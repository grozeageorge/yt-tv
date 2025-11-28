package com.example.yt_tv.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PlaylistChannelDto {
    private Long id;
    private Long playlistId;
    private Long channelId;
}
