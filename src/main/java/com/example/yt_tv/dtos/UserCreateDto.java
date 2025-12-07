package com.example.yt_tv.dtos;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class UserCreateDto {
    private String username;
    private String email;
    private String password;
}