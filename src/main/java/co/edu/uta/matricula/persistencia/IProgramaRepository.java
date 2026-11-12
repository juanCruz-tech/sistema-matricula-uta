package co.edu.uta.matricula.persistencia;

import co.edu.uta.matricula.dominio.ProgramaAcademico;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IProgramaRepository extends JpaRepository<ProgramaAcademico, Long> {
}
