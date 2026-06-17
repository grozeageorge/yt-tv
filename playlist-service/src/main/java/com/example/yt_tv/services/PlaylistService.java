package com.example.yt_tv.services;

import com.example.yt_tv.dtos.DtoMapper;
import com.example.yt_tv.dtos.PlaylistCreateDto;
import com.example.yt_tv.dtos.PlaylistDto;
import com.example.yt_tv.dtos.PlaylistUpdateDto;
import com.example.yt_tv.entities.Playlist;
import com.example.yt_tv.entities.PlaylistChannel;
import com.example.yt_tv.repositories.PlaylistChannelRepository;
import com.example.yt_tv.repositories.PlaylistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaylistService {
    private final PlaylistRepository playlistRepository;
    private final PlaylistChannelRepository playlistChannelRepository;
    private final DtoMapper dtoMapper;

    @Transactional
    public PlaylistDto createPlaylist(Long userId, PlaylistCreateDto playlistCreateDto) {
        Playlist playlist = dtoMapper.toPlaylistEntity(playlistCreateDto);
        playlist.setUserId(userId);
        Playlist saved = playlistRepository.save(playlist);
        log.info("Playlist created: id={}, userId={}, name={}", saved.getId(), userId, saved.getName());
        return dtoMapper.toPlaylistDto(saved);
    }

    @Transactional(readOnly = true)
    public PlaylistDto getPlaylist(Long id, Long userId) {
        Playlist playlist = playlistRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> {
                    log.warn("Unauthorized playlist access attempt: playlistId={}, userId={}", id, userId);
                    return new IllegalArgumentException("Playlist not found or access denied");
                });
        return dtoMapper.toPlaylistDto(playlist);
    }

    @Transactional(readOnly = true)
    public List<PlaylistDto> getPlaylistsByUserId(Long userId) {
        return playlistRepository.findByUserId(userId).stream()
                .map(dtoMapper::toPlaylistDto)
                .toList();
    }

    @Transactional
    public PlaylistDto updatePlaylist(Long id, Long userId, PlaylistUpdateDto playlistUpdateDto) {
        Playlist playlist = playlistRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> {
                    log.warn("Unauthorized playlist update attempt: playlistId={}, userId={}", id, userId);
                    return new IllegalArgumentException("Playlist not found or access denied");
                });

        playlist.setName(playlistUpdateDto.getName());
        Playlist updated = playlistRepository.save(playlist);
        log.info("Playlist updated: id={}, userId={}, name={}", id, userId, updated.getName());
        return dtoMapper.toPlaylistDto(updated);
    }

    @Transactional
    public void deletePlaylist(Long id, Long userId) {
        Playlist playlist = playlistRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> {
                    log.warn("Unauthorized playlist deletion attempt: playlistId={}, userId={}", id, userId);
                    return new IllegalArgumentException("Playlist not found or access denied");
                });

        playlistRepository.deleteById(id);
        log.info("Playlist deleted: id={}, userId={}", id, userId);
    }

    @Transactional
    public void deleteChannelFromAllPlaylists(Long channelId) {
        playlistChannelRepository.deleteByChannelId(channelId);
        log.info("Channel removed from all playlists: channelId={}", channelId);
    }

    @Transactional
    public PlaylistDto addPlaylistChannel(Long playlistId, Long channelId, String channelName, String thumb, Long userId) {
        Playlist playlist = playlistRepository.findByIdAndUserId(playlistId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Playlist not found"));

        boolean alreadyPresent = playlist.getPlaylistChannels().stream()
                .anyMatch(pc -> Objects.equals(pc.getChannelId(), channelId));

        if (alreadyPresent) {
            throw new IllegalArgumentException("Channel already present in playlist");
        }

        PlaylistChannel pc = new PlaylistChannel();
        pc.setPlaylist(playlist);
        pc.setChannelId(channelId);
        pc.setChannelName(channelName);
        pc.setThumbnailUrl(thumb);
        playlist.getPlaylistChannels().add(pc);

        Playlist saved = playlistRepository.save(playlist);
        return dtoMapper.toPlaylistDto(saved);
    }

    @Transactional
    public PlaylistDto removeChannelById(Long playlistId, Long channelId, Long userId) {
        Playlist playlist = playlistRepository.findByIdAndUserId(playlistId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Playlist not found"));

        playlist.removeChannelById(channelId);
        Playlist saved = playlistRepository.save(playlist);
        return dtoMapper.toPlaylistDto(saved);
    }
}
