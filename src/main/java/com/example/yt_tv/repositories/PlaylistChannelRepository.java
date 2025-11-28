package com.example.yt_tv.repositories;

import com.example.yt_tv.entities.PlaylistChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlaylistChannelRepository extends JpaRepository<PlaylistChannel, Long> {

    List<PlaylistChannel> findByPlaylistId(Long playlistId);

    List<PlaylistChannel> findByChannelId(Long channelId);
}
