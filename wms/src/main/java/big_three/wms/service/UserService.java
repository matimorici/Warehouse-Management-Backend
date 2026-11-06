package big_three.wms.service;

import big_three.wms.model.User;
import big_three.wms.dto.UserCreateDTO;
import big_three.wms.dto.UserResponseDTO;
import big_three.wms.exception.InvalidCredentialsException;
import big_three.wms.model.Role;
import big_three.wms.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponseDTO create(UserCreateDTO dto) {
        if (userRepository.existsByCuil(dto.getCuil())) {
            throw new IllegalArgumentException("Ya existe un usuario con ese CUIL: " + dto.getCuil());
        }

        User u = new User();
        u.setNombre(dto.getNombre());
        u.setApellido(dto.getApellido());
        u.setCuil(dto.getCuil());
        u.setRol(dto.getRol() != null ? dto.getRol() : Role.OPERARIO);
        u.setContrasena(passwordEncoder.encode(dto.getContrasena()));
        User savedUser = userRepository.save(u);
        return convertToResponseDTO(savedUser);
    }

    @Transactional
    public void deleteById(Long id) {
        User u = userRepository.findByIdUsuarioAndFechaBajaIsNull(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado para eliminar"));
        u.setFechaBaja(LocalDateTime.now());
        userRepository.save(u);
    }

    public List<UserResponseDTO> findAll() {
        return userRepository.findByFechaBajaIsNull().stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public UserResponseDTO findById(Long id) {
        User u = userRepository.findByIdUsuarioAndFechaBajaIsNull(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return convertToResponseDTO(u);
    }

    public UserResponseDTO findByCuil(String cuil) {
        User u = userRepository.findByCuilAndFechaBajaIsNull(cuil)
                .orElseThrow(() -> new InvalidCredentialsException("CUIL o contraseña incorrectos"));
        return convertToResponseDTO(u);
    }


    private UserResponseDTO convertToResponseDTO(User user) {
        UserResponseDTO response = new UserResponseDTO();
        response.setIdUsuario(user.getIdUsuario());
        response.setNombre(user.getNombre());
        response.setApellido(user.getApellido());
        response.setCuil(user.getCuil());
        response.setRol(user.getRol());
        response.setFechaBaja(user.getFechaBaja());
        return response;
    }
}
