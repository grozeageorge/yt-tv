package com.example.yt_tv.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "playlists")
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class Playlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @OneToMany(
            mappedBy = "playlist",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<PlaylistChannel> playlistChannels = new ArrayList<>();

    public void addPlaylistChannel(PlaylistChannel playlistChannel) {
        if (playlistChannel == null) return;
        if (!playlistChannels.contains(playlistChannel)) {
            playlistChannels.add(playlistChannel);
            playlistChannel.setPlaylist(this);
        }
    }

    public void removePlaylistChannel(PlaylistChannel playlistChannel) {
        if (playlistChannel == null) return;
        if (playlistChannels.remove(playlistChannel)) {
            playlistChannel.setPlaylist(null);
        }
    }

    public boolean removeChannelById(Long channelId) {
        if (channelId == null) return false;
        Iterator<PlaylistChannel> it = playlistChannels.iterator();
        while (it.hasNext()) {
            PlaylistChannel pc = it.next();
            if (Objects.equals(pc.getChannelId(), channelId)) {
                it.remove();
                pc.setPlaylist(null);
                return true;
            }
        }
        return false;
    }

    public void clearPlaylistChannels() {
        for (PlaylistChannel pc : new ArrayList<>(playlistChannels)) {
            pc.setPlaylist(null);
        }
        playlistChannels.clear();
    }
}
