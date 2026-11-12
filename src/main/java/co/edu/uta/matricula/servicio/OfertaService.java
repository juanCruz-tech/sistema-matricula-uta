package co.edu.uta.matricula.servicio;

import co.edu.uta.matricula.dominio.Curso;
import co.edu.uta.matricula.dominio.Estudiante;
import co.edu.uta.matricula.dominio.PeriodoAcademico;
import co.edu.uta.matricula.dominio.ReglaNegocioException;
import co.edu.uta.matricula.persistencia.ICursoRepository;
import co.edu.uta.matricula.persistencia.IPeriodoRepository;
import co.edu.uta.matricula.persistencia.IUsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Consulta de la oferta académica del periodo abierto. */
@Service
@Transactional(readOnly = true)
public class OfertaService {

    private final IPeriodoRepository periodos;
    private final ICursoRepository cursos;
    private final IUsuarioRepository usuarios;

    public OfertaService(IPeriodoRepository periodos, ICursoRepository cursos, IUsuarioRepository usuarios) {
        this.periodos = periodos;
        this.cursos = cursos;
        this.usuarios = usuarios;
    }

    public PeriodoAcademico periodoAbierto() {
        return periodos.findFirstByAbiertoMatriculaTrueOrderByFechaInicioDesc()
                .orElseThrow(() -> new ReglaNegocioException("No hay un periodo abierto para matrícula."));
    }

    public List<Curso> cursosDelPeriodo(PeriodoAcademico periodo) {
        return cursos.buscarPorPeriodo(periodo);
    }

    /** Cursos del periodo abierto que pertenecen al programa del estudiante. */
    public List<Curso> cursosParaEstudiante(String idUsuario) {
        Estudiante e = usuarios.buscarEstudiante(idUsuario)
                .orElseThrow(() -> new ReglaNegocioException("El usuario no es un estudiante."));
        Long programaId = e.getPrograma().getId();
        return cursos.buscarPorPeriodo(periodoAbierto()).stream()
                .filter(c -> c.getAsignatura().getPrograma().getId().equals(programaId))
                .toList();
    }

    /** Nombre y programa del estudiante para mostrarlos en pantalla. */
    public String descripcionEstudiante(String idUsuario) {
        return usuarios.buscarEstudiante(idUsuario)
                .map(e -> e.getNombre() + " · " + e.getCodigo() + " · " + e.getPrograma().getNombre())
                .orElse(idUsuario);
    }
}
