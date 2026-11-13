package big_three.wms.security;

import big_three.wms.model.Role;
import big_three.wms.model.User;
import big_three.wms.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private User user(String cuil, LocalDateTime fechaBaja) {
        User user = new User();
        user.setIdUsuario(1L);
        user.setCuil(cuil);
        user.setContrasena("bcrypt-hash");
        user.setRol(Role.OPERARIO);
        user.setFechaBaja(fechaBaja);
        return user;
    }

    @Test
    void loadUserByUsername_activeUser_returnsEnabledUserDetails() {
        when(userRepository.findByCuil("20-12345678-9"))
                .thenReturn(Optional.of(user("20-12345678-9", null)));

        UserDetails details = userDetailsService.loadUserByUsername("20-12345678-9");

        assertEquals("20-12345678-9", details.getUsername());
        assertEquals("bcrypt-hash", details.getPassword());
        assertTrue(details.isEnabled());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_OPERARIO")));
    }

    @Test
    void loadUserByUsername_deletedUser_returnsDisabledUserDetails() {
        when(userRepository.findByCuil("20-12345678-9"))
                .thenReturn(Optional.of(user("20-12345678-9", LocalDateTime.now().minusDays(1))));

        UserDetails details = userDetailsService.loadUserByUsername("20-12345678-9");

        assertFalse(details.isEnabled());
    }

    @Test
    void loadUserByUsername_unknownCuil_throws() {
        when(userRepository.findByCuil("20-99999999-9")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("20-99999999-9"));
    }
}
