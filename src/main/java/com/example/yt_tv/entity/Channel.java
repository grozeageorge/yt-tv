package com.example.yt_tv.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "channels")
@Getter @Setter @NoArgsConstructor
public class Channel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false, unique = true)
    private String ytChannelId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String thumbnailUrl;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private Instant lastSync;
}
