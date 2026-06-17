package com.example.yt_tv.dtos;

import java.util.List;

public record YoutubeApiResponse(
        List<YoutubeVideoInfo> videos,
        String nextPageToken,
        String uploadsPlaylistId
) {}
