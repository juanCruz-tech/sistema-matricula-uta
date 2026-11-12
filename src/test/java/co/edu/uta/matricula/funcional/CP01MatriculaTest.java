package co.edu.uta.matricula.funcional;

import co.edu.uta.matricula.dominio.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** CP-01 · RF-01: seleccionar y confirmar materias, con comprobante, en menos de 5 s. */
class CP01MatriculaTest extends IntegracionBase {

    @Test
    @DisplayName("CP-01: matrícula completa confirmada en menos de 5 s y con comprobante PDF")
    void matriculaCompletaConComprobante() throws Exception {
        String est = nuevoEstudiante();
        Curso a = nuevoCurso(30, DiaSemana.LUNES, 7, 9);
        Curso b = nuevoCurso(30, DiaSemana.MARTES, 7, 9);
        long inicio = System.nanoTime();

        mvc.perform(post("/estudiante/matricula").with(user(est).roles("ESTUDIANTE")).with(csrf())
                        .param("cursoIds", a.getId().toString(), b.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/estudiante/matricula"));
        Matricula m = matriculaService.matriculaActual(est).orElseThrow();
        assertThat(m.getEstado()).isEqualTo(EstadoMatricula.PENDIENTE_PAGO);

        pasarela.responder(m.getPago().getReferencia(), true);
        mvc.perform(get("/estudiante/matricula/" + m.getId() + "/retorno-pago").with(user(est).roles("ESTUDIANTE")))
                .andExpect(status().is3xxRedirection());
        long ms = (System.nanoTime() - inicio) / 1_000_000;

        Matricula confirmada = matriculaService.matriculaActual(est).orElseThrow();
        assertThat(confirmada.getEstado()).isEqualTo(EstadoMatricula.CONFIRMADA);
        assertThat(confirmada.getComprobante().getNumero()).startsWith("UTA-2027-1-");
        assertThat(ms).as("tiempo total de la transacción en ms").isLessThan(5000);

        MockHttpServletResponse pdf = mvc.perform(get("/estudiante/matricula/" + m.getId() + "/comprobante.pdf")
                        .with(user(est).roles("ESTUDIANTE")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andReturn().getResponse();
        assertThat(new String(pdf.getContentAsByteArray(), 0, 5)).isEqualTo("%PDF-");
        System.out.printf("[CP-01] matrícula confirmada en %d ms, comprobante %s, PDF de %d bytes%n",
                ms, confirmada.getComprobante().getNumero(), pdf.getContentAsByteArray().length);
    }

    @Test
    @DisplayName("CP-01: un cruce de horario impide la matrícula y no reserva ningún cupo")
    void cruceDeHorario() throws Exception {
        String est = nuevoEstudiante();
        Curso a = nuevoCurso(10, DiaSemana.MIERCOLES, 9, 11);
        Curso b = nuevoCurso(10, DiaSemana.MIERCOLES, 10, 12);
        mvc.perform(post("/estudiante/matricula").with(user(est).roles("ESTUDIANTE")).with(csrf())
                        .param("cursoIds", a.getId().toString(), b.getId().toString()))
                .andExpect(redirectedUrl("/estudiante/oferta"))
                .andExpect(flash().attribute("error", org.hamcrest.Matchers.containsString("Cruce de horario")));
        assertThat(cuposDe(a)).isEqualTo(10);
        assertThat(cuposDe(b)).isEqualTo(10);
    }

    @Test
    @DisplayName("CP-01: un estudiante inactivo no puede matricularse")
    void estudianteInactivo() throws Exception {
        String est = nuevoEstudiante(EstadoAcademico.INACTIVO);
        Curso a = nuevoCurso(10, DiaSemana.JUEVES, 7, 9);
        mvc.perform(post("/estudiante/matricula").with(user(est).roles("ESTUDIANTE")).with(csrf())
                        .param("cursoIds", a.getId().toString()))
                .andExpect(flash().attribute("error", org.hamcrest.Matchers.containsString("estado académico")));
        assertThat(cuposDe(a)).isEqualTo(10);
    }

    @Test
    @DisplayName("CP-01: no se permite una segunda matrícula activa en el mismo periodo")
    void matriculaDuplicada() throws Exception {
        String est = nuevoEstudiante();
        Curso a = nuevoCurso(10, DiaSemana.VIERNES, 7, 9);
        Curso b = nuevoCurso(10, DiaSemana.VIERNES, 14, 16);
        matriculaService.registrarMatricula(est, java.util.List.of(a.getId()));
        mvc.perform(post("/estudiante/matricula").with(user(est).roles("ESTUDIANTE")).with(csrf())
                        .param("cursoIds", b.getId().toString()))
                .andExpect(flash().attribute("error", org.hamcrest.Matchers.containsString("Ya tienes una matrícula activa")));
        assertThat(cuposDe(b)).isEqualTo(10);
    }
}
