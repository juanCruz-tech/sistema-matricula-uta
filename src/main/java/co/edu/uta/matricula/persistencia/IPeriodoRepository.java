package co.edu.uta.matricula.persistencia;

import co.edu.uta.matricula.dominio.PeriodoAcademico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IPeriodoRepository extends JpaRepository<PeriodoAcademico, Long> {

    Optional<PeriodoAcademico> findFirstByAbiertoMatriculaTrueOrderByFechaInicioDesc();
}
