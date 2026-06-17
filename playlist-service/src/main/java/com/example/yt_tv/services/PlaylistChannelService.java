package com.example.yt_tv.services;

import com.example.yt_tv.dtos.AddChannelToPlaylistDto;
import com.example.yt_tv.dtos.DtoMapper;
import com.example.yt_tv.dtos.PlaylistChannelDto;
import com.example.yt_tv.entities.Playlist;
import com.example.yt_tv.entities.PlaylistChannel;
import com.example.yt_tv.repositories.PlaylistChannelRepository;
import com.example.yt_tv.repositories.PlaylistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaylistChannelService {
    private final PlaylistChannelRepository playlistChannelRepository;
    private final PlaylistRepository playlistRepository;
    private final DtoMapper dtoMapper;

    @Transactional
    public PlaylistChannelDto addChannelToPlaylist(AddChannelToPlaylistDto addDto, Long userId) {
        if (playlistChannelRepository.existsByPlaylistIdAndChannelId(addDto.getPlaylistId(), addDto.getChannelId())) {
            throw new IllegalArgumentException("Channel is already in this playlist!");
        }

        Playlist playlist = playlistRepository.findByIdAndUserId(addDto.getPlaylistId(), userId)
                .orElseThrow(() -> {
                    log.warn("Unauthorized channel add attempt: playlistId={}, userId={}", addDto.getPlaylistId(), userId);
                    return new IllegalArgumentException("Playlist not found or access denied");
                });

        PlaylistChannel playlistChannel = new PlaylistChannel();
        playlistChannel.setPlaylist(playlist);
        playlistChannel.setChannelId(addDto.getChannelId());
        // For now, we don't have name/thumb in addDto. We might need to fetch from content-service.
        // Or assume the UI sends them.

        PlaylistChannel saved = playlistChannelRepository.save(playlistChannel);
        log.info("Channel added to playlist: playlistId={}, channelId={}, userId={}", addDto.getPlaylistId(), addDto.getChannelId(), userId);
        return dtoMapper.toPlaylistChannelDto(saved);
    }

    @Transactional
    public void removeChannelFromPlaylist(Long playlistChannelId, Long userId) {
        PlaylistChannel pc = playlistChannelRepository.findByIdAndUserId(playlistChannelId, userId)
                .orElseThrow(() -> {
                    log.warn("Unauthorized channel removal attempt: playlistChannelId={}, userId={}", playlistChannelId, userId);
                    return new IllegalArgumentException("PlaylistChannel not found or access denied");
                });

        playlistChannelRepository.deleteById(playlistChannelId);
        log.info("Channel removed from playlist: playlistChannelId={}, userId={}", playlistChannelId, userId);
    }

    @Transactional(readOnly = true)
    public List<PlaylistChannelDto> listByPlaylist(Long playlistId, Long userId) {
        // Verify playlist ownership
        playlistRepository.findByIdAndUserId(playlistId, userId)
                .orElseThrow(() -> {
                    log.warn("Unauthorized playlistChannel list attempt: playlistId={}, userId={}", playlistId, userId);
                    return new IllegalArgumentException("Playlist not found or access denied");
                });

        return playlistChannelRepository.findByPlaylistIdAndUserId(playlistId, userId).stream()
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
