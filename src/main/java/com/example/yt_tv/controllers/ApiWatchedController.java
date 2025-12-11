package com.example.yt_tv.controllers;

import com.example.yt_tv.entities.User;
import com.example.yt_tv.repositories.UserRepository;
import com.example.yt_tv.services.WatchedVideoService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/watched")
@RequiredArgsConstructor
public class ApiWatchedController {
    private final WatchedVideoService watchedVideoService;
    private final UserRepository userRepository;

    @PostMapping("/{ytVideoId}")
    public ResponseEntity<Void> markAsWatched(@PathVariable String ytVideoId, Principal principal) {
        if (principal == null)
            return ResponseEntity.status(401).build();

        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found!"));

        watchedVideoService.markWatchedByYtId(user.getId(), ytVideoId);

        return ResponseEntity.ok().build();
    }
}
