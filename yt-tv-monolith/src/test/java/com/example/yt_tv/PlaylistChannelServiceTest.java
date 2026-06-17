package com.example.yt_tv;

import com.example.yt_tv.dtos.AddChannelToPlaylistDto;
import com.example.yt_tv.dtos.DtoMapper;
import com.example.yt_tv.dtos.PlaylistChannelDto;
import com.example.yt_tv.entities.Channel;
import com.example.yt_tv.entities.Playlist;
import com.example.yt_tv.entities.PlaylistChannel;
import com.example.yt_tv.entities.User;
import com.example.yt_tv.repositories.ChannelRepository;
import com.example.yt_tv.repositories.PlaylistChannelRepository;
import com.example.yt_tv.repositories.PlaylistRepository;
import com.example.yt_tv.services.PlaylistChannelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PlaylistChannelServiceTest {
    @Mock
    private PlaylistChannelRepository playlistChannelRepository;

    @Mock
    private PlaylistRepository playlistRepository;

    @Mock
    private ChannelRepository channelRepository;

    @Spy
    private DtoMapper dtoMapper;

    @InjectMocks
    private PlaylistChannelService playlistChannelService;

    private PlaylistChannel playlistChannel;
    private Playlist playlist;
    private Channel channel;
    private User user;
    private AddChannelToPlaylistDto addDto;
    @BeforeEach
    void setUp() {
        user = TestDataFactory.user(1L, "John", "john@example.com", "johnpass");
        channel = TestDataFactory.channel(1L, "John's channel", "UC-1234-abcd-5678-efgh", "https://example.com/thumbnail.jpg", null, null);
        playlist = TestDataFactory.playlist(1L, "My Playlist", user, null);
        playlistChannel = TestDataFactory.playlistChannel(1L, playlist, channel);
        addDto = TestDataFactory.addChannelToPlaylistDto(1L, 1L);
    }

    @Test
    void whenAddChanneltoPlaylist_andPlaylistAndChannelExist_thenChannelShouldBeAdded() {
        when(playlistRepository.findByIdAndUserId(1L, user.getId())).thenReturn(java.util.Optional.of(playlist));
        when(channelRepository.findById(1L)).thenReturn(java.util.Optional.of(channel));

        doReturn(playlistChannel).when(dtoMapper).toPlaylistChannelEntity(playlist, channel);
        when(playlistChannelRepository.save(playlistChannel)).thenReturn(playlistChannel);

        PlaylistChannelDto expectedDto = new PlaylistChannelDto();
        doReturn(expectedDto).when(dtoMapper).toPlaylistChannelDto(playlistChannel);

        PlaylistChannelDto result = playlistChannelService.addChannelToPlaylist(addDto, user.getId());

        assertThat(result).isNotNull();
        assertThat(result).usingRecursiveComparison().isEqualTo(expectedDto);
        verify(playlistRepository).findByIdAndUserId(1L, user.getId());
        verify(channelRepository).findById(1L);
        verify(dtoMapper).toPlaylistChannelEntity(playlist, channel);
        verify(playlistChannelRepository).save(playlistChannel);
        verify(dtoMapper).toPlaylistChannelDto(playlistChannel);
    }

    @Test
    void whenAddChanneltoPlaylist_andPlaylistDoesNotExist_thenThrowException() {
        when(playlistChannelRepository.existsByPlaylistIdAndChannelId(1L, 1L)).thenReturn(false);
        when(playlistRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playlistChannelService.addChannelToPlaylist(addDto, user.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Playlist not found");

        verify(playlistChannelRepository).existsByPlaylistIdAndChannelId(1L, 1L);
        verify(playlistRepository).findByIdAndUserId(1L, user.getId());
        verifyNoInteractions(channelRepository, dtoMapper);
        verify(playlistChannelRepository, never()).save(any());
    }

    @Test
    void whenAddChanneltoPlaylist_andChannelDoesNotExist_thenThrowException() {
        when(playlistChannelRepository.existsByPlaylistIdAndChannelId(1L, 1L)).thenReturn(false);
        when(playlistRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(playlist));
        when(channelRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playlistChannelService.addChannelToPlaylist(addDto, user.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Channel not found");

        verify(playlistChannelRepository).existsByPlaylistIdAndChannelId(1L, 1L);
        verify(playlistRepository).findByIdAndUserId(1L, user.getId());
        verify(channelRepository).findById(1L);
        verifyNoInteractions(dtoMapper);
        verify(playlistChannelRepository, never()).save(any());
    }
}
