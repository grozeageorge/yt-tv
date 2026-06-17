package com.example.yt_tv.repositories;

import com.example.yt_tv.entities.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, Long> {
	Optional<Channel> findByYtChannelId(String ytChannelId);
}
