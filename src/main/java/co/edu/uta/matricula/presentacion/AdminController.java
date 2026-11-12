package co.edu.uta.matricula.presentacion;

import co.edu.uta.matricula.dominio.Curso;
import co.edu.uta.matricula.dominio.PeriodoAcademico;
import co.edu.uta.matricula.dominio.ReglaNegocioException;
import co.edu.uta.matricula.servicio.CupoService;
import co.edu.uta.matricula.servicio.OfertaService;
import co.edu.uta.matricula.servicio.ReporteService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/** Controlador MVC del administrador de matrícula: cupos y reportes (RF-03, RF-05). */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final OfertaService ofertaService;
    private final CupoService cupoService;
    private final ReporteService reporteService;

    public AdminController(OfertaService ofertaService, CupoService cupoService, ReporteService reporteService) {
        this.ofertaService = ofertaService;
        this.cupoService = cupoService;
        this.reporteService = reporteService;
    }

    @GetMapping("/cursos")
    public String cursos(Model model) {
        PeriodoAcademico periodo = ofertaService.periodoAbierto();
        List<Curso> cursos = ofertaService.cursosDelPeriodo(periodo);
        model.addAttribute("periodo", periodo);
        model.addAttribute("cursos", cursos);
        model.addAttribute("totalCupos", cursos.stream().mapToInt(Curso::getCupoMaximo).sum());
        model.addAttribute("ocupados", cursos.stream().mapToInt(c -> c.getCupoMaximo() - c.getCuposDisponibles()).sum());
        return "admin/cursos";
    }

    @PostMapping("/cursos/{id}/cupo")
    public String ajustarCupo(@PathVariable Long id, @RequestParam int cupoMaximo, RedirectAttributes flash) {
        try {
            Curso c = cupoService.gestionarCupo(id, cupoMaximo);
            flash.addFlashAttribute("ok", "Cupo de " + c.nombreCompleto() + " actualizado a " + c.getCupoMaximo() + ".");
        } catch (ReglaNegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/cursos";
    }

    @GetMapping("/reportes")
    public String reportes(@RequestParam(required = false) Long facultadId,
                           @RequestParam(required = false) Long programaId, Model model) {
        long inicio = System.nanoTime();
        ReporteService.Reporte reporte = reporteService.generar(facultadId, programaId);
        model.addAttribute("ms", (System.nanoTime() - inicio) / 1_000_000);
        model.addAttribute("reporte", reporte);
        model.addAttribute("facultades", reporteService.facultades());
        model.addAttribute("programas", reporteService.programas());
        model.addAttribute("facultadId", facultadId);
        model.addAttribute("programaId", programaId);
        return "admin/reportes";
    }

    @GetMapping("/reportes.pdf")
    public ResponseEntity<byte[]> reportePdf(@RequestParam(required = false) Long facultadId,
                                             @RequestParam(required = false) Long programaId) {
        ReporteService.Reporte reporte = reporteService.generar(facultadId, programaId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"reporte-matricula-" + reporte.periodo() + ".pdf\"")
                .body(reporteService.exportarPdf(reporte));
    }
}
