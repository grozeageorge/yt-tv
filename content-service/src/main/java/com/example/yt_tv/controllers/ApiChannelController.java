package com.example.yt_tv.controllers;

import com.example.yt_tv.dtos.ChannelDto;
import com.example.yt_tv.services.ChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
public class ApiChannelController {
    private final ChannelService channelService;

    @GetMapping
    public List<ChannelDto> list() {
        return channelService.list();
    }

    @GetMapping("/{id}")
    public ChannelDto get(@PathVariable Long id) {
        return channelService.get(id);
    }

    @PostMapping("/sync/{id}")
    public ResponseEntity<Void> sync(@PathVariable Long id) {
        channelService.syncChannelVideos(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/create-from-query")
    public ChannelDto createFromQuery(@RequestParam String query) {
        return channelService.createAndSyncFromQuery(query);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        channelService.delete(id);
        return ResponseEntity.ok().build();
    }
}
