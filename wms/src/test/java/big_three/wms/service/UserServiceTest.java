package big_three.wms.service;

import big_three.wms.dto.UserCreateDTO;
import big_three.wms.dto.UserResponseDTO;
import big_three.wms.exception.InvalidCredentialsException;
import big_three.wms.model.Role;
import big_three.wms.model.User;
import big_three.wms.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
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
        user.setRol(Role.OPERARIO);
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
        assertEquals(Role.OPERARIO, response.getRol());
        verify(userRepository).save(argThat(u -> "hashed-bcrypt".equals(u.getContrasena())));
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void create_preservesRequestedRole() {
        UserCreateDTO dto = validCreateDto();
        dto.setRol(Role.ADMINISTRADOR);
        when(passwordEncoder.encode(dto.getContrasena())).thenReturn("hashed-bcrypt");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponseDTO response = userService.create(dto);

        assertEquals(Role.ADMINISTRADOR, response.getRol());
        verify(userRepository).save(argThat(u -> u.getRol() == Role.ADMINISTRADOR));
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
    void findByCuil_existingActiveUser_returnsUserDto() {
        User existing = user(1L, "20-12345678-9", "hash");
        existing.setFechaBaja(null);
        when(userRepository.findByCuilAndFechaBajaIsNull("20-12345678-9")).thenReturn(Optional.of(existing));

        UserResponseDTO response = userService.findByCuil("20-12345678-9");

        assertEquals(1L, response.getIdUsuario());
        assertEquals("20-12345678-9", response.getCuil());
        assertNull(response.getFechaBaja());
        verify(userRepository).findByCuilAndFechaBajaIsNull("20-12345678-9");
    }

    @Test
    void findByCuil_unknownOrDeactivatedCuil_throwsInvalidCredentials() {
        when(userRepository.findByCuilAndFechaBajaIsNull("20-12345678-9")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> userService.findByCuil("20-12345678-9"));
    }

    @Test
    void findAll_returnsOnlyActiveUsers() {
        when(userRepository.findByFechaBajaIsNull())
                .thenReturn(List.of(user(1L, "20-12345678-9", "hash")));

        List<UserResponseDTO> response = userService.findAll();

        assertEquals(1, response.size());
        assertEquals(1L, response.get(0).getIdUsuario());
        verify(userRepository).findByFechaBajaIsNull();
    }

    @Test
    void findById_notFoundOrDeactivated_throws() {
        when(userRepository.findByIdUsuarioAndFechaBajaIsNull(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.findById(99L));
    }

    @Test
    void deleteById_notFoundOrDeactivated_throws() {
        when(userRepository.findByIdUsuarioAndFechaBajaIsNull(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.deleteById(99L));
        verify(userRepository, never()).save(any());
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void deleteById_existingActiveUser_setsFechaBajaAndSaves() {
        User existing = user(1L, "20-12345678-9", "hash");
        when(userRepository.findByIdUsuarioAndFechaBajaIsNull(1L)).thenReturn(Optional.of(existing));

        userService.deleteById(1L);

        assertNotNull(existing.getFechaBaja());
        verify(userRepository).save(existing);
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void deleteById_alreadyDeactivated_throws() {
        User deactivated = user(1L, "20-12345678-9", "hash");
        deactivated.setFechaBaja(LocalDateTime.now().minusDays(1));
        when(userRepository.findByIdUsuarioAndFechaBajaIsNull(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.deleteById(1L));
        verify(userRepository, never()).save(any());
        verify(userRepository, never()).deleteById(any());
    }
}
