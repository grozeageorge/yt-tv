package com.example.yt_tv.repositories;

import com.example.yt_tv.entities.WatchedVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchedVideoRepository extends JpaRepository<WatchedVideo, Long> {
    Optional<WatchedVideo> findByUserIdAndVideoId(Long userId, Long videoId);
    List<WatchedVideo> findByUserId(Long userId);

    @Query("select wv.ytVideoId from WatchedVideo wv where wv.userId = :userId and wv.watched = true")
    List<String> findWatchedYtVideoIdsByUserId(@Param("userId") Long userId);

    void deleteByUserIdAndVideoId(Long userId, Long videoId);
    void deleteByVideoId(Long videoId);
}
