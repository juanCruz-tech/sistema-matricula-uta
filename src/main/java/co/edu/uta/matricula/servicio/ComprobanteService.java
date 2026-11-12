package co.edu.uta.matricula.servicio;

import co.edu.uta.matricula.dominio.Curso;
import co.edu.uta.matricula.dominio.EstadoMatricula;
import co.edu.uta.matricula.dominio.Matricula;
import co.edu.uta.matricula.dominio.ReglaNegocioException;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Genera el comprobante de matrícula en PDF con la librería reutilizable OpenPDF (RF-01). */
@Service
public class ComprobanteService {

    static final Color AZUL = new Color(0x12, 0x28, 0x4A);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generarPdf(Matricula m) {
        if (m.getEstado() != EstadoMatricula.CONFIRMADA || m.getComprobante() == null) {
            throw new ReglaNegocioException("El comprobante solo existe para matrículas confirmadas.");
        }
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.LETTER, 50, 50, 50, 50);
        PdfWriter.getInstance(doc, salida);
        doc.open();

        Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, AZUL);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10);
        Font negrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

        doc.add(new Paragraph("Universidad Tecnológica de los Andes", titulo));
        doc.add(new Paragraph("Comprobante de matrícula — Periodo " + m.getPeriodo().getCodigo(), negrita));
        doc.add(new Paragraph(" "));
        doc.add(new Paragraph("Número: " + m.getComprobante().getNumero(), normal));
        doc.add(new Paragraph("Fecha de emisión: " + m.getComprobante().getFechaEmision().format(FECHA), normal));
        doc.add(new Paragraph("Estudiante: " + m.getEstudiante().getNombre() + " (código " + m.getEstudiante().getCodigo() + ")", normal));
        doc.add(new Paragraph("Programa: " + m.getEstudiante().getPrograma().getNombre(), normal));
        doc.add(new Paragraph("Referencia de pago: " + m.getPago().getReferencia(), normal));
        doc.add(new Paragraph(" "));

        PdfPTable tabla = new PdfPTable(new float[]{4.5f, 1.2f, 1.2f, 4.5f});
        tabla.setWidthPercentage(100);
        for (String h : new String[]{"Asignatura", "Grupo", "Créditos", "Horario"}) {
            PdfPCell c = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE)));
            c.setBackgroundColor(AZUL);
            c.setPadding(5);
            tabla.addCell(c);
        }
        Font celda = FontFactory.getFont(FontFactory.HELVETICA, 9);
        for (Curso c : m.cursosInscritos()) {
            tabla.addCell(new Phrase(c.getAsignatura().getNombre(), celda));
            tabla.addCell(new Phrase(c.getGrupo(), celda));
            tabla.addCell(new Phrase(String.valueOf(c.getAsignatura().getCreditos()), celda));
            tabla.addCell(new Phrase(c.horarioTexto(), celda));
        }
        doc.add(tabla);
        doc.add(new Paragraph(" "));
        NumberFormat cop = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
        doc.add(new Paragraph("Total de créditos: " + m.calcularCreditos(), negrita));
        doc.add(new Paragraph("Valor pagado: " + cop.format(m.getPago().getValor()), negrita));
        doc.add(new Paragraph(" "));
        doc.add(new Paragraph("Código de verificación: " + m.getComprobante().getCodigoVerificacion(), normal));
        doc.add(new Paragraph("Documento generado por el prototipo académico del Sistema de Matrícula UTA. Datos de prueba ficticios.",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY)));
        doc.close();
        return salida.toByteArray();
    }
}
