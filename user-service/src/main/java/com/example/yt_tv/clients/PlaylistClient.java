package com.example.yt_tv.clients;

import com.example.yt_tv.dtos.PlaylistCreateDto;
import com.example.yt_tv.dtos.PlaylistDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "playlist-service")
public interface PlaylistClient {
    @GetMapping("/api/playlists/user/{userId}")
    List<PlaylistDto> listPlaylists(@PathVariable("userId") Long userId);

    @GetMapping("/api/playlists/{id}")
    PlaylistDto getPlaylist(@PathVariable("id") Long id, @RequestParam("userId") Long userId);

    @PostMapping("/api/playlists")
    PlaylistDto createPlaylist(@RequestParam("userId") Long userId, @RequestBody PlaylistCreateDto dto);

    @DeleteMapping("/api/playlists/{id}")
    void deletePlaylist(@PathVariable("id") Long id, @RequestParam("userId") Long userId);

    @PostMapping("/api/playlists/{id}/add-channel")
    PlaylistDto addChannelToPlaylist(
            @PathVariable("id") Long id,
            @RequestParam("channelId") Long channelId,
            @RequestParam("channelName") String channelName,
            @RequestParam("thumb") String thumb,
            @RequestParam("userId") Long userId);

    @DeleteMapping("/api/playlists/{id}/remove-channel/{channelId}")
    PlaylistDto removeChannelFromPlaylist(
            @PathVariable("id") Long id,
            @PathVariable("channelId") Long channelId,
            @RequestParam("userId") Long userId);

    @GetMapping("/api/watched/user/{userId}/ids")
    List<String> getWatchedYtVideoIds(@PathVariable("userId") Long userId);

    @PostMapping("/api/watched/{ytVideoId}")
    void markAsWatched(@PathVariable("ytVideoId") String ytVideoId, @RequestParam("userId") Long userId);

    @DeleteMapping("/api/playlists/internal/channels/{channelId}")
    void deleteChannelGlobal(@PathVariable("channelId") Long channelId);
}
