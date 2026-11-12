package co.edu.uta.matricula.funcional;

import co.edu.uta.matricula.dominio.Facultad;
import co.edu.uta.matricula.persistencia.IFacultadRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** CP-05 · RF-05 (Should): reporte por facultad y programa exportable a PDF en menos de 10 s. */
class CP05ReporteTest extends IntegracionBase {

    @Autowired
    IFacultadRepository facultades;

    @Test
    @DisplayName("CP-05: el reporte filtra por facultad y se exporta a PDF en menos de 10 s")
    void reporteFiltradoYPdf() throws Exception {
        Facultad salud = facultades.findAll().stream().filter(f -> f.getCodigo().equals("SAL")).findFirst().orElseThrow();
        mvc.perform(get("/admin/reportes").param("facultadId", salud.getId().toString())
                        .with(user("admin.registro").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Valentina Ortiz Mora")))
                .andExpect(content().string(not(containsString("Camila Torres Vega"))));

        long inicio = System.nanoTime();
        byte[] pdf = mvc.perform(get("/admin/reportes.pdf").with(user("admin.registro").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();
        long ms = (System.nanoTime() - inicio) / 1_000_000;
        System.out.printf("[CP-05] reporte PDF generado en %d ms (%d bytes)%n", ms, pdf.length);
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        assertThat(ms).isLessThan(10_000);
    }
}
