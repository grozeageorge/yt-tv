package com.example.yt_tv.dtos;

import com.example.yt_tv.entities.*;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class DtoMapper {
    public PlaylistDto toPlaylistDto(Playlist playlist) {
        if (playlist == null) return null;
        PlaylistDto dto = new PlaylistDto();
        dto.setId(playlist.getId());
        dto.setName(playlist.getName());
        if (playlist.getUserId() != null) {
            dto.setUserId(playlist.getUserId());
        }
        if (playlist.getPlaylistChannels() != null) {
            dto.setChannels(playlist.getPlaylistChannels().stream()
                    .map(this::toPlaylistChannelDto)
                    .toList());
        }
        return dto;
    }

    public PlaylistChannelDto toPlaylistChannelDto(PlaylistChannel pc) {
        if (pc == null) return null;
        PlaylistChannelDto dto = new PlaylistChannelDto();
        dto.setId(pc.getId());
        dto.setPlaylistId(pc.getPlaylist() != null ? pc.getPlaylist().getId() : null);
        dto.setChannelId(pc.getChannelId());
        dto.setChannelName(pc.getChannelName());
        dto.setThumbnailUrl(pc.getThumbnailUrl());
        return dto;
    }

    public Playlist toPlaylistEntity(PlaylistCreateDto dto) {
        if (dto == null) return null;
        Playlist playlist = new Playlist();
        playlist.setName(dto.getName());
        playlist.setPlaylistChannels(new ArrayList<>());
        return playlist;
    }

    public WatchedVideoDto toWatchedVideoDto(WatchedVideo wv) {
        if (wv == null) return null;
        WatchedVideoDto dto = new WatchedVideoDto();
        dto.setId(wv.getId());
        dto.setVideoId(wv.getVideoId());
        dto.setUserId(wv.getUserId());
        dto.setWatched(wv.isWatched());
        return dto;
    }
}
