package co.edu.uta.matricula.funcional;

import co.edu.uta.matricula.dominio.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** CP-06 · RNF-04: autenticación, contraseñas cifradas y control de acceso por rol. */
class CP06SeguridadTest extends IntegracionBase {

    @Test
    @DisplayName("CP-06: sin sesión, cualquier pantalla redirige al inicio de sesión")
    void sinSesion() throws Exception {
        mvc.perform(get("/estudiante/oferta")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(get("/admin/reportes")).andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("CP-06: un estudiante no puede entrar a las pantallas del administrador y viceversa")
    void controlPorRol() throws Exception {
        mvc.perform(get("/admin/cursos").with(user("laura.gomez").roles("ESTUDIANTE"))).andExpect(status().isForbidden());
        mvc.perform(get("/estudiante/oferta").with(user("admin.registro").roles("ADMINISTRADOR"))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CP-06: las contraseñas se guardan con hash BCrypt, nunca en texto plano")
    void contrasenaCifrada() {
        Usuario u = usuarios.findByIdUsuario("laura.gomez").orElseThrow();
        assertThat(u.getContrasenaHash()).startsWith("$2a$").doesNotContain("Uta2027*");
        assertThat(codificador.matches("Uta2027*", u.getContrasenaHash())).isTrue();
    }

    @Test
    @DisplayName("CP-06: credenciales correctas inician sesión y las incorrectas no")
    void inicioDeSesion() throws Exception {
        mvc.perform(formLogin("/login").user("laura.gomez").password("Uta2027*")).andExpect(authenticated());
        mvc.perform(formLogin("/login").user("laura.gomez").password("incorrecta")).andExpect(unauthenticated());
    }

    @Test
    @DisplayName("CP-06: un formulario enviado sin token CSRF es rechazado")
    void csrf() throws Exception {
        mvc.perform(post("/estudiante/matricula").with(user("laura.gomez").roles("ESTUDIANTE")).param("cursoIds", "1"))
                .andExpect(status().isForbidden());
    }
}
