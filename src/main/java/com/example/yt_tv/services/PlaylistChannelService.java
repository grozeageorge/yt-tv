package com.example.yt_tv.services;

import com.example.yt_tv.dtos.AddChannelToPlaylistDto;
import com.example.yt_tv.dtos.DtoMapper;
import com.example.yt_tv.dtos.PlaylistChannelDto;
import com.example.yt_tv.entities.Channel;
import com.example.yt_tv.entities.Playlist;
import com.example.yt_tv.entities.PlaylistChannel;
import com.example.yt_tv.repositories.ChannelRepository;
import com.example.yt_tv.repositories.PlaylistChannelRepository;
import com.example.yt_tv.repositories.PlaylistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaylistChannelService {
    private final PlaylistChannelRepository playlistChannelRepository;
    private final PlaylistRepository playlistRepository;
    private final ChannelRepository channelRepository;
    private final DtoMapper dtoMapper;

    @Transactional
    public PlaylistChannelDto addChannelToPlaylist(AddChannelToPlaylistDto addDto) {
        Playlist playlist = playlistRepository.findById(addDto.getPlaylistId())
                .orElseThrow(() -> new IllegalArgumentException("Playlist not found"));
        Channel channel = channelRepository.findById(addDto.getChannelId())
                .orElseThrow(() -> new IllegalArgumentException("Channel not found"));

        PlaylistChannel playlistChannel = dtoMapper.toPlaylistChannelEntity(playlist, channel, addDto);
        PlaylistChannel saved = playlistChannelRepository.save(playlistChannel);
        return dtoMapper.toPlaylistChannelDto(saved);
    }

    @Transactional
    public void removeChannelFromPlaylist(Long playlistChannelId) {
        playlistChannelRepository.deleteById(playlistChannelId);
    }

    @Transactional(readOnly = true)
    public List<PlaylistChannelDto> listByPlaylist(Long playlistId) {
        return playlistChannelRepository.findByPlaylistId(playlistId).stream()
                .map(dtoMapper::toPlaylistChannelDto)
                .toList();

    }

    @Transactional(readOnly = true)
    public List<PlaylistChannelDto> listByChannel(Long channelId) {
        return playlistChannelRepository.findByChannelId(channelId).stream()
                .map(dtoMapper::toPlaylistChannelDto)
                .toList();
    }

}
