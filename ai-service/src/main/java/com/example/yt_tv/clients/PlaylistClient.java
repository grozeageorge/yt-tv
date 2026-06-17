package com.example.yt_tv.clients;

import com.example.yt_tv.dtos.PlaylistDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "playlist-service")
public interface PlaylistClient {
    @GetMapping("/api/watched/user/{userId}/ids")
    List<String> getWatchedYtVideoIds(@PathVariable("userId") Long userId);

    @GetMapping("/api/playlists/user/{userId}")
    List<PlaylistDto> getPlaylistsByUserId(@PathVariable("userId") Long userId);

    @GetMapping("/api/playlists/{id}")
    PlaylistDto getPlaylist(@PathVariable("id") Long id, @RequestParam("userId") Long userId);
}
