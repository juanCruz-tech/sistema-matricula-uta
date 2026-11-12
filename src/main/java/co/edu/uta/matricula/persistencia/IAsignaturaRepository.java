package co.edu.uta.matricula.persistencia;

import co.edu.uta.matricula.dominio.Asignatura;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IAsignaturaRepository extends JpaRepository<Asignatura, Long> {
}
