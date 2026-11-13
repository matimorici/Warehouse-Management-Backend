package big_three.wms.security;

import big_three.wms.model.User;
import big_three.wms.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resuelve el usuario autenticado de la petición actual a partir del CUIL que
 * Spring Security guarda como nombre de la autenticación.
 */
@Component
public class CurrentUserProvider {

    private final UserRepository userRepository;

    public CurrentUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Long requireCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("No hay un usuario autenticado");
        }

        return userRepository.findByCuil(authentication.getName())
                .map(User::getIdUsuario)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado"));
    }
}
