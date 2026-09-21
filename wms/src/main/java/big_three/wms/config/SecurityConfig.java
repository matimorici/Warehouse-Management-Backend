package big_three.wms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
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
                .sessionManagement(session -> session
                        .sessionFixation(fixation -> fixation.changeSessionId())
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/usuarios", "/api/auth/login", "/api/proveedores/**", "/api/productos/**", "/api/ordenes-retiro/**").permitAll() //lista de endpoints permitidos, modificar; placeholders hasta implementar Sessions (ver TODO: seguridad delegada)
                        .anyRequest().authenticated() // pide auth para todos los endpoints que no estén arriba
                );
        return http.build();
    }
    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetails, PasswordEncoder password) {
        DaoAuthenticationProvider authentication = new DaoAuthenticationProvider(userDetails);
        authentication.setPasswordEncoder(password);
        return authentication;
    }
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}

/*
 * ============================================================================
 * Autorización por rol — tabla de referencia (Fase 5)
 * ============================================================================
 * Definida en conjunto con el equipo. Regla general: GET = lectura (cualquier
 * rol autenticado), POST/PUT/DELETE = escritura (según se detalla abajo).
 * IMPORTANTE (ver explicación completa en la hoja de ruta): Spring Security
 * evalúa las reglas en orden y usa la PRIMERA que matchea. Las reglas de
 * método específico (GET/POST/...) tienen que declararse ANTES que cualquier
 * regla genérica que cubra el mismo path sin especificar método.
 *
 * Ruta                                  | Método            | Rol requerido
 * ---------------------------------------|--------------------|------------------
 * /api/auth/login                        | POST               | Público (sin rol)
 * /api/usuarios/**                       | -                  | ADMINISTRADOR
 * /api/proveedores/**                    | GET                | Cualquier rol
 * /api/proveedores/**                    | POST/PUT/DELETE    | ADMINISTRADOR
 * /api/productos/**                      | -                  | Cualquier rol
 * /api/ordenes-retiro/**                 | -                  | Cualquier rol
 *
 * Cualquier ruta no listada arriba: anyRequest().authenticated() como cierre
 * (falla "cerrado" por default ante endpoints nuevos que no se clasifiquen).
 * ============================================================================
 */
