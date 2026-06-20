package big_three.wms.service;

import big_three.wms.model.User;
import big_three.wms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    public User crear(String nombre, String apellido, String cuil, String rol) {
        User u = new User();
        u.setId_usuario(UUID.randomUUID());   // lo generamos en Java, como hablamos
        u.setNombre(nombre);
        u.setApellido(apellido);
        u.setCuil(cuil);
        u.setRol(rol);
        return userRepository.save(u);
    }

    public void deleteById(UUID id) {
        userRepository.deleteById(id);
    }
}
