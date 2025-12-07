package com.example.yt_tv;

import com.example.yt_tv.dtos.*;
import com.example.yt_tv.entities.*;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@NoArgsConstructor
public final class TestDataFactory {

    public static User user(Long id, String username, String email, String password) {
        return User.builder()
                .username(username)
                .email(email)
                .password(password)
                .id(1L)
                .build();
    }

    public static UserCreateDto userCreateDto(String username, String email, String password) {
        return UserCreateDto.builder()
                .username(username)
                .email(email)
                .password(password)
                .build();
    }

    public static Channel channel(Long id, String name, String ytChannelId, String thumbnailUrl, Instant sync, List<PlaylistChannel> playlistChannels) {
        return Channel.builder()
                .id(id)
                .name(name)
                .ytChannelId(ytChannelId)
                .thumbnailUrl(thumbnailUrl)
                .lastSync(sync)
                .playlistChannels(playlistChannels)
                .build();
    }

    public static ChannelCreateDto channelCreateDto(String name, String ytChannelId, String thumbnailUrl) {
        return ChannelCreateDto.builder()
                .name(name)
                .ytChannelId(ytChannelId)
                .thumbnailUrl(thumbnailUrl)
                .build();
    }

    public static Playlist playlist(Long id, String name, User user, List<PlaylistChannel> playlistChannels) {
        return Playlist.builder()
                .id(id)
                .name(name)
                .user(user)
                .playlistChannels(playlistChannels)
                .build();
    }

    public static PlaylistCreateDto playlistCreateDto(String name) {
        return PlaylistCreateDto.builder()
                .name(name)
                .build();
    }

    public static PlaylistUpdateDto playlistUpdateDto(String name) {
        return PlaylistUpdateDto.builder()
                .name(name)
                .build();
    }

    public static PlaylistChannel playlistChannel(Long id, Playlist playlist, Channel channel) {
        return PlaylistChannel.builder()
                .id(id)
                .playlist(playlist)
                .channel(channel)
                .build();
    }

    public static AddChannelToPlaylistDto addChannelToPlaylistDto(Long channelId, Long playlistId) {
        return AddChannelToPlaylistDto.builder()
                .channelId(channelId)
                .playlistId(playlistId)
                .build();
    }

    public static Video video(Long id, String title, String thumbnailUrl, String ytVideoId, Channel channel) {
        return Video.builder()
                .id(id)
                .title(title)
                .ytVideoId(ytVideoId)
                .thumbnailUrl(thumbnailUrl)
                .channel(channel)
                .build();
    }

    public static WatchedVideo watchedVideo(Long id, User user, Video video) {
        return WatchedVideo.builder()
                .id(id)
                .video(video)
                .user(user)
                .watched(false)
                .build();
    }
}
