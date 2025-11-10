package com.example.yt_tv.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WatchedVideoRepository extends JpaRepository<WatchedVideo, Long> {

}
