package co.edu.uta.matricula.servicio;

import co.edu.uta.matricula.dominio.Curso;
import co.edu.uta.matricula.dominio.ReglaNegocioException;
import co.edu.uta.matricula.persistencia.ICursoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Única responsabilidad: los cupos (principio SRP). Reserva y libera cupos con operaciones
 * atómicas en la base de datos para que nunca haya sobrecupo (RF-03).
 */
@Service
public class CupoService {

    private final ICursoRepository cursos;

    public CupoService(ICursoRepository cursos) {
        this.cursos = cursos;
    }

    /** Reserva un cupo. Participa en la transacción de quien lo llama. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean reservar(Long cursoId) {
        return cursos.decrementarCupo(cursoId) == 1;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void liberar(Long cursoId) {
        cursos.incrementarCupo(cursoId);
    }

    @Transactional(readOnly = true)
    public int consultarDisponibles(Long cursoId) {
        return cursos.findById(cursoId).map(Curso::getCuposDisponibles)
                .orElseThrow(() -> new ReglaNegocioException("El curso no existe."));
    }

    /** Ajuste del cupo máximo por el administrador, hecho de forma atómica en la base de datos. */
    @Transactional
    public Curso gestionarCupo(Long cursoId, int nuevoMaximo) {
        if (nuevoMaximo < 1) {
            throw new ReglaNegocioException("El cupo máximo debe ser al menos 1.");
        }
        Curso curso = cursos.findById(cursoId).orElseThrow(() -> new ReglaNegocioException("El curso no existe."));
        if (cursos.ajustarCupoMaximo(cursoId, nuevoMaximo) == 0) {
            int ocupados = curso.getCupoMaximo() - curso.getCuposDisponibles();
            throw new ReglaNegocioException("El nuevo cupo (" + nuevoMaximo
                    + ") es menor que los cupos ya ocupados (" + ocupados + ").");
        }
        return cursos.findById(cursoId).orElseThrow();
    }
}
