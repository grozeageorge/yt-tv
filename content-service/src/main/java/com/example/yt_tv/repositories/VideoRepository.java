package com.example.yt_tv.repositories;

import com.example.yt_tv.entities.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {
    List<Video> findByChannelId(Long channelId);

    Optional<Video> findByYtVideoId(String ytVideoId);

    @Query(value = "SELECT TOP 1 * FROM videos WHERE channel_id = :channelId ORDER BY NEWID()", nativeQuery = true)
    Optional<Video> findRandomByChannelId(@Param("channelId") Long channelId);

    @Query(value = "SELECT * FROM videos WHERE channel_id = :channelId ORDER BY NEWID() OFFSET 0 ROWS FETCH NEXT :limit ROWS ONLY", nativeQuery = true)
    List<Video> findRandomByChannelIdWithLimit(@Param("channelId") Long channelId, @Param("limit") int limit);

    List<Video> findByChannelIdIn(List<Long> channelIds);
}
