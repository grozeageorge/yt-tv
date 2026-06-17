package com.example.yt_tv.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "watched_videos")
@Getter @Setter @NoArgsConstructor @Builder @AllArgsConstructor
public class WatchedVideo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "video_id", nullable = false)
    private Long videoId;

    @Column(name = "yt_video_id", nullable = false)
    private String ytVideoId;

    @Column(nullable = false)
    private boolean watched = false;
}
