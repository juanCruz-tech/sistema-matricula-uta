package co.edu.uta.matricula.funcional;

import co.edu.uta.matricula.dominio.*;
import co.edu.uta.matricula.integracion.pagos.PasarelaSimulada;
import co.edu.uta.matricula.persistencia.*;
import co.edu.uta.matricula.servicio.MatriculaService;
import co.edu.uta.matricula.servicio.OfertaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base de las pruebas de integración: levanta la aplicación completa con base de datos H2 y
 * crea datos propios para cada prueba (estudiantes y cursos nuevos) para no depender del orden.
 */
@SpringBootTest(properties = "matricula.revision-vencidas-ms=3600000")
@AutoConfigureMockMvc
@Import(IntegracionBase.ConfigPruebas.class)
public abstract class IntegracionBase {

    private static final AtomicInteger SECUENCIA = new AtomicInteger();
    private static volatile String hashClave;

    @Autowired protected MockMvc mvc;
    @Autowired protected MatriculaService matriculaService;
    @Autowired protected OfertaService ofertaService;
    @Autowired protected PasarelaSimulada pasarela;
    @Autowired protected ICursoRepository cursos;
    @Autowired protected IAsignaturaRepository asignaturas;
    @Autowired protected IProgramaRepository programas;
    @Autowired protected IUsuarioRepository usuarios;
    @Autowired protected IMatriculaRepository matriculas;
    @Autowired protected PasswordEncoder codificador;
    @Autowired protected RelojAjustable reloj;

    @TestConfiguration
    static class ConfigPruebas {
        @Bean
        @Primary
        RelojAjustable relojAjustable() {
            return new RelojAjustable();
        }
    }

    protected String nuevoEstudiante() {
        return nuevoEstudiante(EstadoAcademico.ACTIVO);
    }

    protected String nuevoEstudiante(EstadoAcademico estado) {
        int n = SECUENCIA.incrementAndGet();
        String id = "prueba" + n + "." + System.nanoTime() % 100000;
        ProgramaAcademico isis = programas.findAll().stream().filter(p -> p.getCodigo().equals("ISIS")).findFirst().orElseThrow();
        usuarios.save(new Estudiante(id, "Estudiante de Prueba " + n, id + "@pruebas.uta.edu.co",
                hash(), "T" + n, isis, estado));
        return id;
    }

    private String hash() {
        if (hashClave == null) {
            hashClave = codificador.encode("Clave123*");
        }
        return hashClave;
    }

    protected Curso nuevoCurso(int cupo, DiaSemana dia, int desde, int hasta) {
        int n = SECUENCIA.incrementAndGet();
        ProgramaAcademico isis = programas.findAll().stream().filter(p -> p.getCodigo().equals("ISIS")).findFirst().orElseThrow();
        Asignatura a = asignaturas.save(new Asignatura("PR-" + n + "-" + System.nanoTime() % 10000,
                "Asignatura de prueba " + n, 3, isis));
        return cursos.save(new Curso(a, ofertaService.periodoAbierto(), "1", cupo, "Docente de prueba",
                List.of(new FranjaHoraria(dia, LocalTime.of(desde, 0), LocalTime.of(hasta, 0), "PR-1"))));
    }

    protected int cuposDe(Curso c) {
        return cursos.findById(c.getId()).orElseThrow().getCuposDisponibles();
    }
}
