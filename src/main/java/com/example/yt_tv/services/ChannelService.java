package com.example.yt_tv.services;

import com.example.yt_tv.dtos.ChannelCreateDto;
import com.example.yt_tv.dtos.ChannelDto;
import com.example.yt_tv.dtos.DtoMapper;
import com.example.yt_tv.dtos.YoutubeVideoInfo;
import com.example.yt_tv.entities.Channel;
import com.example.yt_tv.entities.PlaylistChannel;
import com.example.yt_tv.entities.Video;
import com.example.yt_tv.repositories.ChannelRepository;
import com.example.yt_tv.repositories.PlaylistChannelRepository;
import com.example.yt_tv.repositories.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChannelService {
    private final ChannelRepository channelRepository;
    private final DtoMapper dtoMapper;

    private final YoutubeApiClient youtubeApiClient;
    private final VideoRepository videoRepository;
    private final PlaylistChannelRepository playlistChannelRepository;

    @Transactional
    public ChannelDto create(ChannelCreateDto channelCreateDto) {
        Channel saved = channelRepository.save(dtoMapper.toChannelEntity(channelCreateDto));
        return dtoMapper.toChannelDto(saved);
    }

    @Transactional(readOnly = true)
    public ChannelDto get(Long id) {
        return channelRepository.findById(id)
                .map(dtoMapper::toChannelDto)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<ChannelDto> list() {
        return channelRepository.findAll().stream()
                .map(dtoMapper::toChannelDto)
                .toList();
    }

    @Transactional
    public ChannelDto update(Long id, ChannelCreateDto channelCreateDto) {
        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found with id: " + id));
        channel.setYtChannelId(channelCreateDto.getYtChannelId());
        channel.setName(channelCreateDto.getName());
        channel.setThumbnailUrl(channelCreateDto.getThumbnailUrl());
        Channel updated = channelRepository.save(channel);
        return dtoMapper.toChannelDto(updated);
    }

    @Transactional
    public void delete(Long id) {
        List<Video> videos = videoRepository.findByChannelId(id);
        videoRepository.deleteAll(videos);

        List<PlaylistChannel> playlistChannels = playlistChannelRepository.findByChannelId(id);
        playlistChannelRepository.deleteAll(playlistChannels);

        channelRepository.deleteById(id);
    }

    @Transactional
    public ChannelDto updateLastSync(Long id, Instant newSync) {
        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found with id: " + id));
        channel.setLastSync(newSync);
        Channel updated = channelRepository.save(channel);
        return dtoMapper.toChannelDto(updated);
    }

    @Transactional
    public void syncChannelVideos(Long channelId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found with id: " + channelId));

        List<YoutubeVideoInfo> ytVideos = youtubeApiClient.fetchLatestVideos(channel.getYtChannelId());

        int addedCount = 0;
        for (YoutubeVideoInfo info : ytVideos) {
            Optional<Video> existing = videoRepository.findByYtVideoId(info.videoId());

            if (existing.isEmpty()) {
                Video video = new Video();
                video.setChannel(channel);
                video.setYtVideoId(info.videoId());
                video.setTitle(info.title());
                video.setThumbnailUrl(info.thumbnail());
                videoRepository.save(video);
                addedCount++;
            }
        }

        channel.setLastSync(Instant.now());
        channelRepository.save(channel);
        System.out.println("Synced channel " + channel.getName() + ": Added " + addedCount + " new videos.");
    }

    @Transactional
    public ChannelDto createAndSyncFromQuery(String query) {
        ChannelCreateDto dto = youtubeApiClient.searchChannel(query);

        if (dto == null) {
            throw new IllegalArgumentException("No channel found by the Youtube API for query: " + query);
        }

        Optional<Channel> existing = channelRepository.findAll().stream()
                .filter(c -> c.getYtChannelId().equals(dto.getYtChannelId()))
                .findFirst();

        ChannelDto channelDto;
        if (existing.isPresent()) {
            channelDto = dtoMapper.toChannelDto(existing.get());
        } else {
            channelDto = create(dto);
        }

        syncChannelVideos(channelDto.getId());

        return channelDto;
    }

}
