package com.example.yt_tv.dtos;

import com.example.yt_tv.entities.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class DtoMapper {
    public UserDto toUserDto(User user) {
        if (user == null)
            return null;
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setUsername(user.getUsername());
        userDto.setEmail(user.getEmail());
        return userDto;
    }

    public User toUserEntity(UserCreateDto userCreateDto) {
        if (userCreateDto == null)
            return null;
        User user = new User();
        user.setUsername(userCreateDto.getUsername());
        user.setEmail(userCreateDto.getEmail());
        user.setPassword(userCreateDto.getPassword());
        return user;
    }

    public ChannelDto toChannelDto(Channel channel) {
        if (channel == null)
            return null;
        ChannelDto channelDto = new ChannelDto();
        channelDto.setId(channel.getId());
        channelDto.setYtChannelId(channel.getYtChannelId());
        channelDto.setName(channel.getName());
        channelDto.setThumbnailUrl(channel.getThumbnailUrl());
        channelDto.setLastSync(channel.getLastSync());
        return channelDto;

    }

    public Channel toChannelEntity(ChannelCreateDto dto) {
        if (dto == null)
            return null;
        Channel channel = new Channel();
        channel.setYtChannelId(dto.getYtChannelId());
        channel.setName(dto.getName());
        channel.setThumbnailUrl(dto.getThumbnailUrl());
        channel.setLastSync(Instant.now());
        return channel;
    }

    public PlaylistChannel toPlaylistChannelEntity(Playlist playlist, Channel channel, AddChannelToPlaylistDto addDto) {
        if (playlist == null || channel == null || addDto == null)
            return null;
        PlaylistChannel playlistChannel = new PlaylistChannel();
        playlistChannel.setPlaylist(playlist);
        playlistChannel.setChannel(channel);
        return playlistChannel;
    }

    public PlaylistChannelDto toPlaylistChannelDto(PlaylistChannel playlistChannel) {
        if (playlistChannel == null)
            return null;
        PlaylistChannelDto dto = new PlaylistChannelDto();
        dto.setId(playlistChannel.getId());

        if (playlistChannel.getPlaylist() != null) {
            dto.setPlaylistId(playlistChannel.getPlaylist().getId());
        }

        if (playlistChannel.getChannel() != null) {
            dto.setChannelId(playlistChannel.getChannel().getId());
            dto.setChannelName(playlistChannel.getChannel().getName());
            dto.setThumbnailUrl(playlistChannel.getChannel().getThumbnailUrl());
        }

        return dto;
    }

    public Playlist toPlaylistEntity(User user,PlaylistCreateDto playlistCreateDto) {
        if (playlistCreateDto == null)
            return null;
        Playlist playlist = new Playlist();
        playlist.setName(playlistCreateDto.getName());
        playlist.setUser(user);
        playlist.setPlaylistChannels(new ArrayList<PlaylistChannel>());
        return playlist;
    }

    public PlaylistDto toPlaylistDto(Playlist saved) {
        if (saved == null) return null;
        PlaylistDto dto = new PlaylistDto();
        dto.setId(saved.getId());
        dto.setName(saved.getName());

        // Add user ID mapping if needed for UI
        if (saved.getUser() != null) {
            dto.setUserId(saved.getUser().getId());
        }

        // MAP THE CHANNELS
        if (saved.getPlaylistChannels() != null) {
            List<PlaylistChannelDto> channelDtos = saved.getPlaylistChannels().stream()
                    .map(this::toPlaylistChannelDto)
                    .toList();
            dto.setChannels(channelDtos);
        }
        return dto;
    }

    public void updatePlaylistEntity(Playlist playlist, PlaylistUpdateDto playlistUpdateDto) {
        if (playlist == null || playlistUpdateDto == null)
            return;
        playlist.setName(playlistUpdateDto.getName());
    }

    public VideoDto toVideoDto(Video video) {
        if (video == null)
            return null;
        VideoDto dto = new VideoDto();
        dto.setId(video.getId());
        dto.setTitle(video.getTitle());
        dto.setThumbnailUrl(video.getThumbnailUrl());
        dto.setYtVideoId(video.getYtVideoId());
        dto.setChannelId(video.getChannel() != null ? video.getChannel().getId() : null);
        return dto;
    }

    public Video toVideoEntity(VideoCreateDto videoCreateDto, Channel channel) {
        if (videoCreateDto == null)
            return null;

        Video video = new Video();
        video.setTitle(videoCreateDto.getTitle());
        video.setThumbnailUrl(videoCreateDto.getThumbnailUrl());
        video.setYtVideoId(videoCreateDto.getYtVideoId());
        video.setChannel(channel);
        return video;
    }

    public WatchedVideoDto toWatchedVideoDto(WatchedVideo watchedVideo) {
        if (watchedVideo == null)
            return null;
        WatchedVideoDto dto = new WatchedVideoDto();
        dto.setId(watchedVideo.getId());
        dto.setVideoId(watchedVideo.getVideo() != null ? watchedVideo.getVideo().getId() : null);
        dto.setUserId(watchedVideo.getUser() != null ? watchedVideo.getUser().getId() : null);
        dto.setWatched(watchedVideo.isWatched());
        return dto;
    }

    public WatchedVideo toWatchedVideoEntity(User user, Video video) {
        if (user == null || video == null)
            return null;
        WatchedVideo watchedVideo = new WatchedVideo();
        watchedVideo.setUser(user);
        watchedVideo.setVideo(video);
        watchedVideo.setWatched(false);
        return watchedVideo;
    }
}
