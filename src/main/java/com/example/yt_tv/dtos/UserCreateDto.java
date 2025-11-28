package com.example.yt_tv.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserCreateDto {
    private String username;
    private String email;
    private String password;
}