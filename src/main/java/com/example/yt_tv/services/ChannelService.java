package com.example.yt_tv.services;

import com.example.yt_tv.dtos.ChannelCreateDto;
import com.example.yt_tv.dtos.ChannelDto;
import com.example.yt_tv.dtos.DtoMapper;
import com.example.yt_tv.entities.Channel;
import com.example.yt_tv.repositories.ChannelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChannelService {
    private final ChannelRepository channelRepository;
    private final DtoMapper dtoMapper;

    @Transactional
    public ChannelDto create(ChannelCreateDto channelCreateDto) {
        Channel saved = channelRepository.save(dtoMapper.toChannelEntity(channelCreateDto));
        return dtoMapper.toChannelDto(saved);
    }

    @Transactional(readOnly = true)
    public ChannelDto get(Long id) {
        return channelRepository.findById(id)
                .map(dtoMapper::toChannelDto)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found"));
    }

    @Transactional(readOnly = true)
    public List<ChannelDto> list() {
        return channelRepository.findAll().stream()
                .map(dtoMapper::toChannelDto)
                .toList();
    }

    @Transactional
    public ChannelDto update(Long id, ChannelCreateDto channelCreateDto) {
        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found"));
        channel.setYtChannelId(channelCreateDto.getYtChannelId());
        channel.setName(channelCreateDto.getName());
        channel.setThumbnailUrl(channelCreateDto.getThumbnailUrl());
        Channel updated = channelRepository.save(channel);
        return dtoMapper.toChannelDto(updated);
    }

    @Transactional
    public void delete(Long id) {
        channelRepository.deleteById(id);
    }

    @Transactional
    public ChannelDto updateLastSync(Long id, Instant newSync) {
        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found"));
        channel.setLastSync(newSync);
        Channel updated = channelRepository.save(channel);
        return dtoMapper.toChannelDto(updated);
    }

}
