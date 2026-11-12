package co.edu.uta.matricula.dominio;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Objetos de dominio en memoria para las pruebas unitarias (sin base de datos). */
public final class Fixtures {

    public static final Facultad FACULTAD = new Facultad("ING", "Facultad de Ingeniería");
    public static final ProgramaAcademico PROGRAMA = new ProgramaAcademico("ISIS", "Ingeniería de Sistemas", FACULTAD);
    public static final PeriodoAcademico PERIODO = new PeriodoAcademico("2027-1",
            LocalDate.of(2027, 1, 25), LocalDate.of(2027, 5, 29), true);

    private Fixtures() {
    }

    public static Curso curso(String codigo, int creditos, int cupo, DiaSemana dia, int desde, int hasta) {
        Asignatura a = new Asignatura(codigo, "Asignatura " + codigo, creditos, PROGRAMA);
        return new Curso(a, PERIODO, "1", cupo, "Docente",
                List.of(new FranjaHoraria(dia, LocalTime.of(desde, 0), LocalTime.of(hasta, 0), "A-1")));
    }

    public static Estudiante estudiante() {
        return new Estudiante("prueba", "Estudiante de Prueba", "prueba@uta.edu.co", "hash",
                "2027000", PROGRAMA, EstadoAcademico.ACTIVO);
    }
}
