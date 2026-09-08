package big_three.wms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean //metodo de hash de password
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean // Autenticacion y autorización de la app
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) //desabilita protecciones, quitar el disable para implementar Sessions o Cookies
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/usuarios" /*ADMIN*/, "/api/auth/login"/*SIN ROL*/, "/api/proveedores/**"/*ADMIN?*/, "/api/productos/**"/*CUALQUIER ROL*/, "/api/ordenes-retiro/**"/*CUALQUIER ROL*/).permitAll() //lista de endpoints permitidos, modificar; placeholders hasta implementar Sessions (ver TODO: seguridad delegada)
                        .anyRequest().authenticated() // pide auth para todos los endpoints que no estén arriba
                );
        return http.build();
    }
}
