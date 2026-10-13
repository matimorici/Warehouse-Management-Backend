package big_three.wms.config;

import big_three.wms.security.CsrfCookieFilter;
import big_three.wms.security.SpaCsrfTokenRequestHandler;
import big_three.wms.security.LoggingAccessDeniedHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean //metodo de hash de password
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean // Autenticacion y autorización de la app
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
                .sessionManagement(session -> session
                        .sessionFixation(fixation -> fixation.changeSessionId())
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/usuarios/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/productos/**").hasAnyRole("ADMINISTRADOR", "OPERARIO")
                        .requestMatchers("/api/ordenes-retiro/**").hasAnyRole("ADMINISTRADOR", "OPERARIO")
                        .requestMatchers("/api/ordenes-compra/**").hasAnyRole("ADMINISTRADOR", "OPERARIO")
                        .requestMatchers("/api/ubicaciones/**").hasAnyRole("ADMINISTRADOR", "OPERARIO")
                        .requestMatchers("/api/valoraciones-proveedor/**").hasAnyRole("ADMINISTRADOR", "OPERARIO")
                        .requestMatchers("/api/movimientos-fisicos/**").hasAnyRole("ADMINISTRADOR", "OPERARIO")
                        .requestMatchers(HttpMethod.GET, "/api/proveedores/**").hasAnyRole("ADMINISTRADOR", "OPERARIO")
                        .requestMatchers("/api/proveedores/**").hasRole("ADMINISTRADOR")
                        .anyRequest().authenticated() // pide auth para todos los endpoints que no estén arriba
                )
                .exceptionHandling(ex -> ex.accessDeniedHandler(new LoggingAccessDeniedHandler()));
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
    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origin}") String allowedOrigin) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigin));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
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
 * Ruta                                   | Método            | Rol requerido
 * ---------------------------------------|--------------------|------------------
 * /api/auth/login                        | POST               | Público (sin rol)
 * /api/usuarios/**                       | -                  | ADMINISTRADOR
 * /api/proveedores/**                    | GET                | Cualquier rol
 * /api/proveedores/**                    | POST/PUT/DELETE    | ADMINISTRADOR
 * /api/productos/**                      | -                  | Cualquier rol
 * /api/ordenes-retiro/**                 | -                  | Cualquier rol
 * /api/ordenes-compra/**                 | -                  | Cualquier rol
 * /api/ubicaciones/**                    | -                  | Cualquier rol
 * /api/valoraciones-proveedor/**         | -                  | Cualquier rol
 * /api/movimientos-fisicos/**            | -                  | Cualquier rol
 * Cualquier ruta no listada arriba: anyRequest().authenticated() como cierre
 * (falla "cerrado" por default ante endpoints nuevos que no se clasifiquen).
 * ============================================================================
 */
