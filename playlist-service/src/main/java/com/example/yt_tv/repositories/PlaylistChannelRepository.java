package com.example.yt_tv.repositories;

import com.example.yt_tv.entities.PlaylistChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlaylistChannelRepository extends JpaRepository<PlaylistChannel, Long> {

    List<PlaylistChannel> findByPlaylistId(Long playlistId);

    List<PlaylistChannel> findByChannelId(Long channelId);

    boolean existsByPlaylistIdAndChannelId(Long playlistId, Long channelId);

    @Query("SELECT pc FROM PlaylistChannel pc WHERE pc.playlist.id = :playlistId AND pc.playlist.userId = :userId")
    List<PlaylistChannel> findByPlaylistIdAndUserId(@Param("playlistId") Long playlistId, @Param("userId") Long userId);

    @Query("SELECT pc FROM PlaylistChannel pc WHERE pc.id = :id AND pc.playlist.userId = :userId")
    Optional<PlaylistChannel> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    void deleteByChannelId(Long channelId);
}
