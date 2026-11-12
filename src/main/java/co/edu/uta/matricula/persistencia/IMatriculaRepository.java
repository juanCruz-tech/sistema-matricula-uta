package co.edu.uta.matricula.persistencia;

import co.edu.uta.matricula.dominio.EstadoMatricula;
import co.edu.uta.matricula.dominio.Estudiante;
import co.edu.uta.matricula.dominio.Matricula;
import co.edu.uta.matricula.dominio.PeriodoAcademico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Interfaz IMatriculaRepository de la Figura 5. Spring Data JPA genera su implementación
 * (el equivalente a MatriculaRepositoryJPA del diagrama) al iniciar la aplicación.
 */
public interface IMatriculaRepository extends JpaRepository<Matricula, Long> {

    @Query("select m from Matricula m where m.estudiante = :e and m.periodo = :p and m.estado <> co.edu.uta.matricula.dominio.EstadoMatricula.ANULADA")
    Optional<Matricula> buscarActiva(@Param("e") Estudiante estudiante, @Param("p") PeriodoAcademico periodo);

    @Query("select m from Matricula m where m.estudiante = :e and m.periodo = :p order by m.fechaRegistro desc")
    List<Matricula> buscarHistorial(@Param("e") Estudiante estudiante, @Param("p") PeriodoAcademico periodo);

    List<Matricula> findByEstadoAndFechaReservaBefore(EstadoMatricula estado, LocalDateTime limite);

    @Query("select m from Matricula m join fetch m.estudiante e join fetch e.programa pr join fetch pr.facultad f "
            + "where m.periodo = :p and m.estado = co.edu.uta.matricula.dominio.EstadoMatricula.CONFIRMADA "
            + "order by f.nombre, pr.nombre, e.nombre")
    List<Matricula> buscarConfirmadasPorPeriodo(@Param("p") PeriodoAcademico periodo);

    long countByEstado(EstadoMatricula estado);
}
