package com.example.yt_tv.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AddChannelToPlaylistDto {
    private Long channelId;
    public Long playlistId;
}
