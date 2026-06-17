package com.example.yt_tv.controllers;

import com.example.yt_tv.dtos.PlaylistCreateDto;
import com.example.yt_tv.dtos.PlaylistDto;
import com.example.yt_tv.services.PlaylistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
public class ApiPlaylistController {
    private final PlaylistService playlistService;

    @GetMapping("/user/{userId}")
    public List<PlaylistDto> list(@PathVariable Long userId) {
        return playlistService.getPlaylistsByUserId(userId);
    }

    @GetMapping("/{id}")
    public PlaylistDto get(@PathVariable Long id, @RequestParam Long userId) {
        return playlistService.getPlaylist(id, userId);
    }

    @PostMapping
    public PlaylistDto create(@RequestParam Long userId, @RequestBody PlaylistCreateDto dto) {
        return playlistService.createPlaylist(userId, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @RequestParam Long userId) {
        playlistService.deletePlaylist(id, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/add-channel")
    public PlaylistDto addChannel(
            @PathVariable Long id,
            @RequestParam Long channelId,
            @RequestParam String channelName,
            @RequestParam String thumb,
            @RequestParam Long userId) {
        return playlistService.addPlaylistChannel(id, channelId, channelName, thumb, userId);
    }

    @DeleteMapping("/{id}/remove-channel/{channelId}")
    public PlaylistDto removeChannel(
            @PathVariable Long id,
            @PathVariable Long channelId,
            @RequestParam Long userId) {
        return playlistService.removeChannelById(id, channelId, userId);
    }

    @DeleteMapping("/internal/channels/{channelId}")
    public ResponseEntity<Void> deleteChannelGlobal(@PathVariable Long channelId) {
        playlistService.deleteChannelFromAllPlaylists(channelId);
        return ResponseEntity.ok().build();
    }
}
