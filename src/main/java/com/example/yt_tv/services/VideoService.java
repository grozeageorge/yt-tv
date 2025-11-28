package com.example.yt_tv.services;

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

    @Transactional
    public Video create(Video data, Long channelId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found"));

        Video video = new Video();
        video.setYtVideoId(data.getYtVideoId());
        video.setTitle(data.getTitle());
        video.setThumbnailUrl(data.getThumbnailUrl());
        video.setChannel(channel);

        return videoRepository.save(video);
    }

    @Transactional(readOnly = true)
    public Video get(Long id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));
    }

    @Transactional(readOnly = true)
    public List<Video> list() {
        return videoRepository.findAll();
    }

    @Transactional
    public Video update(Long id, Video updatedVideo, Long channelId) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));

        if (channelId != null){
            Channel channel = channelRepository.findById(channelId)
                    .orElseThrow(() -> new IllegalArgumentException("Channel not found"));
            video.setChannel(channel);
        }

        video.setYtVideoId(updatedVideo.getYtVideoId());
        video.setTitle(updatedVideo.getTitle());
        video.setThumbnailUrl(updatedVideo.getThumbnailUrl());

        return videoRepository.save(video);
    }

    @Transactional
    public void delete(Long id) {
        videoRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Video> listByChannel(Long channelId) {
        return videoRepository.findByChannelId(channelId);
    }
}
