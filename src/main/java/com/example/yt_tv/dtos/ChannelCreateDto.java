package com.example.yt_tv.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ChannelCreateDto {
    @NotBlank(message = "ChannelID is required")
    private String ytChannelId;

    @NotBlank(message = "Channel name is required")
    private String name;

    private String thumbnailUrl;
}
