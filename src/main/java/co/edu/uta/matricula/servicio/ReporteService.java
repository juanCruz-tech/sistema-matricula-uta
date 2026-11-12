package co.edu.uta.matricula.servicio;

import co.edu.uta.matricula.dominio.*;
import co.edu.uta.matricula.persistencia.IFacultadRepository;
import co.edu.uta.matricula.persistencia.IMatriculaRepository;
import co.edu.uta.matricula.persistencia.IProgramaRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reportes de estudiantes matriculados por facultad y programa (RF-05). Única responsabilidad: reportes (SRP). */
@Service
@Transactional(readOnly = true)
public class ReporteService {

    private final IMatriculaRepository matriculas;
    private final IFacultadRepository facultades;
    private final IProgramaRepository programas;
    private final OfertaService ofertaService;

    public ReporteService(IMatriculaRepository matriculas, IFacultadRepository facultades,
                          IProgramaRepository programas, OfertaService ofertaService) {
        this.matriculas = matriculas;
        this.facultades = facultades;
        this.programas = programas;
        this.ofertaService = ofertaService;
    }

    public Reporte generar(Long facultadId, Long programaId) {
        PeriodoAcademico periodo = ofertaService.periodoAbierto();
        List<Fila> filas = matriculas.buscarConfirmadasPorPeriodo(periodo).stream()
                .filter(m -> facultadId == null || m.getEstudiante().getPrograma().getFacultad().getId().equals(facultadId))
                .filter(m -> programaId == null || m.getEstudiante().getPrograma().getId().equals(programaId))
                .map(m -> new Fila(
                        m.getEstudiante().getPrograma().getFacultad().getNombre(),
                        m.getEstudiante().getPrograma().getNombre(),
                        m.getEstudiante().getCodigo(),
                        m.getEstudiante().getNombre(),
                        m.cursosInscritos().size(),
                        m.calcularCreditos(),
                        m.getComprobante() == null ? "" : m.getComprobante().getNumero()))
                .toList();
        Map<String, Long> porFacultad = new LinkedHashMap<>();
        filas.forEach(f -> porFacultad.merge(f.facultad(), 1L, Long::sum));
        return new Reporte(periodo.getCodigo(), filas, porFacultad);
    }

    public List<Facultad> facultades() {
        return facultades.findAll();
    }

    public List<ProgramaAcademico> programas() {
        List<ProgramaAcademico> lista = programas.findAll();
        lista.forEach(p -> p.getFacultad().getNombre());
        return lista;
    }

    public byte[] exportarPdf(Reporte r) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.LETTER.rotate(), 36, 36, 36, 36);
        PdfWriter.getInstance(doc, salida);
        doc.open();
        doc.add(new Paragraph("Reporte de estudiantes matriculados — Periodo " + r.periodo(),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, ComprobanteService.AZUL)));
        doc.add(new Paragraph("Total: " + r.filas().size() + " estudiantes con matrícula confirmada",
                FontFactory.getFont(FontFactory.HELVETICA, 10)));
        doc.add(new Paragraph(" "));
        PdfPTable t = new PdfPTable(new float[]{3, 3.5f, 1.6f, 3.5f, 1.2f, 1.2f, 2.6f});
        t.setWidthPercentage(100);
        for (String h : new String[]{"Facultad", "Programa", "Código", "Estudiante", "Cursos", "Créditos", "Comprobante"}) {
            PdfPCell c = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE)));
            c.setBackgroundColor(ComprobanteService.AZUL);
            c.setPadding(4);
            t.addCell(c);
        }
        Font f = FontFactory.getFont(FontFactory.HELVETICA, 8.5f);
        for (Fila x : r.filas()) {
            t.addCell(new Phrase(x.facultad(), f));
            t.addCell(new Phrase(x.programa(), f));
            t.addCell(new Phrase(x.codigo(), f));
            t.addCell(new Phrase(x.estudiante(), f));
            t.addCell(new Phrase(String.valueOf(x.cursos()), f));
            t.addCell(new Phrase(String.valueOf(x.creditos()), f));
            t.addCell(new Phrase(x.comprobante(), f));
        }
        doc.add(t);
        doc.close();
        return salida.toByteArray();
    }

    public record Fila(String facultad, String programa, String codigo, String estudiante,
                       int cursos, int creditos, String comprobante) {
    }

    public record Reporte(String periodo, List<Fila> filas, Map<String, Long> porFacultad) {
    }
}
