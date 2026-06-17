package com.example.yt_tv.controllers;

import com.example.yt_tv.services.WatchedVideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/watched")
@RequiredArgsConstructor
public class ApiWatchedController {
    private final WatchedVideoService watchedVideoService;

    @PostMapping("/{ytVideoId}")
    public ResponseEntity<Void> markAsWatched(@PathVariable String ytVideoId, @RequestParam Long userId) {
        watchedVideoService.markWatchedByYtId(userId, ytVideoId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/{userId}/ids")
    public List<String> getWatchedYtVideoIds(@PathVariable Long userId) {
        return watchedVideoService.getWatchedYtVideoIds(userId);
    }
}
