package com.example.yt_tv.dtos;

import com.example.yt_tv.entities.User;
import org.springframework.stereotype.Component;

@Component
public class DtoMapper {
    public UserDto toUserDto(User user) {
        if (user == null) return null;
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setUsername(user.getUsername());
        userDto.setEmail(user.getEmail());
        return userDto;
    }

    public User toUserEntity(UserCreateDto userCreateDto) {
        if (userCreateDto == null) return null;
        User user = new User();
        user.setUsername(userCreateDto.getUsername());
        user.setEmail(userCreateDto.getEmail());
        user.setPassword(userCreateDto.getPassword());
        return user;
    }
}
