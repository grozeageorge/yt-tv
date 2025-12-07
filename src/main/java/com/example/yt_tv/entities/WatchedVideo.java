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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(nullable = false)
    private boolean watched = false;
}
