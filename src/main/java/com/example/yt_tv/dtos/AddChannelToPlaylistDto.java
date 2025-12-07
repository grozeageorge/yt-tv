package com.example.yt_tv.dtos;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class AddChannelToPlaylistDto {
    private Long channelId;
    public Long playlistId;
}
