package com.example.yt_tv.services;

import com.example.yt_tv.dtos.DtoMapper;
import com.example.yt_tv.dtos.WatchedVideoDto;
import com.example.yt_tv.entities.User;
import com.example.yt_tv.entities.Video;
import com.example.yt_tv.entities.WatchedVideo;
import com.example.yt_tv.repositories.UserRepository;
import com.example.yt_tv.repositories.VideoRepository;
import com.example.yt_tv.repositories.WatchedVideoRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class WatchedVideoService {
    private final WatchedVideoRepository watchedVideoRepository;
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final DtoMapper dtoMapper;

    public WatchedVideoDto markWatched(Long userId, Long videoId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));

        WatchedVideo wv = watchedVideoRepository.findByUserIdAndVideoId(userId, videoId)
                .orElseGet(() -> {
                    WatchedVideo newWv = dtoMapper.toWatchedVideoEntity(user, video);
                    return newWv;
                });

        wv.setWatched(true);
        WatchedVideo saved = watchedVideoRepository.save(wv);
        return dtoMapper.toWatchedVideoDto(saved);
    }

    public WatchedVideoDto markSkipped(Long userId, Long videoId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));

        WatchedVideo wv = watchedVideoRepository.findByUserIdAndVideoId(userId, videoId)
                .orElseGet(() -> dtoMapper.toWatchedVideoEntity(user, video));

        wv.setWatched(false);
        WatchedVideo saved = watchedVideoRepository.save(wv);
        return dtoMapper.toWatchedVideoDto(saved);
    }

    @Transactional(readOnly = true)
    public boolean isPlayable(Long userId, Long videoId) {
        return watchedVideoRepository.findByUserIdAndVideoId(userId, videoId)
                .map(w-> !w.isWatched())
                .orElse(true);
    }

    @Transactional(readOnly = true)
    public List<WatchedVideoDto> getByUserId(Long userId) {
        return watchedVideoRepository.findByUserId(userId).stream()
                .map(dtoMapper::toWatchedVideoDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteWatched(Long userId, Long videoId) {
        watchedVideoRepository.deleteByUserIdAndVideoId(userId, videoId);
    }

    @Transactional(readOnly = true)
    public WatchedVideoDto getWatched(Long id) {
        WatchedVideo wv = watchedVideoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("WatchedVideo not found: " + id));
        return dtoMapper.toWatchedVideoDto(wv);
    }

    public WatchedVideoDto toggleWatched(Long id) {
        WatchedVideo wv = watchedVideoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("WatchedVideo not found: " + id));
        wv.setWatched(!wv.isWatched());
        WatchedVideo saved = watchedVideoRepository.save(wv);
        return dtoMapper.toWatchedVideoDto(saved);
    }

    @Transactional
    public void markWatchedByYtId(Long userId, String ytVideoId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Video video = videoRepository.findByYtVideoId(ytVideoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));

        WatchedVideo wv = watchedVideoRepository.findByUserIdAndVideoId(userId, video.getId())
                .orElseGet(() -> dtoMapper.toWatchedVideoEntity(user, video));

        wv.setWatched(true);
        watchedVideoRepository.save(wv);
        System.out.println("Marked video " + ytVideoId + " as watched by user " + user.getUsername());
    }
}

