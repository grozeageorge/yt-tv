package com.example.yt_tv.controllers;

import com.example.yt_tv.dtos.*;
import com.example.yt_tv.entities.User;
import com.example.yt_tv.repositories.UserRepository;
import com.example.yt_tv.services.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PlaylistService playlistService;
    private final ChannelService channelService;
    private final PlaylistChannelService playlistChannelService;

    // --- LOGIN & AUTH ---

    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("loginRequest", new UserCreateDto());
        return "login";
    }

    @PostMapping("/login")
    public String handleLogin(@ModelAttribute("loginRequest") UserCreateDto loginRequest, HttpSession session, Model model) {
        Optional<User> userOpt = userRepository.findByEmail(loginRequest.getEmail());
        if (userOpt.isPresent() && userOpt.get().getPassword().equals(loginRequest.getPassword())) {
            session.setAttribute("user", userOpt.get());
            return "redirect:/dashboard";
        }
        model.addAttribute("error", "Invalid email or password");
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("userDto", new UserCreateDto());
        return "register";
    }

    @PostMapping("/register")
    public String handleRegister(@ModelAttribute UserCreateDto userDto) {
        userService.create(userDto);
        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // --- DASHBOARD ---

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        model.addAttribute("playlists", playlistService.getPlaylistsByUserId(user.getId()));
        model.addAttribute("newPlaylist", new PlaylistCreateDto());
        model.addAttribute("username", user.getUsername());
        return "dashboard";
    }

    @PostMapping("/playlists/create")
    public String createPlaylist(@ModelAttribute PlaylistCreateDto createDto, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        playlistService.createPlaylist(user, createDto);
        return "redirect:/dashboard";
    }

    // --- PLAYLIST VIEW (The Missing Piece?) ---

    @GetMapping("/playlist/{id}")
    public String viewPlaylist(@PathVariable Long id, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        // Get the playlist details
        PlaylistDto playlist = playlistService.getPlaylist(id);
        model.addAttribute("playlist", playlist);

        return "playlist_view";
    }

    // --- ADD CHANNEL (SEARCH) ---

    @PostMapping("/playlist/{playlistId}/add-channel-query")
    public String addChannelToPlaylistByQuery(
            @PathVariable Long playlistId,
            @RequestParam String query,
            RedirectAttributes redirectAttributes) {

        try {
            // 1. Search, Create & Sync
            ChannelDto channel = channelService.createAndSyncFromQuery(query);

            // 2. Add to Playlist
            AddChannelToPlaylistDto addDto = new AddChannelToPlaylistDto();
            addDto.setPlaylistId(playlistId);
            addDto.setChannelId(channel.getId());
            playlistChannelService.addChannelToPlaylist(addDto);

            redirectAttributes.addFlashAttribute("success", "Added " + channel.getName() + "!");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error adding channel.");
        }

        return "redirect:/playlist/" + playlistId;
    }

    // --- SYNC, REMOVE & DELETE ---

    @PostMapping("/channels/sync")
    public String syncChannel(@RequestParam Long channelId,
                              @RequestParam(required = false) Long playlistId) {
        channelService.syncChannelVideos(channelId);

        if (playlistId != null) {
            return "redirect:/playlist/" + playlistId;
        }
        return "redirect:/channels";
    }

    @GetMapping("/playlist/remove-channel")
    public String removeChannel(@RequestParam Long playlistId, @RequestParam Long channelId) {
        playlistService.removeChannelById(playlistId, channelId);
        return "redirect:/playlist/" + playlistId;
    }

    @GetMapping("/channels/delete-global")
    public String deleteChannelGlobal(@RequestParam Long channelId, @RequestParam Long returnPlaylistId) {
        channelService.delete(channelId);
        return "redirect:/playlist/" + returnPlaylistId;
    }

    // --- MANAGE CHANNELS (Legacy Page) ---

    @GetMapping("/channels")
    public String listChannels(Model model) {
        model.addAttribute("channels", channelService.list());
        model.addAttribute("newChannel", new ChannelCreateDto());
        return "channels";
    }

    @PostMapping("/channels/create")
    public String createChannel(@RequestParam String query, RedirectAttributes redirectAttributes) {
        try {
            channelService.createAndSyncFromQuery(query);
            redirectAttributes.addFlashAttribute("success", "Channel added and synced!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/channels";
    }

    @GetMapping("/channels/delete/{id}")
    public String deleteChannel(@PathVariable Long id) {
        channelService.delete(id);
        return "redirect:/channels";
    }
}