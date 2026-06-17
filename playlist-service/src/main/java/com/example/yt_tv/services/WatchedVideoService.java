package com.example.yt_tv.services;

import com.example.yt_tv.dtos.DtoMapper;
import com.example.yt_tv.dtos.WatchedVideoDto;
import com.example.yt_tv.entities.WatchedVideo;
import com.example.yt_tv.repositories.WatchedVideoRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class WatchedVideoService {
    private final WatchedVideoRepository watchedVideoRepository;
    private final DtoMapper dtoMapper;

    public WatchedVideoDto markWatched(Long userId, Long videoId, String ytVideoId) {
        WatchedVideo wv = getOrCreateWatchedVideo(userId, videoId, ytVideoId);
        wv.setWatched(true);
        WatchedVideo saved = watchedVideoRepository.save(wv);
        return dtoMapper.toWatchedVideoDto(saved);
    }

    public WatchedVideoDto markSkipped(Long userId, Long videoId, String ytVideoId) {
        WatchedVideo wv = getOrCreateWatchedVideo(userId, videoId, ytVideoId);
        wv.setWatched(false);
        WatchedVideo saved = watchedVideoRepository.save(wv);
        return dtoMapper.toWatchedVideoDto(saved);
    }

    @Transactional(readOnly = true)
    public List<String> getWatchedYtVideoIds(Long userId) {
        return watchedVideoRepository.findWatchedYtVideoIdsByUserId(userId);
    }

    @Transactional(readOnly = true)
    public boolean isPlayable(Long userId, Long videoId) {
        return watchedVideoRepository.findByUserIdAndVideoId(userId, videoId)
                .map(w -> !w.isWatched())
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
        WatchedVideo wv = watchedVideoRepository.findByUserIdAndVideoId(userId, 0L) // Simplified or handle differently
                .orElseGet(() -> {
                    WatchedVideo v = new WatchedVideo();
                    v.setUserId(userId);
                    v.setYtVideoId(ytVideoId);
                    v.setVideoId(0L); // Placeholder if we don't have internal ID
                    return v;
                });

        wv.setWatched(true);
        watchedVideoRepository.save(wv);
        log.info("Marked video as watched: ytVideoId={}, userId={}", ytVideoId, userId);
    }

    private WatchedVideo getOrCreateWatchedVideo(Long userId, Long videoId, String ytVideoId) {
        return watchedVideoRepository.findByUserIdAndVideoId(userId, videoId)
                .orElseGet(() -> {
                    WatchedVideo wv = new WatchedVideo();
                    wv.setUserId(userId);
                    wv.setVideoId(videoId);
                    wv.setYtVideoId(ytVideoId);
                    wv.setWatched(false);
                    return wv;
                });
    }
}

