package com.example.yt_tv.dtos;

import com.example.yt_tv.entities.Channel;
import com.example.yt_tv.entities.Video;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
public class DtoMapper {
    public ChannelDto toChannelDto(Channel channel) {
        if (channel == null) return null;
        ChannelDto dto = new ChannelDto();
        dto.setId(channel.getId());
        dto.setYtChannelId(channel.getYtChannelId());
        dto.setName(channel.getName());
        dto.setThumbnailUrl(channel.getThumbnailUrl());
        dto.setLastSync(channel.getLastSync());
        return dto;
    }

    public Channel toChannelEntity(ChannelCreateDto dto) {
        if (dto == null) return null;
        Channel channel = new Channel();
        channel.setYtChannelId(dto.getYtChannelId());
        channel.setName(dto.getName());
        channel.setThumbnailUrl(dto.getThumbnailUrl());
        channel.setLastSync(Instant.now());
        return channel;
    }

    public VideoDto toVideoDto(Video video) {
        if (video == null) return null;
        VideoDto dto = new VideoDto();
        dto.setId(video.getId());
        dto.setTitle(video.getTitle());
        dto.setThumbnailUrl(video.getThumbnailUrl());
        dto.setYtVideoId(video.getYtVideoId());
        dto.setChannelId(video.getChannel() != null ? video.getChannel().getId() : null);
        return dto;
    }

    public Video toVideoEntity(VideoCreateDto dto, Channel channel) {
        if (dto == null) return null;
        Video video = new Video();
        video.setTitle(dto.getTitle());
        video.setThumbnailUrl(dto.getThumbnailUrl());
        video.setYtVideoId(dto.getYtVideoId());
        video.setChannel(channel);
        return video;
    }
}
