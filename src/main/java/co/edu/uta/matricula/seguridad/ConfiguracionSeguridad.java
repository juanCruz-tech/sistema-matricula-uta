package co.edu.uta.matricula.seguridad;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Módulo de Seguridad (componente reutilizable Spring Security, enfoque CBSE).
 * Control de acceso por rol (RNF-04): /estudiante/** solo para estudiantes y /admin/** solo para
 * administradores. Las contraseñas se almacenan con BCrypt y los formularios usan token CSRF.
 */
@Configuration
public class ConfiguracionSeguridad {

    @Bean
    public SecurityFilterChain cadenaDeFiltros(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/css/**", "/login", "/error").permitAll()
                        .requestMatchers("/estudiante/**").hasRole("ESTUDIANTE")
                        .requestMatchers("/admin/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/pasarela-simulada/**").authenticated()
                        .anyRequest().authenticated())
                .formLogin(f -> f.loginPage("/login").defaultSuccessUrl("/", true).permitAll())
                .logout(l -> l.logoutUrl("/logout").logoutSuccessUrl("/login?salida").permitAll())
                .exceptionHandling(e -> e.accessDeniedPage("/acceso-denegado"))
                .headers(h -> h.frameOptions(f -> f.sameOrigin()));
        return http.build();
    }

    @Bean
    public PasswordEncoder codificador() {
        return new BCryptPasswordEncoder();
    }
}
