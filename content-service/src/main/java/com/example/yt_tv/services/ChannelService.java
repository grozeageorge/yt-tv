package com.example.yt_tv.services;

import com.example.yt_tv.clients.AiSyncClient;
import com.example.yt_tv.clients.AiCategorizeClient;
import com.example.yt_tv.dtos.*;
import com.example.yt_tv.entities.Channel;
import com.example.yt_tv.entities.Video;
import com.example.yt_tv.repositories.ChannelRepository;
import com.example.yt_tv.repositories.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChannelService {
    private final ChannelRepository channelRepository;
    private final DtoMapper dtoMapper;

    private final YoutubeApiClient youtubeApiClient;
    private final VideoRepository videoRepository;
    private final AiSyncClient aiSyncClient;
    private final AiCategorizeClient aiCategorizeClient;

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

        // Notify playlist-service to delete watched records? Or let it be.
        // For now, just clean up locally.

        videoRepository.deleteAll(videos);
        channelRepository.deleteById(id);
        log.info("Channel deleted: id={}", id);
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
            log.warn("Youtube returned 0 videos for channel={}", channel.getName());
        }

        if (channel.getCategory() == null || channel.getCategory().isEmpty()) {
            log.debug("Channel '{}' has no category set yet.", channel.getName());
        } else {
            log.info("Channel already categorized as {}", channel.getCategory());
        }

        int addedCount = 0;
        List<YoutubeVideoInfo> newVideosForAi = new ArrayList<>();

        List<Video> videosToSave = new ArrayList<>();
        for (YoutubeVideoInfo info : response.videos()) {
            Optional<Video> existing = videoRepository.findByYtVideoId(info.videoId());

            if (existing.isEmpty()) {
                Video video = toVideoEntity(channel, info);
                videosToSave.add(video);
                newVideosForAi.add(info);
                addedCount++;
            }
        }
        if (!videosToSave.isEmpty()) {
            videoRepository.saveAll(videosToSave);
        }

        // Attempt categorization if missing, using current fetched data
        if (channel.getCategory() == null || channel.getCategory().isBlank()) {
            try {
                List<String> hints = youtubeApiClient.getChannelTopics(channel.getYtChannelId());
                String description = youtubeApiClient.getChannelDescription(channel.getYtChannelId());

                ChannelCategorizeRequest categorizeRequest = ChannelCategorizeRequest.builder()
                        .channelName(channel.getName())
                        .description(description)
                        .sampleVideos(newVideosForAi.isEmpty() ? response.videos().stream().limit(5).toList() : newVideosForAi.stream().limit(5).toList())
                        .youtubeHints(hints)
                        .build();

                ChannelCategorizeResponse categorizeResponse = aiCategorizeClient.categorize(categorizeRequest);
                if (categorizeResponse != null && categorizeResponse.getCategory() != null && !categorizeResponse.getCategory().isBlank()) {
                    channel.setCategory(categorizeResponse.getCategory());
                    log.info("Applied AI category '{}' to channel '{}'", categorizeResponse.getCategory(), channel.getName());
                } else {
                    log.warn("AI categorization returned no category for channel '{}'", channel.getName());
                }
            } catch (Exception e) {
                log.warn("Failed to categorize channel '{}': {}", channel.getName(), e.getMessage());
            }
        }

        channel.setLastSync(Instant.now());
        channelRepository.save(channel);
        log.info("Synced channel {} with {} new videos", channel.getName(), addedCount);

        if (!newVideosForAi.isEmpty()) {
            try {
                aiSyncClient.addVideos(channel.getName(), channel.getCategory(), newVideosForAi);
            } catch (Exception e) {
                log.warn("Failed to send videos to AI service: {}", e.getMessage());
            }
        }
    }

    @Transactional
    public ChannelDto createAndSyncFromQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Query is required");
        }
        if (!youtubeApiClient.isApiKeyConfigured()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "YouTube API key not configured (GOOGLE_API_KEY)");
        }

        ChannelCreateDto dto = youtubeApiClient.searchChannel(query);

        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No channel found for query: " + query);
        }

        Optional<Channel> existing = channelRepository.findByYtChannelId(dto.getYtChannelId());

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

        List<Video> nextVideosToSave = new ArrayList<>();
        for (YoutubeVideoInfo videoInfo : response.videos()) {
            if (videoRepository.findByYtVideoId(videoInfo.videoId()).isEmpty()) {
                Video video = toVideoEntity(channel, videoInfo);
                nextVideosToSave.add(video);
            }
        }
        if (!nextVideosToSave.isEmpty()) {
            videoRepository.saveAll(nextVideosToSave);
        }

        channel.setNextPageToken(response.nextPageToken());
        if (channel.getUploadsPlaylistId() == null && response.uploadsPlaylistId() != null) {
            channel.setUploadsPlaylistId(response.uploadsPlaylistId());
        }

        channel.setLastSync(Instant.now());

        channelRepository.save(channel);
        log.info("Fetched next batch for channel {}. Next token present={}", channel.getName(), response.nextPageToken() != null);
    }

    private Video toVideoEntity(Channel channel, YoutubeVideoInfo info) {
        Video video = new Video();
        video.setChannel(channel);
        video.setYtVideoId(info.videoId());
        video.setTitle(info.title());
        video.setThumbnailUrl(info.thumbnail());
        return video;
    }
}
