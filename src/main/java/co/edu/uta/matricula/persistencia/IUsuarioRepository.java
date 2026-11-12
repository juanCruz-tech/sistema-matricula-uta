package co.edu.uta.matricula.persistencia;

import co.edu.uta.matricula.dominio.Estudiante;
import co.edu.uta.matricula.dominio.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IUsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByIdUsuario(String idUsuario);

    @Query("select e from Estudiante e where e.idUsuario = :u")
    Optional<Estudiante> buscarEstudiante(@Param("u") String idUsuario);
}
