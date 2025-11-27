package com.example.yt_tv.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "channel")
    private List<PlaylistChannel> playlistChannels = new ArrayList<>();

    @Column(nullable = false)
    private Instant lastSync;
}
