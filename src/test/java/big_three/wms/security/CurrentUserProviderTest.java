package big_three.wms.security;

import big_three.wms.model.User;
import big_three.wms.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserProviderTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CurrentUserProvider currentUserProvider;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String cuil) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(cuil, "password", AuthorityUtils.NO_AUTHORITIES));
    }

    @Test
    void requireCurrentUserId_authenticatedUser_returnsId() {
        authenticateAs("20-12345678-9");
        User user = new User();
        user.setIdUsuario(7L);
        user.setCuil("20-12345678-9");
        when(userRepository.findByCuil("20-12345678-9")).thenReturn(Optional.of(user));

        assertEquals(7L, currentUserProvider.requireCurrentUserId());
    }

    @Test
    void requireCurrentUserId_noAuthentication_throws() {
        SecurityContextHolder.clearContext();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> currentUserProvider.requireCurrentUserId());

        assertEquals("No hay un usuario autenticado", ex.getMessage());
    }

    @Test
    void requireCurrentUserId_anonymous_throws() {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anonymousUser",
                        AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> currentUserProvider.requireCurrentUserId());

        assertEquals("No hay un usuario autenticado", ex.getMessage());
    }

    @Test
    void requireCurrentUserId_unknownCuil_throws() {
        authenticateAs("20-99999999-9");
        when(userRepository.findByCuil("20-99999999-9")).thenReturn(Optional.empty());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> currentUserProvider.requireCurrentUserId());

        assertEquals("Usuario autenticado no encontrado", ex.getMessage());
    }
}
