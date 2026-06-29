package big_three.wms.service;

import big_three.wms.model.User;
import big_three.wms.dto.UserCreateDTO;
import big_three.wms.dto.UserResponseDTO;
import big_three.wms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public UserResponseDTO create(UserCreateDTO dto) {
        //validaciones de Negocio
        if (userRepository.existsByCuil(dto.getCuil())) {
            throw new IllegalArgumentException("Ya existe un usuario con ese CUIL" + dto.getCuil());
        }

        User u = new User();
        u.setNombre(dto.getNombre());
        u.setApellido(dto.getApellido());
        u.setCuil(dto.getCuil());
        u.setContrasena(passwordEncoder.encode(dto.getContrasena()));
        User savedUser= userRepository.save(u);
        return convertToResponseDTO(savedUser);
    }
    // Retorna una lista de DTOs mapeados
    public List<UserResponseDTO> findAll() {
        return userRepository.findAll()
                .stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    // Retorna un DTO de respuesta o lanza error
    public UserResponseDTO findById(UUID id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return convertToResponseDTO(u);
    }

    public void deleteById(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Usuario no encontrado para eliminar");
        }
        userRepository.deleteById(id);
    }


private UserResponseDTO convertToResponseDTO(User user) {
    UserResponseDTO response = new UserResponseDTO();
    response.setIdUsuario(user.getIdUsuario());
    response.setNombre(user.getNombre());
    response.setApellido(user.getApellido());
    response.setCuil(user.getCuil());
    response.setRol(user.getRol());
    return response;
    }
}