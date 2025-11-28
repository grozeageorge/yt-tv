package com.example.yt_tv.services;

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

@Service
@Transactional
@RequiredArgsConstructor
public class WatchedVideoService {
    private final WatchedVideoRepository watchedVideoRepository;
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;

    public WatchedVideo markWatched(Long userId, Long videoId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));

        WatchedVideo wv = watchedVideoRepository.findByUserIdAndVideoId(userId, videoId)
                .orElseGet(() -> {
                    WatchedVideo newWv = new WatchedVideo();
                    newWv.setUser(user);
                    newWv.setVideo(video);
                    return newWv;
                });

        wv.setWatched(true);
        return watchedVideoRepository.save(wv);
    }

    public void markSkipped(Long userId, Long videoId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));

        WatchedVideo wv = watchedVideoRepository.findByUserIdAndVideoId(userId, videoId)
                .orElseGet(() -> {
                    WatchedVideo newWv = new WatchedVideo();
                    newWv.setUser(user);
                    newWv.setVideo(video);
                    return newWv;
                });
        wv.setWatched(false);
        watchedVideoRepository.save(wv);
    }

    @Transactional(readOnly = true)
    public boolean isPlayable(Long userId, Long videoId) {
        return watchedVideoRepository.findByUserIdAndVideoId(userId, videoId)
                .map(w-> !w.isWatched())
                .orElse(true);
    }

    @Transactional(readOnly = true)
    public List<WatchedVideo> getByUserId(Long userId) {
        return watchedVideoRepository.findByUserId(userId);
    }

    public void deleteWatched(Long userId, Long videoId) {
        watchedVideoRepository.deleteByUserIdAndVideoId(userId, videoId);
    }

}

