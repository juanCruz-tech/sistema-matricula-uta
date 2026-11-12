package co.edu.uta.matricula.dominio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static co.edu.uta.matricula.dominio.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas unitarias de la máquina de estados de Matricula (Figura 8 de la Fase 2). */
class MatriculaTest {

    private final LocalDateTime ahora = LocalDateTime.of(2027, 1, 10, 8, 0);

    private Matricula conDosCursos() {
        Matricula m = new Matricula(estudiante(), PERIODO, ahora);
        m.agregarCurso(curso("M-1", 3, 10, DiaSemana.LUNES, 7, 9), ahora);
        m.agregarCurso(curso("M-2", 4, 10, DiaSemana.MARTES, 7, 9), ahora);
        return m;
    }

    @Test
    @DisplayName("Una matrícula nueva empieza en BORRADOR y suma los créditos de sus cursos")
    void borradorYCreditos() {
        Matricula m = conDosCursos();
        assertThat(m.getEstado()).isEqualTo(EstadoMatricula.BORRADOR);
        assertThat(m.calcularCreditos()).isEqualTo(7);
        assertThat(m.calcularValor(new BigDecimal("350000"))).isEqualByComparingTo("2450000");
    }

    @Test
    @DisplayName("No permite agregar un curso que se cruza en horario")
    void rechazaCruce() {
        Matricula m = conDosCursos();
        Curso cruzado = curso("M-3", 3, 10, DiaSemana.LUNES, 8, 10);
        assertThatThrownBy(() -> m.agregarCurso(cruzado, ahora))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("Cruce de horario");
    }

    @Test
    @DisplayName("No permite dos grupos de la misma asignatura")
    void rechazaAsignaturaRepetida() {
        Matricula m = conDosCursos();
        Curso otroGrupo = curso("M-1", 3, 10, DiaSemana.VIERNES, 7, 9);
        assertThatThrownBy(() -> m.agregarCurso(otroGrupo, ahora)).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("Flujo feliz: BORRADOR → PENDIENTE_PAGO → CONFIRMADA con pago aprobado")
    void flujoConfirmado() {
        Matricula m = conDosCursos();
        m.confirmarSeleccion(ahora);
        assertThat(m.getEstado()).isEqualTo(EstadoMatricula.PENDIENTE_PAGO);
        Pago pago = new Pago("REF-1", new BigDecimal("2450000"));
        m.asociarPago(pago);
        pago.registrarRespuesta(EstadoPago.APROBADO, ahora);
        m.confirmar(new Comprobante("UTA-1", ahora, "ABC"));
        assertThat(m.getEstado()).isEqualTo(EstadoMatricula.CONFIRMADA);
        assertThat(m.getComprobante().getNumero()).isEqualTo("UTA-1");
    }

    @Test
    @DisplayName("No se puede confirmar sin un pago aprobado")
    void sinPagoNoConfirma() {
        Matricula m = conDosCursos();
        m.confirmarSeleccion(ahora);
        m.asociarPago(new Pago("REF-2", BigDecimal.TEN));
        assertThatThrownBy(() -> m.confirmar(new Comprobante("UTA-2", ahora, "X")))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("Anular una matrícula pendiente devuelve los cursos cuyos cupos deben liberarse")
    void anularLiberaCupos() {
        Matricula m = conDosCursos();
        m.confirmarSeleccion(ahora);
        List<Curso> liberar = m.anular("Pago rechazado");
        assertThat(liberar).hasSize(2);
        assertThat(m.getEstado()).isEqualTo(EstadoMatricula.ANULADA);
        assertThat(m.cursosInscritos()).isEmpty();
    }

    @Test
    @DisplayName("Vence a los 15 minutos de la reserva, no antes")
    void vencimiento() {
        Matricula m = conDosCursos();
        m.confirmarSeleccion(ahora);
        assertThat(m.estaVencida(ahora.plusMinutes(14), 15)).isFalse();
        assertThat(m.estaVencida(ahora.plusMinutes(16), 15)).isTrue();
    }

    @Test
    @DisplayName("No se puede confirmar la selección sin cursos")
    void seleccionVacia() {
        Matricula m = new Matricula(estudiante(), PERIODO, ahora);
        assertThatThrownBy(() -> m.confirmarSeleccion(ahora)).isInstanceOf(ReglaNegocioException.class);
    }
}
