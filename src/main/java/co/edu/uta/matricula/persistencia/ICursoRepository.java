package co.edu.uta.matricula.persistencia;

import co.edu.uta.matricula.dominio.Curso;
import co.edu.uta.matricula.dominio.PeriodoAcademico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ICursoRepository extends JpaRepository<Curso, Long> {

    @Query("select c from Curso c join fetch c.asignatura a where c.periodo = :periodo order by a.nombre, c.grupo")
    List<Curso> buscarPorPeriodo(@Param("periodo") PeriodoAcademico periodo);

    /**
     * Reserva atómica de un cupo (RF-03). El UPDATE solo afecta la fila si todavía queda cupo,
     * y la base de datos bloquea la fila mientras lo hace: dos solicitudes simultáneas nunca
     * pueden tomar el mismo último cupo. Devuelve 1 si reservó y 0 si no había cupo.
     */
    @Modifying(flushAutomatically = true)
    @Query("update Curso c set c.cuposDisponibles = c.cuposDisponibles - 1 where c.id = :id and c.cuposDisponibles > 0")
    int decrementarCupo(@Param("id") Long id);

    /** Libera un cupo sin superar nunca el cupo máximo. */
    @Modifying(flushAutomatically = true)
    @Query("update Curso c set c.cuposDisponibles = c.cuposDisponibles + 1 where c.id = :id and c.cuposDisponibles < c.cupoMaximo")
    int incrementarCupo(@Param("id") Long id);

    /** Ajuste atómico del cupo máximo; no aplica si el nuevo máximo es menor que los cupos ocupados. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Curso c set c.cuposDisponibles = c.cuposDisponibles + (:nuevo - c.cupoMaximo), c.cupoMaximo = :nuevo "
            + "where c.id = :id and (c.cupoMaximo - c.cuposDisponibles) <= :nuevo")
    int ajustarCupoMaximo(@Param("id") Long id, @Param("nuevo") int nuevoMaximo);
}
