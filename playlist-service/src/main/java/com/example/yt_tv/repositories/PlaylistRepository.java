package com.example.yt_tv.repositories;

import com.example.yt_tv.entities.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
    List<Playlist> findByUserId(Long userId);
    Optional<Playlist> findByIdAndUserId(Long id, Long userId);
}
