package com.example.yt_tv;

import com.example.yt_tv.dtos.DtoMapper;
import com.example.yt_tv.dtos.VideoCreateDto;
import com.example.yt_tv.dtos.VideoDto;
import com.example.yt_tv.entities.Channel;
import com.example.yt_tv.entities.Video;
import com.example.yt_tv.repositories.ChannelRepository;
import com.example.yt_tv.repositories.VideoRepository;
import com.example.yt_tv.services.VideoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VideoServiceTest {

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private ChannelRepository channelRepository;

    @Spy
    private DtoMapper dtoMapper;

    @InjectMocks
    private VideoService videoService;

    private Channel channel;
    private Video existingVideo;
    private Video savedVideo;

    @BeforeEach
    void setUp() {
        channel = TestDataFactory.channel(
                1L,
                "John's Channel",
                "UC-1234-abcd-5678-efgh",
                "https://example.com/thumbnail.jpg",
                null,
                null
        );
        existingVideo = TestDataFactory.video(
                1L,
                "John's first video",
                "basic-thumbnail",
                "yt-1",
                channel
        );
        savedVideo = TestDataFactory.video(
                2L,
                "John's updated video",
                "new-thumbnail",
                "yt-2",
                channel
        );
    }

    @Test
    void whenCreate_andChannelExists_thenVideoIsCreatedAndDtoReturned() {
        VideoCreateDto createDto = VideoCreateDto.builder()
                .ytVideoId("yt-2")
                .title("John's updated video")
                .thumbnailUrl("new-thumbnail")
                .build();

        when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));
        when(videoRepository.save(any(Video.class))).thenReturn(savedVideo);

        VideoDto result = videoService.createVideo(createDto, 1L);

        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getYtVideoId()).isEqualTo("yt-2");
        assertThat(result.getTitle()).isEqualTo("John's updated video");
        assertThat(result.getThumbnailUrl()).isEqualTo("new-thumbnail");
        assertThat(result.getChannelId()).isEqualTo(1L);

        verify(channelRepository).findById(1L);
        verify(videoRepository).save(any(Video.class));
    }

    @Test
    void whenCreate_andChannelMissing_thenExceptionThrown() {
        VideoCreateDto createDto = VideoCreateDto.builder()
                .ytVideoId("yt-2")
                .title("John's updated video")
                .thumbnailUrl("new-thumbnail")
                .build();

        when(channelRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> videoService.createVideo(createDto, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Channel not found");

        verify(videoRepository, never()).save(any());
    }

    @Test
    void whenGet_andVideoExists_thenDtoReturned() {
        when(videoRepository.findById(1L)).thenReturn(Optional.of(existingVideo));

        VideoDto result = videoService.get(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getYtVideoId()).isEqualTo("yt-1");
        assertThat(result.getTitle()).isEqualTo("John's first video");
        assertThat(result.getChannelId()).isEqualTo(1L);
    }

    @Test
    void whenGet_andVideoMissing_thenExceptionThrown() {
        when(videoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> videoService.get(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Video not found");
    }

    @Test
    void whenList_thenAllVideosMappedToDtos() {
        Video otherVideo = TestDataFactory.video(
                2L,
                "Another video",
                "thumb-2",
                "yt-2",
                channel
        );

        when(videoRepository.findAll()).thenReturn(Arrays.asList(existingVideo, otherVideo));

        List<VideoDto> result = videoService.list();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(VideoDto::getId).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void whenUpdate_andVideoExists_thenFieldsUpdatedAndDtoReturned() {
        VideoCreateDto updateDto = VideoCreateDto.builder()
                .ytVideoId("yt-2")
                .title("Updated title")
                .thumbnailUrl("updated-thumbnail")
                .build();

        when(videoRepository.findById(1L)).thenReturn(Optional.of(existingVideo));
        when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));
        when(videoRepository.save(any(Video.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VideoDto result = videoService.update(1L, updateDto, 1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getYtVideoId()).isEqualTo("yt-2");
        assertThat(result.getTitle()).isEqualTo("Updated title");
        assertThat(result.getThumbnailUrl()).isEqualTo("updated-thumbnail");
        assertThat(result.getChannelId()).isEqualTo(1L);
    }

    @Test
    void whenUpdate_andVideoMissing_thenExceptionThrown() {
        VideoCreateDto updateDto = VideoCreateDto.builder()
                .ytVideoId("yt-2")
                .title("Updated title")
                .thumbnailUrl("updated-thumbnail")
                .build();

        when(videoRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> videoService.update(1L, updateDto, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Video not found");
    }

    @Test
    void whenDelete_thenRepositoryDeleteByIdCalled() {
        videoService.delete(1L);
        verify(videoRepository).deleteById(1L);
    }

    @Test
    void whenListByChannel_thenOnlyChannelVideosReturned() {
        when(videoRepository.findByChannelId(1L)).thenReturn(List.of(existingVideo));

        List<VideoDto> result = videoService.listByChannel(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChannelId()).isEqualTo(1L);
    }
}
