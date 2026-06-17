package com.example.yt_tv;

import com.example.yt_tv.dtos.ChannelCreateDto;
import com.example.yt_tv.dtos.ChannelDto;
import com.example.yt_tv.dtos.DtoMapper;
import com.example.yt_tv.entities.Channel;
import com.example.yt_tv.entities.PlaylistChannel;
import com.example.yt_tv.entities.Video;
import com.example.yt_tv.repositories.ChannelRepository;
import com.example.yt_tv.repositories.PlaylistChannelRepository;
import com.example.yt_tv.repositories.VideoRepository;
import com.example.yt_tv.repositories.WatchedVideoRepository;
import com.example.yt_tv.services.AiCategorizerService;
import com.example.yt_tv.services.AiSyncService;
import com.example.yt_tv.services.ChannelService;
import com.example.yt_tv.services.YoutubeApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChannelServiceTest {
    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private PlaylistChannelRepository playlistChannelRepository;

    @Mock
    private WatchedVideoRepository watchedVideoRepository;

    @Mock
    private YoutubeApiClient youtubeApiClient;

    @Mock
    private AiCategorizerService aiCategorizerService;

    @Mock
    private AiSyncService aiSyncService;

    @Spy
    private DtoMapper dtoMapper;

    @InjectMocks
    private ChannelService channelService;

    private Channel channel;
    private ChannelCreateDto channelCreateDto;
    private Channel saved;
    private Channel channel1;
    private Channel channel2;
    private ChannelCreateDto channelUpdateDto;
    private Channel updated;
    @BeforeEach
    void setUp() {
        channel = TestDataFactory.channel(1L, "John's Channel", "UC-1234-abcd-5678-efgh",  "https://example.com/thumbnail.jpg", Instant.now(), new ArrayList<PlaylistChannel>());
        saved = channel;
        channel1 = channel;
        channel2 = TestDataFactory.channel(2L, "Jane's Channel", "UC-5678-efgh-abcd-1234", "https://example.com/thumbnail2.jpg", Instant.now(), new ArrayList<PlaylistChannel>());
        updated = channel2;
        channelCreateDto = TestDataFactory.channelCreateDto("John's Channel", "UC-1234-abcd-5678-efgh", "https://example.com/thumbnail.jpg");
        channelUpdateDto = TestDataFactory.channelCreateDto("Jane's Channel", "UC-5678-efgh-abcd-1234", "https://example.com/thumbnail2.jpg");
    }

    @Test
    void whenCreateChannel_thenChannelShouldBeSaved() {
        when(dtoMapper.toChannelEntity(channelCreateDto)).thenReturn(channel);

        when(channelRepository.save(channel)).thenReturn(saved);

        ChannelDto result = channelService.create(channelCreateDto);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("John's Channel");
        verify(dtoMapper).toChannelEntity(channelCreateDto);
        verify(channelRepository).save(channel);
        verify(dtoMapper).toChannelDto(saved);
    }

    @Test
    void whenGetChannel_andChannelExists_thenReturnChannelDto() {
        when(channelRepository.findById(1L)).thenReturn(java.util.Optional.of(channel));

        ChannelDto result = channelService.get(1L);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("John's Channel");

        verify(dtoMapper, times(1)).toChannelDto(any(Channel.class));
    }

    @Test
    void whenGetChannel_andChannelDoesNotExist_thenThrowException() {
        when(channelRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> channelService.get(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Channel not found with id: 1");
    }

    @Test
    void whenListChannels_thenReturnAllChannels() {
        when(channelRepository.findAll()).thenReturn(List.of(channel1, channel2));

        List<ChannelDto> result = channelService.list();

        assertThat(result).hasSize(2);
        verify(dtoMapper, times(2)).toChannelDto(any(Channel.class));
    }

    @Test
    void whenUpdate_andChannelExists_thenModifyAndReturnDto() {
        when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));

        when(channelRepository.save(channel)).thenReturn(updated);

        ChannelDto result = channelService.update(1L, channelUpdateDto);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Jane's Channel");
        verify(channelRepository).findById(1L);
        verify(channelRepository).save(channel);
        verify(dtoMapper).toChannelDto(updated);
    }

    @Test
    void whenUpdate_andChannelDoesNotExist_thenThrowException() {
        when(channelRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> channelService.update(1L, channelUpdateDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Channel not found with id: 1");
    }

    @Test
    void whenDelete_thenDeleteById() {
        List<Video> videos = List.of(new Video(), new Video());
        videos.get(0).setId(10L);
        videos.get(1).setId(11L);

        List<PlaylistChannel> playlistChannels = List.of(new PlaylistChannel(), new PlaylistChannel());

        when(videoRepository.findByChannelId(1L)).thenReturn(videos);
        when(playlistChannelRepository.findByChannelId(1L)).thenReturn(playlistChannels);

        channelService.delete(1L);

        verify(videoRepository).findByChannelId(1L);
        verify(watchedVideoRepository).deleteByVideoId(10L);
        verify(watchedVideoRepository).deleteByVideoId(11L);
        verify(videoRepository).deleteAll(videos);
        verify(playlistChannelRepository).findByChannelId(1L);
        verify(playlistChannelRepository).deleteAll(playlistChannels);
        verify(channelRepository).deleteById(1L);
    }

    @Test
    void whenUpdateLastSync_andChannelExists_thenModifyAndReturnDto() {
        Instant sync = Instant.ofEpochMilli(1625097600000L);
        channel.setLastSync(sync);
        when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));

        Instant newSync = sync.plusSeconds(3600);

        saved.setLastSync(newSync);
        when(channelRepository.save(channel)).thenReturn(saved);

        ChannelDto result = channelService.updateLastSync(1L, newSync);

        assertThat(result).isNotNull();
        assertThat(result.getLastSync()).isEqualTo(newSync);
        verify(channelRepository).findById(1L);
        verify(channelRepository).save(channel);
        verify(dtoMapper).toChannelDto(saved);
    }

    @Test
    void whenUpdateLastSync_andChannelDoesNotExist_thenThrowException() {
        Instant sync = Instant.now();
        when(channelRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> channelService.updateLastSync(1L, sync))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Channel not found with id: 1");
    }
}
