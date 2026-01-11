package com.example.yt_tv.services;

import com.example.yt_tv.dtos.*;
import com.example.yt_tv.entities.Channel;
import com.example.yt_tv.entities.PlaylistChannel;
import com.example.yt_tv.entities.Video;
import com.example.yt_tv.repositories.ChannelRepository;
import com.example.yt_tv.repositories.PlaylistChannelRepository;
import com.example.yt_tv.repositories.VideoRepository;
import com.example.yt_tv.repositories.WatchedVideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
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
    private final WatchedVideoRepository watchedVideoRepository;

    private final AiCategorizerService aiCategorizerService;
    private final AiSyncService aiSyncService;

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

        for (Video video : videos) {
            watchedVideoRepository.deleteByVideoId(video.getId());
        }

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

        YoutubeApiResponse response = youtubeApiClient.fetchVideos(
                channel.getYtChannelId(),
                channel.getUploadsPlaylistId(),
                null
        );

        channel.setNextPageToken(response.nextPageToken());
        if (channel.getUploadsPlaylistId() == null) {
            channel.setUploadsPlaylistId(response.uploadsPlaylistId());
        }

        if (response.videos().isEmpty()) {
            System.out.println("AI Warning: Youtube returned 0 videos for: " + channel.getName());
        }

        if (channel.getCategory() == null || channel.getCategory().isEmpty()) {
            if (!response.videos().isEmpty()) {
                try {
                    System.out.println("AI: Categorizing channel " + channel.getName() + "...");
                    List<String> hints = youtubeApiClient.getChannelTopics(channel.getYtChannelId());

                    String category = aiCategorizerService.categorizeChannel(channel.getName(), response.videos(), hints);

                    channel.setCategory(category);
                    System.out.println("AI: Categorized as -> " + category);
                } catch (Exception e) {
                    System.err.println("AI Categorization failed: " + e.getMessage());
                    channel.setCategory("Entertainment");
                }
            }
        } else {
            System.out.println("AI: Channel already has category: " + channel.getCategory());
        }

        int addedCount = 0;
        List<YoutubeVideoInfo> newVideosForAi = new ArrayList<>();

        for (YoutubeVideoInfo info : response.videos()) {
            Optional<Video> existing = videoRepository.findByYtVideoId(info.videoId());

            if (existing.isEmpty()) {
                Video video = new Video();
                video.setChannel(channel);
                video.setYtVideoId(info.videoId());
                video.setTitle(info.title());
                video.setThumbnailUrl(info.thumbnail());
                videoRepository.save(video);
                newVideosForAi.add(info);
                addedCount++;
            }
        }

        channel.setLastSync(Instant.now());
        channelRepository.save(channel);
        System.out.println("Synced channel " + channel.getName() + ": Added " + addedCount + " new videos.");

        if (!newVideosForAi.isEmpty()) {
            try {
                aiSyncService.addVideosToVectorDb(newVideosForAi, channel.getName(), channel.getCategory());
            } catch (Exception e) {
                System.err.println("Warning: AI Ingestion failed: " + e.getMessage());
            }
        } else {
            System.out.println("AI: No new videos found for channel " + channel.getName());
        }
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

    @Transactional
    public void fetchNextBatch(Long channelId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found with id: " + channelId));

        YoutubeApiResponse response = youtubeApiClient.fetchVideos(
                channel.getYtChannelId(),
                channel.getUploadsPlaylistId(),
                channel.getNextPageToken()
        );

        for (YoutubeVideoInfo videoInfo : response.videos()) {
            if (videoRepository.findByYtVideoId(videoInfo.videoId()).isEmpty()) {
                Video video = new Video();
                video.setChannel(channel);
                video.setYtVideoId(videoInfo.videoId());
                video.setTitle(videoInfo.title());
                video.setThumbnailUrl(videoInfo.thumbnail());
                videoRepository.save(video);
            }
        }

        channel.setNextPageToken(response.nextPageToken());
        if (channel.getUploadsPlaylistId() == null && response.uploadsPlaylistId() != null) {
            channel.setUploadsPlaylistId(response.uploadsPlaylistId());
        }

        channel.setLastSync(Instant.now());

        channelRepository.save(channel);
        System.out.println("Fetched next batch for " + channel.getName() + ". Next Token: " + response.nextPageToken());
    }
}
