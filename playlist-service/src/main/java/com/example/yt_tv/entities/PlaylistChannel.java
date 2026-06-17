package com.example.yt_tv.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "playlist_channels")
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class PlaylistChannel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "playlist_id")
    private Playlist playlist;

    @Column(name = "channel_id")
    private Long channelId;

    @Column(name = "channel_name")
    private String channelName;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;
}
