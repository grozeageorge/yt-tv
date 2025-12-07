package com.example.yt_tv;

import com.example.yt_tv.dtos.DtoMapper;
import com.example.yt_tv.dtos.UserCreateDto;
import com.example.yt_tv.dtos.UserDto;
import com.example.yt_tv.entities.User;
import com.example.yt_tv.repositories.UserRepository;
import com.example.yt_tv.services.UserService;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Spy
    private DtoMapper dtoMapper;

    @InjectMocks
    private UserService userService;

    private User user;
    private User user1;
    private User user2;
    private User saved;
    private UserCreateDto userCreateDto;
    private UserCreateDto userUpdateDto;

    @BeforeEach
    void setUp() {
        user = TestDataFactory.user(1L, "John", "john@example.com", "johnpass");
        user1 = user;
        user2 = TestDataFactory.user(2L, "Jane", "jane@example.com", "janepass");
        saved = user2;
        userCreateDto = TestDataFactory.userCreateDto("John", "john@example.com", "johnpass");
        userUpdateDto = TestDataFactory.userCreateDto("Jane", "jane@example.com", "janepass");
    }

    @Test
    void whenGetUser_andUserExists_thenDtoShouldBeReturned() {
        // ARRANGE
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        //ACT
        UserDto userDto = userService.get(1L);

        //ASSERT
        assertThat(userDto).isNotNull();
        assertThat(userDto.getUsername()).isEqualTo("John");

        verify(dtoMapper, times(1)).toUserDto(any(User.class));
    }

    @Test
    void whenGetUser_andUserDoesNotExist_thenThrowException() {
        //ARRANGE
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        //ACT & ASSERT
        assertThatThrownBy(() -> userService.get(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found with id: 1");
    }

    @Test
    void whenCreate_UserShouldSaveAndReturnDto() {
        when(dtoMapper.toUserEntity(userCreateDto)).thenReturn(user);

        User saved = user;
        when(userRepository.save(user)).thenReturn(saved);

        UserDto result = userService.create(userCreateDto);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("John");

        verify(dtoMapper).toUserEntity(userCreateDto);
        verify(userRepository).save(user);
        verify(dtoMapper).toUserDto(saved);
    }

    @Test
    void whenList_thenReturnMappedDtos() {
        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        List<UserDto> result = userService.list();

        assertThat(result).hasSize(2);
        verify(dtoMapper, times(2)).toUserDto(any(User.class));
    }

    @Test
    void whenUpdate_andUserExists_thenModifyAndReturnDto() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        when(userRepository.save(user)).thenReturn(saved);

        UserDto result = userService.update(1L, userUpdateDto);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("Jane");
        verify(userRepository).findById(1L);
        verify(userRepository).save(user);
        verify(dtoMapper).toUserDto(saved);
    }

    @Test
    void whenUpdate_andUserDoesNotExist_thenThrowException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(1L, userUpdateDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found with id: 1");
    }

    @Test
    void whenDelete_andUserExists_thenDeleteById() {
        doNothing().when(userRepository).deleteById(1L);

        userService.delete(1L);

        verify(userRepository).deleteById(1L);
    }
}
