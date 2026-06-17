package com.example.yt_tv.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class PlaylistCreateDto {
    @NotBlank(message = "Playlist name cannot be empty")
    @Size(max = 50, message = "Playlist name too long")
    private String name;
}
