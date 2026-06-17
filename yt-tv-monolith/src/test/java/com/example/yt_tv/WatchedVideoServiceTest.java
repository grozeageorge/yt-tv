package com.example.yt_tv;

import com.example.yt_tv.dtos.DtoMapper;
import com.example.yt_tv.dtos.WatchedVideoDto;
import com.example.yt_tv.entities.User;
import com.example.yt_tv.entities.Video;
import com.example.yt_tv.entities.WatchedVideo;
import com.example.yt_tv.repositories.UserRepository;
import com.example.yt_tv.repositories.VideoRepository;
import com.example.yt_tv.repositories.WatchedVideoRepository;
import com.example.yt_tv.services.WatchedVideoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WatchedVideoServiceTest {

    @Mock
    private WatchedVideoRepository watchedVideoRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VideoRepository videoRepository;

    @Spy
    private DtoMapper dtoMapper;

    @InjectMocks
    private WatchedVideoService watchedVideoService;

    private User user;
    private Video video;
    private WatchedVideo existingWatched;

    @BeforeEach
    void setUp() {
        user = TestDataFactory.user(
                1L,
                "john",
                "john@example.com",
                "secret"
        );

        video = TestDataFactory.video(
                1L,
                "Some video",
                "thumb-url",
                "yt-1",
                TestDataFactory.channel(
                        1L,
                        "John's Channel",
                        "UC-1234",
                        "https://example.com/thumb.jpg",
                        null,
                        null
                )
        );

        existingWatched = TestDataFactory.watchedVideo(
                1L,
                user,
                video
        );
    }

    @Test
    void whenMarkWatched_andUserAndVideoExist_andNoExistingRecord_thenCreateAndReturnDto() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video));
        when(watchedVideoRepository.findByUserIdAndVideoId(1L, 1L))
                .thenReturn(Optional.empty());
        when(watchedVideoRepository.save(any(WatchedVideo.class)))
                .thenAnswer(invocation -> {
                    WatchedVideo wv = invocation.getArgument(0);
                    wv.setId(1L);
                    return wv;
                });

        WatchedVideoDto result = watchedVideoService.markWatched(1L, 1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getVideoId()).isEqualTo(1L);
        assertThat(result.isWatched()).isTrue();

        verify(watchedVideoRepository).save(any(WatchedVideo.class));
    }

    @Test
    void whenMarkWatched_andExistingRecord_thenUpdateToWatchedAndReturnDto() {
        existingWatched.setWatched(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video));
        when(watchedVideoRepository.findByUserIdAndVideoId(1L, 1L))
                .thenReturn(Optional.of(existingWatched));
        when(watchedVideoRepository.save(existingWatched)).thenReturn(existingWatched);

        WatchedVideoDto result = watchedVideoService.markWatched(1L, 1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.isWatched()).isTrue();
    }

    @Test
    void whenMarkWatched_andUserMissing_thenExceptionThrown() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> watchedVideoService.markWatched(1L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");

        verify(watchedVideoRepository, never()).save(any());
    }

    @Test
    void whenMarkWatched_andVideoMissing_thenExceptionThrown() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(videoRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> watchedVideoService.markWatched(1L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Video not found");

        verify(watchedVideoRepository, never()).save(any());
    }

    @Test
    void whenMarkSkipped_andNoExistingRecord_thenCreateAndReturnDtoWithWatchedFalse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video));
        when(watchedVideoRepository.findByUserIdAndVideoId(1L, 1L))
                .thenReturn(Optional.empty());
        when(watchedVideoRepository.save(any(WatchedVideo.class)))
                .thenAnswer(invocation -> {
                    WatchedVideo wv = invocation.getArgument(0);
                    wv.setId(2L);
                    return wv;
                });

        WatchedVideoDto result = watchedVideoService.markSkipped(1L, 1L);

        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.isWatched()).isFalse();
    }

    @Test
    void whenMarkSkipped_andExistingRecord_thenSetWatchedFalseAndReturnDto() {
        existingWatched.setWatched(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video));
        when(watchedVideoRepository.findByUserIdAndVideoId(1L, 1L))
                .thenReturn(Optional.of(existingWatched));
        when(watchedVideoRepository.save(existingWatched)).thenReturn(existingWatched);

        WatchedVideoDto result = watchedVideoService.markSkipped(1L, 1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.isWatched()).isFalse();
    }

    @Test
    void whenIsPlayable_andRecordExistsAndWatchedTrue_thenNotPlayable() {
        existingWatched.setWatched(true);
        when(watchedVideoRepository.findByUserIdAndVideoId(1L, 1L))
                .thenReturn(Optional.of(existingWatched));

        boolean playable = watchedVideoService.isPlayable(1L, 1L);

        assertThat(playable).isFalse();
    }

    @Test
    void whenIsPlayable_andRecordExistsAndWatchedFalse_thenPlayable() {
        existingWatched.setWatched(false);
        when(watchedVideoRepository.findByUserIdAndVideoId(1L, 1L))
                .thenReturn(Optional.of(existingWatched));

        boolean playable = watchedVideoService.isPlayable(1L, 1L);

        assertThat(playable).isTrue();
    }

    @Test
    void whenIsPlayable_andNoRecord_thenPlayable() {
        when(watchedVideoRepository.findByUserIdAndVideoId(1L, 1L))
                .thenReturn(Optional.empty());

        boolean playable = watchedVideoService.isPlayable(1L, 1L);

        assertThat(playable).isTrue();
    }

    @Test
    void whenGetByUserId_thenDtosReturned() {
        when(watchedVideoRepository.findByUserId(1L))
                .thenReturn(List.of(existingWatched));

        List<WatchedVideoDto> result = watchedVideoService.getByUserId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(1L);
        assertThat(result.get(0).getVideoId()).isEqualTo(1L);
    }

    @Test
    void whenDeleteWatched_thenRepositoryDeleteCalled() {
        watchedVideoService.deleteWatched(1L, 1L);
        verify(watchedVideoRepository).deleteByUserIdAndVideoId(1L, 1L);
    }

    @Test
    void whenGetWatched_andExists_thenDtoReturned() {
        when(watchedVideoRepository.findById(1L))
                .thenReturn(Optional.of(existingWatched));

        WatchedVideoDto result = watchedVideoService.getWatched(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getVideoId()).isEqualTo(1L);
    }

    @Test
    void whenGetWatched_andMissing_thenExceptionThrown() {
        when(watchedVideoRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> watchedVideoService.getWatched(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("WatchedVideo not found");
    }

    @Test
    void whenToggleWatched_andExists_thenFlipsFlagAndReturnsDto() {
        existingWatched.setWatched(false);
        when(watchedVideoRepository.findById(1L))
                .thenReturn(Optional.of(existingWatched));
        when(watchedVideoRepository.save(existingWatched))
                .thenReturn(existingWatched);

        WatchedVideoDto result = watchedVideoService.toggleWatched(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.isWatched()).isTrue();
    }

    @Test
    void whenToggleWatched_andMissing_thenExceptionThrown() {
        when(watchedVideoRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> watchedVideoService.toggleWatched(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("WatchedVideo not found");
    }
}
