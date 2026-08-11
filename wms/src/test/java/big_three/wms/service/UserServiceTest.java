package big_three.wms.service;

import big_three.wms.dto.LoginRequestDTO;
import big_three.wms.dto.UserCreateDTO;
import big_three.wms.dto.UserResponseDTO;
import big_three.wms.exception.InvalidCredentialsException;
import big_three.wms.model.User;
import big_three.wms.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UserCreateDTO validCreateDto() {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setNombre("Juan");
        dto.setApellido("Perez");
        dto.setCuil("20-12345678-9");
        dto.setContrasena("Password1");
        return dto;
    }

    private User user(Long id, String cuil, String hash) {
        User user = new User();
        user.setIdUsuario(id);
        user.setNombre("Juan");
        user.setApellido("Perez");
        user.setCuil(cuil);
        user.setRol("OPERARIO");
        user.setContrasena(hash);
        return user;
    }

    @Test
    void create_encodesPasswordBeforeSaving() {
        UserCreateDTO dto = validCreateDto();
        when(passwordEncoder.encode(dto.getContrasena())).thenReturn("hashed-bcrypt");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            saved.setIdUsuario(1L);
            return saved;
        });

        UserResponseDTO response = userService.create(dto);

        assertNotNull(response);
        assertEquals(1L, response.getIdUsuario());
        assertEquals("Juan", response.getNombre());
        assertEquals("OPERARIO", response.getRol());
        verify(userRepository).save(argThat(u -> "hashed-bcrypt".equals(u.getContrasena())));
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void create_duplicateCuil_throws() {
        UserCreateDTO dto = validCreateDto();
        when(userRepository.existsByCuil(dto.getCuil())).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.create(dto));

        assertTrue(ex.getMessage().contains(dto.getCuil()));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_validCredentials_returnsUserDto() {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setCuil("20-12345678-9");
        dto.setContrasena("Password1");
        when(userRepository.findByCuil(dto.getCuil())).thenReturn(Optional.of(user(1L, dto.getCuil(), "hash")));
        when(passwordEncoder.matches(dto.getContrasena(), "hash")).thenReturn(true);

        UserResponseDTO response = userService.login(dto);

        assertEquals(1L, response.getIdUsuario());
        assertEquals(dto.getCuil(), response.getCuil());
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setCuil("20-12345678-9");
        dto.setContrasena("WrongPass1");
        when(userRepository.findByCuil(dto.getCuil())).thenReturn(Optional.of(user(1L, dto.getCuil(), "hash")));
        when(passwordEncoder.matches(dto.getContrasena(), "hash")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> userService.login(dto));
    }

    @Test
    void login_unknownCuil_throwsInvalidCredentials() {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setCuil("20-12345678-9");
        dto.setContrasena("Password1");
        when(userRepository.findByCuil(dto.getCuil())).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> userService.login(dto));
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void findAll_mapsUsersToDtos() {
        when(userRepository.findAll()).thenReturn(List.of(user(1L, "20-12345678-9", "hash")));

        List<UserResponseDTO> response = userService.findAll();

        assertEquals(1, response.size());
        assertEquals(1L, response.get(0).getIdUsuario());
    }

    @Test
    void findById_notFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.findById(99L));
    }

    @Test
    void deleteById_notFound_throws() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> userService.deleteById(99L));
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void deleteById_existing_deletes() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteById(1L);

        verify(userRepository).deleteById(1L);
    }
}
