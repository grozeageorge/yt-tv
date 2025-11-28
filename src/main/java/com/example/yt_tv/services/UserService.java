package com.example.yt_tv.services;

import com.example.yt_tv.dtos.DtoMapper;
import com.example.yt_tv.dtos.UserDto;
import com.example.yt_tv.dtos.UserCreateDto;
import com.example.yt_tv.entities.User;
import com.example.yt_tv.repositories.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final DtoMapper dtoMapper;

    @Transactional
    public UserDto create(UserCreateDto userCreateDto) {
        User saved = userRepository.save(dtoMapper.toUserEntity(userCreateDto));
        return dtoMapper.toUserDto(saved);
    }

    @Transactional(readOnly = true)
    public UserDto get (Long id) {
        return userRepository.findById(id)
                .map(dtoMapper::toUserDto)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Transactional(readOnly = true)
    public List<UserDto> list() {
        return userRepository.findAll().stream()
                .map(dtoMapper::toUserDto)
                .toList();
    }

    @Transactional
    public UserDto update(Long id, UserCreateDto userCreateDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setUsername(userCreateDto.getUsername());
        user.setEmail(userCreateDto.getEmail());
        user.setPassword(userCreateDto.getPassword());

        User updated = userRepository.save(user);
        return dtoMapper.toUserDto(updated);
    }

    @Transactional
    public void delete(Long id) {
        userRepository.deleteById(id);
    }
}
