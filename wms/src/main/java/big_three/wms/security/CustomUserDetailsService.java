package big_three.wms.security;

import big_three.wms.repository.UserRepository;
import big_three.wms.model.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService{
    private final UserRepository userRepository;

    public CustomUserDetailsService (UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public UserDetails loadUserByUsername(String username) {
        User u = userRepository.findByCuil(username)
                .orElseThrow(() -> new UsernameNotFoundException("Cuil inexistente"));

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(u.getCuil())
                .password(u.getContrasena())
                .roles(u.getRol().name())
                .build();

        return userDetails;
    }
}
