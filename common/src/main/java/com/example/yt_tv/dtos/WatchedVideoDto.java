package com.example.yt_tv.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchedVideoDto {
    private Long id;
    private Long userId;
    private Long videoId;
    private boolean watched;
}