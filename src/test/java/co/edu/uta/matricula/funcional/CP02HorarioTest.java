package co.edu.uta.matricula.funcional;

import co.edu.uta.matricula.dominio.Curso;
import co.edu.uta.matricula.dominio.DiaSemana;
import co.edu.uta.matricula.dominio.Matricula;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** CP-02 · RF-02: el horario es visible inmediatamente después de confirmar la matrícula. */
class CP02HorarioTest extends IntegracionBase {

    @Test
    @DisplayName("CP-02: el horario aparece al confirmar y muestra cada curso inscrito")
    void horarioVisibleTrasConfirmar() throws Exception {
        String est = nuevoEstudiante();
        Curso a = nuevoCurso(10, DiaSemana.LUNES, 14, 16);
        Curso b = nuevoCurso(10, DiaSemana.JUEVES, 16, 18);
        Matricula m = matriculaService.registrarMatricula(est, List.of(a.getId(), b.getId()));

        mvc.perform(get("/estudiante/horario").with(user(est).roles("ESTUDIANTE")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("El horario aparece cuando tu matrícula está confirmada")))
                .andExpect(content().string(not(containsString(a.getAsignatura().getNombre()))));

        pasarela.responder(m.getPago().getReferencia(), true);
        matriculaService.confirmarPago(est, m.getId());

        mvc.perform(get("/estudiante/horario").with(user(est).roles("ESTUDIANTE")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(a.getAsignatura().getNombre())))
                .andExpect(content().string(containsString(b.getAsignatura().getNombre())))
                .andExpect(content().string(containsString("2</b> bloques de clase")));
    }
}
