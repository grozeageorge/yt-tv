package com.example.yt_tv;

import com.example.yt_tv.dtos.DtoMapper;
import com.example.yt_tv.dtos.PlaylistCreateDto;
import com.example.yt_tv.dtos.PlaylistDto;
import com.example.yt_tv.dtos.PlaylistUpdateDto;
import com.example.yt_tv.entities.Channel;
import com.example.yt_tv.entities.Playlist;
import com.example.yt_tv.entities.PlaylistChannel;
import com.example.yt_tv.entities.User;
import com.example.yt_tv.repositories.PlaylistRepository;
import com.example.yt_tv.services.PlaylistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PlaylistServiceTest {
    @Mock
    private PlaylistRepository playlistRepository;

    @Spy
    private DtoMapper dtoMapper;

    @InjectMocks
    private PlaylistService playlistService;

    private User user;
    private Channel channel1;
    private Channel channel2;
    private PlaylistChannel playlistChannel1;
    private PlaylistChannel playlistChannel2;
    private Playlist playlist;
    private Playlist saved;
    private Playlist playlist1;
    private Playlist playlist2;
    private Playlist updated;
    private PlaylistCreateDto playlistCreateDto;
    private PlaylistUpdateDto playlistUpdateDto;

    @BeforeEach
    void setUp() {
        user = TestDataFactory.user(1L, "John", "john@example.com", "johnpass");
        playlist = TestDataFactory.playlist(1L, "Music", user, new ArrayList<>());
        playlistCreateDto = TestDataFactory.playlistCreateDto("Music");
        playlistUpdateDto = TestDataFactory.playlistUpdateDto("Science");
        channel1 = TestDataFactory.channel(1L, "John's Channel", "UC-1234-abcd-5678-efgh", "https://example.com/thumbnail1.jpg", null, new ArrayList<>());
        channel2 = TestDataFactory.channel(2L, "Jane's Channel", "UC-5678-efgh-abcd-1234", "https://example.com/thumbnail2.jpg", null, new ArrayList<>());
        playlistChannel1 = TestDataFactory.playlistChannel(1L, playlist, channel1);
        playlistChannel2 = TestDataFactory.playlistChannel(2L, playlist, channel2);
        saved = playlist;
        playlist1 = playlist;
        playlist2 = TestDataFactory.playlist(2L, "Science", user, new ArrayList<>());
        updated = playlist2;
    }

    @Test
    void whenCreatePlaylist_thenPlaylistShouldBeSaved() {

        playlist.addPlaylistChannel(playlistChannel1);
        playlist.addPlaylistChannel(playlistChannel2);
        when(dtoMapper.toPlaylistEntity(user, playlistCreateDto)).thenReturn(playlist);

        saved.addPlaylistChannel(playlistChannel1);
        saved.addPlaylistChannel(playlistChannel2);
        when(playlistRepository.save(playlist)).thenReturn(saved);

        PlaylistDto result = playlistService.createPlaylist(user, playlistCreateDto);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Music");
        verify(dtoMapper).toPlaylistEntity(user, playlistCreateDto);
        verify(playlistRepository).save(playlist);
        verify(dtoMapper).toPlaylistDto(saved);
    }

    @Test
    void whenGetPlaylist_andPlaylistExists_thenReturnPlaylistDto() {
        when(playlistRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(playlist));

        PlaylistDto result = playlistService.getPlaylist(1L, user.getId());

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Music");
        verify(playlistRepository).findByIdAndUserId(1L, user.getId());
    }

    @Test
    void whenGetPlaylist_andPlaylistDoesNotExist_thenThrowException() {
        when(playlistRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playlistService.getPlaylist(1L, user.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Playlist not found");
    }

    @Test
    void whenGetPlaylists_thenReturnAllPlaylists() {
        when(playlistRepository.findAll()).thenReturn(List.of(playlist1, playlist2));

        playlist1.setPlaylistChannels(List.of(playlistChannel1));

        PlaylistDto playlistDto1 = new PlaylistDto();
        playlistDto1.setName("Music");
        playlistDto1.setChannels(new ArrayList<>());

        PlaylistDto playlistDto2 = new PlaylistDto();
        playlistDto2.setName("Science");
        playlistDto2.setChannels(new ArrayList<>());

        doReturn(playlistDto1).when(dtoMapper).toPlaylistDto(playlist1);
        doReturn(playlistDto2).when(dtoMapper).toPlaylistDto(playlist2);

        //ACT
        List<PlaylistDto> result = playlistService.getPlaylists();

        //ASSERT
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Music");
        assertThat(result.get(1).getName()).isEqualTo("Science");

        verify(playlistRepository).findAll();
        verify(dtoMapper, times(2)).toPlaylistDto(any(Playlist.class));
    }

    @Test
    void whenUpdatePlaylist_andPlaylistExists_thenModifyAndReturnDto() {
        when(playlistRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(playlist));

        when(playlistRepository.save(playlist)).thenReturn(updated);

        PlaylistDto result = playlistService.updatePlaylist(1L, user.getId(), playlistUpdateDto);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Science");
        verify(playlistRepository).findByIdAndUserId(1L, user.getId());
        verify(dtoMapper).updatePlaylistEntity(playlist, playlistUpdateDto);
        verify(playlistRepository).save(playlist);
        verify(dtoMapper).toPlaylistDto(updated);
    }

    @Test
    void whenUpdatePlaylist_andPlaylistDoesNotExist_thenThrowException() {
        when(playlistRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playlistService.updatePlaylist(1L, user.getId(), playlistUpdateDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Playlist not found");
    }

    @Test
    void whenDeletePlaylist_thenDeleteById() {
        when(playlistRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(playlist));
        doNothing().when(playlistRepository).deleteById(1L);

        playlistService.deletePlaylist(1L, user.getId());

        verify(playlistRepository).findByIdAndUserId(1L, user.getId());
        verify(playlistRepository).deleteById(1L);
    }

    @Test
    void whenAddPlaylistChannel_andPlaylistExists_thenAddsAndSaves() {


        Channel channel = new Channel();
        channel.setId(2L);

        PlaylistChannel playlistChannel = new PlaylistChannel();
        playlistChannel.setChannel(channel);

        when(playlistRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(playlist));
        Playlist saved = new Playlist();
        saved.setId(1L);
        saved.setName("Music");
        when(playlistRepository.save(playlist)).thenReturn(saved);

        playlistService.addPlaylistChannel(1L, playlistChannel, user.getId());

        verify(playlistRepository).findByIdAndUserId(1L, user.getId());
        verify(playlistRepository).save(playlist);
        assertThat(playlist.getPlaylistChannels()).contains(playlistChannel);
        assertThat(playlistChannel.getPlaylist()).isEqualTo(playlist);
    }

    @Test
    void whenAddPlaylistChannel_andChannelAlreadyPresent_thenThrowException() {
        playlistChannel1.setChannel(channel1);

        playlist.addPlaylistChannel(playlistChannel1);

        PlaylistChannel duplicate = new PlaylistChannel();
        Channel channelCopy = channel1;
        duplicate.setChannel(channelCopy);

        when(playlistRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(playlist));

        assertThatThrownBy(() -> playlistService.addPlaylistChannel(1L, duplicate, user.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Channel already present");

        verify(playlistRepository).findByIdAndUserId(1L, user.getId());
        verify(playlistRepository, never()).save(any());
    }

    @Test
    void whenRemoveChannelById_andChannelExists_thenRemoveAndSave() {
        playlistChannel1.setChannel(channel1);
        playlist.addPlaylistChannel(playlistChannel1);

        when(playlistRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(playlist));

        when(playlistRepository.save(playlist)).thenReturn(saved);

        PlaylistDto result = playlistService.removeChannelById(1L, user.getId(), 1L);

        verify(playlistRepository).findByIdAndUserId(1L, user.getId());
        verify(playlistRepository).save(playlist);
        assertThat(playlist.getPlaylistChannels()).isEmpty();
        assertThat(playlistChannel1.getPlaylist()).isNull();
    }

    @Test
    void whenRemoveChannelById_andChannelNotInPlaylist_thenThrowException() {
        when(playlistRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(playlist));

        assertThatThrownBy(() -> playlistService.removeChannelById(1L, 2L, user.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Channel not found in playlist");

        verify(playlistRepository).findByIdAndUserId(1L, user.getId());
        verify(playlistRepository, never()).save(any());
    }
}
