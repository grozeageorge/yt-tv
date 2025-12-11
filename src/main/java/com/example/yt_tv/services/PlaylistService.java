package com.example.yt_tv.services;

import com.example.yt_tv.dtos.DtoMapper;
import com.example.yt_tv.dtos.PlaylistCreateDto;
import com.example.yt_tv.dtos.PlaylistDto;
import com.example.yt_tv.dtos.PlaylistUpdateDto;
import com.example.yt_tv.entities.*;
import com.example.yt_tv.repositories.PlaylistRepository;
import com.example.yt_tv.repositories.VideoRepository;
import com.example.yt_tv.repositories.WatchedVideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlaylistService {
    private final PlaylistRepository playlistRepository;
    private final VideoRepository videoRepository;
    private final WatchedVideoRepository watchedVideoRepository;
    private final DtoMapper dtoMapper;
    private final ChannelService channelService;


    @Transactional
    public PlaylistDto createPlaylist(User user, PlaylistCreateDto playlistCreateDto) {
        Playlist saved = playlistRepository.save(dtoMapper.toPlaylistEntity(user, playlistCreateDto));
        return dtoMapper.toPlaylistDto(saved);
    }

    @Transactional(readOnly = true)
    public PlaylistDto getPlaylist(Long id) {
        return playlistRepository.findById(id)
                .map(dtoMapper::toPlaylistDto)
                .orElseThrow(() -> new IllegalArgumentException("Playlist not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<PlaylistDto> getPlaylists() {
        return playlistRepository.findAll().stream()
                .map(dtoMapper::toPlaylistDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlaylistDto> getPlaylistsByUserId(Long userId) {
        return playlistRepository.findByUserId(userId).stream()
                .map(dtoMapper::toPlaylistDto)
                .toList();
    }

    @Transactional
    public PlaylistDto updatePlaylist(Long id, PlaylistUpdateDto playlistUpdateDto) {
        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Playlist not found with id: " + id));

        dtoMapper.updatePlaylistEntity(playlist, playlistUpdateDto);
        Playlist updated = playlistRepository.save(playlist);
        return dtoMapper.toPlaylistDto(updated);
    }

    @Transactional
    public void deletePlaylist(Long id) {
        playlistRepository.deleteById(id);
    }

    @Transactional
    public PlaylistDto addPlaylistChannel(Long playlistId, PlaylistChannel playlistChannel) {
        if (playlistId == null) {
            throw new IllegalArgumentException("Playlist id cannot be null");
        }

        if (playlistChannel == null || playlistChannel.getChannel() == null) {
            throw new IllegalArgumentException("Channel cannot be null");
        }

        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("Playlist not found with id: " + playlistId));

        Long newChannelId = playlistChannel.getChannel().getId();
        boolean alreadyPresent = playlist.getPlaylistChannels().stream()
                .anyMatch(pc -> pc.getChannel() != null && Objects.equals(pc.getChannel().getId(), newChannelId));

        if (alreadyPresent) {
            throw new IllegalArgumentException("Channel already present in playlist");
        }

        playlist.addPlaylistChannel(playlistChannel);
        Playlist saved = playlistRepository.save(playlist);
        return dtoMapper.toPlaylistDto(saved);
    }

    @Transactional
    public PlaylistDto removeChannelById(Long playlistId, Long channelId) {
        if (playlistId == null) {
            throw new IllegalArgumentException("Playlist id cannot be null");
        }

        if (channelId == null) {
            throw new IllegalArgumentException("Channel id cannot be null");
        }

        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("Playlist not found"));

        boolean removed = playlist.removeChannelById(channelId);
        if (!removed) {
            throw new IllegalArgumentException("Channel not found in playlist");
        }

        Playlist saved = playlistRepository.save(playlist);
        return dtoMapper.toPlaylistDto(saved);
    }

    @Transactional(readOnly = true)
    public List<String> getVideoQueue(Long playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("Playlist not found"));

        if (playlist.getPlaylistChannels().isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> channelIds = playlist.getPlaylistChannels().stream()
                .map(pc -> pc.getChannel().getId())
                .toList();

        // If this line is red, you need to add the method to VideoRepository (see Step 3)
        List<Video> allVideos = videoRepository.findByChannelIdIn(channelIds);

        Collections.shuffle(allVideos);

        return allVideos.stream()
                .map(Video::getYtVideoId)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<String> getVideoQueue(Long playlistId, Long userId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("Playlist not found"));

        if (playlist.getPlaylistChannels().isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> channelIds = playlist.getPlaylistChannels().stream()
                .map(pc -> pc.getChannel().getId())
                .toList();

        List<Video> dbVideos = videoRepository.findByChannelIdIn(channelIds);

        List<Long> watchedIds = watchedVideoRepository.findByUserId(userId).stream()
                .map(wv -> wv.getVideo().getId())
                .toList();

        List<String> queue = dbVideos.stream()
                .filter(v -> !watchedIds.contains(v.getId()))
                .map(Video::getYtVideoId)
                .collect(Collectors.toList());

        if (queue.isEmpty()) {
            System.out.println("Queue is empty! Attempting to fetch next batch from Youtube...");
            boolean newVideosFound = false;

            for (PlaylistChannel pc : playlist.getPlaylistChannels()) {
                Channel c = pc.getChannel();

                if (c.getNextPageToken() != null || dbVideos.isEmpty()) {
                    channelService.fetchNextBatch(c.getId());
                    newVideosFound = true;
                }
            }

            if (newVideosFound) {
                return getVideoQueue(playlistId, userId);
            }
        }

        Collections.shuffle(queue);
        return queue;
    }
}
