package co.edu.uta.matricula.funcional;

import co.edu.uta.matricula.dominio.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;

/** CP-04 · RF-04: la matrícula solo se confirma con un pago aprobado por la pasarela. */
class CP04PagoTest extends IntegracionBase {

    @AfterEach
    void reiniciarReloj() {
        reloj.reiniciar();
    }

    @Test
    @DisplayName("CP-04: pago rechazado → matrícula anulada y cupos liberados")
    void pagoRechazado() throws Exception {
        String est = nuevoEstudiante();
        Curso a = nuevoCurso(5, DiaSemana.LUNES, 16, 18);
        Matricula m = matriculaService.registrarMatricula(est, List.of(a.getId()));
        assertThat(cuposDe(a)).isEqualTo(4);

        pasarela.responder(m.getPago().getReferencia(), false);
        mvc.perform(get("/estudiante/matricula/" + m.getId() + "/retorno-pago").with(user(est).roles("ESTUDIANTE")))
                .andExpect(flash().attribute("error", org.hamcrest.Matchers.containsString("rechazado")));

        Matricula anulada = matriculaService.matriculaActual(est).orElseThrow();
        assertThat(anulada.getEstado()).isEqualTo(EstadoMatricula.ANULADA);
        assertThat(anulada.getPago().getEstado()).isEqualTo(EstadoPago.RECHAZADO);
        assertThat(anulada.getComprobante()).isNull();
        assertThat(cuposDe(a)).isEqualTo(5);
    }

    @Test
    @DisplayName("CP-04: sin respuesta de la pasarela la matrícula sigue pendiente y no se confirma")
    void pagoPendiente() {
        String est = nuevoEstudiante();
        Curso a = nuevoCurso(5, DiaSemana.MARTES, 16, 18);
        Matricula m = matriculaService.registrarMatricula(est, List.of(a.getId()));
        Matricula despues = matriculaService.confirmarPago(est, m.getId());
        assertThat(despues.getEstado()).isEqualTo(EstadoMatricula.PENDIENTE_PAGO);
        assertThat(despues.getComprobante()).isNull();
    }

    @Test
    @DisplayName("CP-04: si el pago no llega en 15 minutos la matrícula se anula y libera el cupo")
    void pagoVencido() {
        String est = nuevoEstudiante();
        Curso a = nuevoCurso(1, DiaSemana.MIERCOLES, 18, 20);
        matriculaService.registrarMatricula(est, List.of(a.getId()));
        assertThat(cuposDe(a)).isZero();

        reloj.adelantar(Duration.ofMinutes(14));
        matriculaService.anularVencidas();
        assertThat(matriculaService.matriculaActual(est).orElseThrow().getEstado()).isEqualTo(EstadoMatricula.PENDIENTE_PAGO);

        reloj.adelantar(Duration.ofMinutes(2));
        int anuladas = matriculaService.anularVencidas();
        assertThat(anuladas).isGreaterThanOrEqualTo(1);
        assertThat(matriculaService.matriculaActual(est).orElseThrow().getEstado()).isEqualTo(EstadoMatricula.ANULADA);
        assertThat(cuposDe(a)).isEqualTo(1);
    }
}
