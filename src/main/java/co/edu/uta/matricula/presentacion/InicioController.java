package co.edu.uta.matricula.presentacion;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Entrada al sistema: inicio de sesión y redirección según el rol. */
@Controller
public class InicioController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String inicio(Authentication auth) {
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));
        return admin ? "redirect:/admin/cursos" : "redirect:/estudiante/oferta";
    }

    @GetMapping("/acceso-denegado")
    public String accesoDenegado() {
        return "acceso-denegado";
    }
}
