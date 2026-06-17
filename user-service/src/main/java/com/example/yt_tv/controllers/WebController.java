package com.example.yt_tv.controllers;

import com.example.yt_tv.clients.*;
import com.example.yt_tv.dtos.*;
import com.example.yt_tv.entities.User;
import com.example.yt_tv.repositories.UserRepository;
import com.example.yt_tv.services.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.*;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final ContentClient contentClient;
    private final PlaylistClient playlistClient;
    private final AiClient aiClient;

    // --- HELPER ---

    private User getLoggedInUser(Principal principal) {
        if (principal == null)
            return null;

        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found!"));
    }

    // --- LOGIN & AUTH ---

    @GetMapping("/")
    public String index()
    {
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("userDto", new UserCreateDto());
        return "register";
    }

    @PostMapping("/register")
    public String handleRegister(@Valid @ModelAttribute UserCreateDto userDto, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "register";
        }

        try {
            userService.create(userDto);
        } catch (Exception e) {
            model.addAttribute("error", "Username or Email already exists.");
            return "register";
        }

        return "redirect:/login?registered";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // --- DASHBOARD ---

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        User user = getLoggedInUser(principal);
        model.addAttribute("username", user.getUsername());
        model.addAttribute("playlists", playlistClient.listPlaylists(user.getId()));
        model.addAttribute("newPlaylist", new PlaylistCreateDto());
        return "dashboard";
    }

    @GetMapping("/chat")
    public String chatPage(Model model, Principal principal) {
        User user = getLoggedInUser(principal);
        if (user != null) {
            model.addAttribute("currentUserId", user.getId());
        }
        return "chat";
    }

    // --- PLAYLISTS ---

    @GetMapping("/playlist/{id}")
    public String viewPlaylist(@PathVariable Long id, Model model, Principal principal) {
        User user = getLoggedInUser(principal);
        PlaylistDto playlist = playlistClient.getPlaylist(id, user.getId());
        model.addAttribute("playlist", playlist);
        model.addAttribute("currentUserId", user.getId());
        return "playlist_view";
    }

    @PostMapping("/playlists/create")
    public String createPlaylist(@ModelAttribute PlaylistCreateDto dto, Principal principal) {
        User user = getLoggedInUser(principal);
        playlistClient.createPlaylist(user.getId(), dto);
        return "redirect:/dashboard";
    }

    @GetMapping("/playlists/delete/{id}")
    public String deletePlaylist(@PathVariable Long id, Principal principal) {
        User user = getLoggedInUser(principal);
        playlistClient.deletePlaylist(id, user.getId());
        return "redirect:/dashboard";
    }

    @PostMapping("/playlist/{id}/add-channel-query")
    public String addChannelToPlaylist(@PathVariable Long id, @RequestParam String query, Principal principal, RedirectAttributes ra) {
        User user = getLoggedInUser(principal);
        try {
            ChannelDto channel = contentClient.createChannelFromQuery(query);
            playlistClient.addChannelToPlaylist(id, channel.getId(), channel.getName(), channel.getThumbnailUrl(), user.getId());
            ra.addFlashAttribute("success", "Added channel: " + channel.getName());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to add channel: " + e.getMessage());
        }
        return "redirect:/playlist/" + id;
    }

    @GetMapping("/playlist/remove-channel")
    public String removeChannelFromPlaylist(@RequestParam Long playlistId, @RequestParam Long channelId, Principal principal) {
        User user = getLoggedInUser(principal);
        playlistClient.removeChannelFromPlaylist(playlistId, channelId, user.getId());
        return "redirect:/playlist/" + playlistId;
    }

    // --- CHANNELS ---

    @PostMapping("/channels/sync")
    public String syncChannel(@RequestParam Long channelId, @RequestParam Long playlistId) {
        contentClient.syncChannel(channelId);
        return "redirect:/playlist/" + playlistId;
    }

    @GetMapping("/channels/delete-global")
    public String deleteChannelGlobal(@RequestParam Long channelId, @RequestParam Long returnPlaylistId) {
        try {
            playlistClient.deleteChannelGlobal(channelId);
        } catch (Exception e) {
            log.warn("Failed to delete channel from all playlists via playlist-service for channelId={}: {}", channelId, e.getMessage());
        }

        try {
            contentClient.deleteChannel(channelId);
        } catch (Exception e) {
            log.warn("Failed to delete channel entity via content-service for channelId={}: {}", channelId, e.getMessage());
        }

        return "redirect:/playlist/" + returnPlaylistId;
    }

    // --- TV PLAYER ---

    @GetMapping("/play/playlist/{id}")
    public String playPlaylist(@PathVariable Long id, Model model, Principal principal) {
        User user = getLoggedInUser(principal);
        PlaylistDto playlist = playlistClient.getPlaylist(id, user.getId());

        List<String> videoQueue = new ArrayList<>();
        List<String> watchedIds = playlistClient.getWatchedYtVideoIds(user.getId());
        int channelsCount = playlist.getChannels().size();
        int videosPerChannel = channelsCount > 0 ? (20 / channelsCount) + 1 : 0;

        // For each channel in playlist, get a batch of unwatched videos
        for (PlaylistChannelDto pc : playlist.getChannels()) {
            List<VideoDto> batch = contentClient.getRandomBatchFromChannel(pc.getChannelId(), videosPerChannel);
            for (VideoDto v : batch) {
                if (v != null && !watchedIds.contains(v.getYtVideoId()) && videoQueue.size() < 20) {
                    videoQueue.add(v.getYtVideoId());
                }
            }
        }

        if (videoQueue.size() < 20) {
            // Fill more if still not enough
            for (PlaylistChannelDto pc : playlist.getChannels()) {
                if (videoQueue.size() >= 20) break;
                List<VideoDto> batch = contentClient.getRandomBatchFromChannel(pc.getChannelId(), 20);
                for (VideoDto v : batch) {
                    if (v != null && !watchedIds.contains(v.getYtVideoId()) && !videoQueue.contains(v.getYtVideoId())) {
                        videoQueue.add(v.getYtVideoId());
                        if (videoQueue.size() >= 20) break;
                    }
                }
            }
        }

        Collections.shuffle(videoQueue);
        model.addAttribute("videoQueue", videoQueue);
        model.addAttribute("playlistId", id);
        return "tv_player";
    }

    @PostMapping("/play/custom")
    public String playCustom(@RequestParam String videoIds, @RequestParam(required = false) String originalQuery, Model model) {
        List<String> queue = Arrays.asList(videoIds.split(","));
        model.addAttribute("videoQueue", queue);
        model.addAttribute("originalQuery", originalQuery);
        return "tv_player";
    }

    // --- AJAX ENDPOINTS ---

    @PostMapping("/api/watched/{ytVideoId}")
    @ResponseBody
    public ResponseEntity<Void> markWatched(@PathVariable String ytVideoId, Principal principal) {
        User user = getLoggedInUser(principal);
        playlistClient.markAsWatched(ytVideoId, user.getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/ai/suggest-channels")
    @ResponseBody
    public Map<String, List<String>> suggestChannels(@RequestParam Long playlistId, Principal principal) {
        User user = getLoggedInUser(principal);
        return aiClient.suggestChannels(playlistId, user.getId());
    }

    @PostMapping("/api/ai/chat-full")
    @ResponseBody
    public ChatResponseDto chatFull(@RequestParam String message, Principal principal) {
        User user = getLoggedInUser(principal);
        return aiClient.chatFull(message, user.getId());
    }
}