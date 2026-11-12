package co.edu.uta.matricula.persistencia;

import co.edu.uta.matricula.dominio.Facultad;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IFacultadRepository extends JpaRepository<Facultad, Long> {
}
