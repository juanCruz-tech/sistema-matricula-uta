package co.edu.uta.matricula.seguridad;

import co.edu.uta.matricula.dominio.Usuario;
import co.edu.uta.matricula.persistencia.IUsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Adapta el Usuario del dominio al modelo de Spring Security para la autenticación. */
@Service
public class UsuarioDetallesService implements UserDetailsService {

    private final IUsuarioRepository usuarios;

    public UsuarioDetallesService(IUsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public UserDetails loadUserByUsername(String idUsuario) {
        Usuario u = usuarios.findByIdUsuario(idUsuario)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario o contraseña incorrectos"));
        return User.withUsername(u.getIdUsuario())
                .password(u.getContrasenaHash())
                .roles(u.obtenerRol().name())
                .build();
    }
}
