package com.example.yt_tv.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "videos")
@Getter @Setter @NoArgsConstructor @Builder @AllArgsConstructor
public class Video {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String ytVideoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String thumbnailUrl;
}
