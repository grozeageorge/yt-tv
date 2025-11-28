package com.example.yt_tv.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PlaylistDto {

    private Long id;
    private String name;
    private Long userId;
    private List<PlaylistChannelDto> channels;
}
