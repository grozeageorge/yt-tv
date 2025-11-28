package com.example.yt_tv.dtos;

import com.example.yt_tv.entities.Channel;
import com.example.yt_tv.entities.Playlist;
import com.example.yt_tv.entities.PlaylistChannel;
import com.example.yt_tv.entities.User;
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
        dto.setPlaylistId(playlistChannel.getPlaylist() != null ? playlistChannel.getPlaylist().getId() : null);
        dto.setChannelId(playlistChannel.getChannel() != null ? playlistChannel.getChannel().getId() : null);
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
        if (saved == null)
            return null;
        PlaylistDto dto = new PlaylistDto();
        dto.setId(saved.getId());
        dto.setName(saved.getName());
        return dto;
    }

    public void updatePlaylistEntity(Playlist playlist, PlaylistUpdateDto playlistUpdateDto) {
        if (playlist == null || playlistUpdateDto == null)
            return;
        playlist.setName(playlistUpdateDto.getName());
    }
}
