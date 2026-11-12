package co.edu.uta.matricula.servicio;

import co.edu.uta.matricula.dominio.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Construye el horario semanal del estudiante a partir de su matrícula confirmada (RF-02). */
@Service
@Transactional(readOnly = true)
public class HorarioService {

    private final MatriculaService matriculaService;

    public HorarioService(MatriculaService matriculaService) {
        this.matriculaService = matriculaService;
    }

    public HorarioSemanal horarioDe(String idUsuario) {
        Optional<Matricula> actual = matriculaService.matriculaActual(idUsuario);
        if (actual.isEmpty() || actual.get().getEstado() != EstadoMatricula.CONFIRMADA) {
            return new HorarioSemanal(false, List.of(), 0);
        }
        Matricula m = actual.get();
        List<Bloque> bloques = new ArrayList<>();
        for (Curso c : m.cursosInscritos()) {
            for (FranjaHoraria f : c.getFranjas()) {
                bloques.add(new Bloque(f.getDia(), f.getHoraInicio(), f.getHoraFin(), f.getAula(),
                        c.getAsignatura().getNombre(), c.getGrupo(), c.getDocente()));
            }
        }
        bloques.sort(Comparator.comparing(Bloque::dia).thenComparing(Bloque::inicio));
        return new HorarioSemanal(true, bloques, m.calcularCreditos());
    }

    public record Bloque(DiaSemana dia, LocalTime inicio, LocalTime fin, String aula,
                         String asignatura, String grupo, String docente) {
    }

    public record HorarioSemanal(boolean confirmado, List<Bloque> bloques, int creditos) {

        public List<Bloque> delDia(DiaSemana dia) {
            return bloques.stream().filter(b -> b.dia() == dia).toList();
        }
    }
}
