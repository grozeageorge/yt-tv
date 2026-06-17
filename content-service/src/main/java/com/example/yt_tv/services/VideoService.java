package com.example.yt_tv.services;

import com.example.yt_tv.dtos.DtoMapper;
import com.example.yt_tv.dtos.VideoCreateDto;
import com.example.yt_tv.dtos.VideoDto;
import com.example.yt_tv.entities.Channel;
import com.example.yt_tv.entities.Video;
import com.example.yt_tv.repositories.ChannelRepository;
import com.example.yt_tv.repositories.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoService {
    private final VideoRepository videoRepository;
    private final ChannelRepository channelRepository;
    private final DtoMapper dtoMapper;

    @Transactional
    public VideoDto createVideo(VideoCreateDto videoCreateDto, Long channelId) {
        if (videoCreateDto == null) {
            throw new IllegalArgumentException("VideoCreateDto is required");
        }
        if (channelId == null) {
            throw new IllegalArgumentException("ChannelId is required");
        }

        Channel channel = getChannelOrThrow(channelId);

        Video video = dtoMapper.toVideoEntity(videoCreateDto, channel);
        Video saved = videoRepository.save(video);
        return dtoMapper.toVideoDto(saved);
    }

    @Transactional(readOnly = true)
    public VideoDto get(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Video not found with id: " + id));
        return dtoMapper.toVideoDto(video);
    }

    @Transactional(readOnly = true)
    public VideoDto getRandomVideoFromChannel(Long channelId) {
        return videoRepository.findRandomByChannelId(channelId)
                .map(dtoMapper::toVideoDto)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<VideoDto> getRandomVideosFromChannel(Long channelId, int limit) {
        return videoRepository.findRandomByChannelIdWithLimit(channelId, limit).stream()
                .map(dtoMapper::toVideoDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VideoDto> list() {
        return videoRepository.findAll().stream()
                .map(dtoMapper::toVideoDto)
                .toList();
    }

    @Transactional
    public VideoDto update(Long id, VideoCreateDto updateDto, Long channelId) {
        if (updateDto == null) {
            throw new IllegalArgumentException("VideoCreateDto is required");
        }

        Video video = getVideoOrThrow(id);

        if (channelId != null) {
            Channel channel = getChannelOrThrow(channelId);
            video.setChannel(channel);
        }

        video.setYtVideoId(updateDto.getYtVideoId());
        video.setTitle(updateDto.getTitle());
        video.setThumbnailUrl(updateDto.getThumbnailUrl());

        Video saved = videoRepository.save(video);
        return dtoMapper.toVideoDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        videoRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<VideoDto> listByChannel(Long channelId) {
        return videoRepository.findByChannelId(channelId).stream()
                .map(dtoMapper::toVideoDto)
                .toList();
    }

    private Video getVideoOrThrow(Long videoId) {
        return videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found with id: " + videoId));
    }

    private Channel getChannelOrThrow(Long channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found with id: " + channelId));
    }
}
