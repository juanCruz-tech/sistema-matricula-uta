package co.edu.uta.matricula.presentacion;

import co.edu.uta.matricula.dominio.DiaSemana;
import co.edu.uta.matricula.dominio.EstadoMatricula;
import co.edu.uta.matricula.dominio.Matricula;
import co.edu.uta.matricula.dominio.ReglaNegocioException;
import co.edu.uta.matricula.servicio.ComprobanteService;
import co.edu.uta.matricula.servicio.HorarioService;
import co.edu.uta.matricula.servicio.MatriculaService;
import co.edu.uta.matricula.servicio.OfertaService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

/**
 * Controlador MVC del estudiante: recibe las acciones de la Vista y delega en los servicios (Modelo).
 * No contiene reglas de negocio ni accede a la base de datos.
 */
@Controller
@RequestMapping("/estudiante")
public class EstudianteController {

    private final OfertaService ofertaService;
    private final MatriculaService matriculaService;
    private final HorarioService horarioService;
    private final ComprobanteService comprobanteService;

    public EstudianteController(OfertaService ofertaService, MatriculaService matriculaService,
                                HorarioService horarioService, ComprobanteService comprobanteService) {
        this.ofertaService = ofertaService;
        this.matriculaService = matriculaService;
        this.horarioService = horarioService;
        this.comprobanteService = comprobanteService;
    }

    @GetMapping("/oferta")
    public String oferta(Principal p, Model model) {
        Optional<Matricula> actual = matriculaService.matriculaActual(p.getName());
        model.addAttribute("estudiante", ofertaService.descripcionEstudiante(p.getName()));
        model.addAttribute("periodo", ofertaService.periodoAbierto());
        model.addAttribute("cursos", ofertaService.cursosParaEstudiante(p.getName()));
        model.addAttribute("matriculaActiva", actual.filter(Matricula::estaActiva).orElse(null));
        model.addAttribute("valorCredito", matriculaService.getValorCredito());
        if (!model.containsAttribute("seleccion")) {
            model.addAttribute("seleccion", List.of());
        }
        return "estudiante/oferta";
    }

    @PostMapping("/matricula")
    public String registrar(Principal p, @RequestParam(name = "cursoIds", required = false) List<Long> cursoIds,
                            RedirectAttributes flash) {
        long inicio = System.nanoTime();
        try {
            Matricula m = matriculaService.registrarMatricula(p.getName(), cursoIds);
            long ms = (System.nanoTime() - inicio) / 1_000_000;
            flash.addFlashAttribute("ok", "Cupos reservados para " + m.cursosInscritos().size()
                    + " cursos (" + ms + " ms). Tienes " + matriculaService.getMinutosEsperaPago()
                    + " minutos para completar el pago.");
            return "redirect:/estudiante/matricula";
        } catch (ReglaNegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
            flash.addFlashAttribute("seleccion", cursoIds == null ? List.of() : cursoIds);
            return "redirect:/estudiante/oferta";
        }
    }

    @GetMapping("/matricula")
    public String matricula(Principal p, Model model) {
        model.addAttribute("estudiante", ofertaService.descripcionEstudiante(p.getName()));
        model.addAttribute("matricula", matriculaService.matriculaActual(p.getName()).orElse(null));
        model.addAttribute("minutosEspera", matriculaService.getMinutosEsperaPago());
        return "estudiante/matricula";
    }

    @PostMapping("/matricula/{id}/pagar")
    public String pagar(Principal p, @PathVariable Long id) {
        Matricula m = matriculaService.consultar(p.getName(), id);
        if (m.getEstado() != EstadoMatricula.PENDIENTE_PAGO) {
            return "redirect:/estudiante/matricula";
        }
        String retorno = UriUtils.encode("/estudiante/matricula/" + id + "/retorno-pago", StandardCharsets.UTF_8);
        return "redirect:/pasarela-simulada/" + m.getPago().getReferencia() + "?retorno=" + retorno;
    }

    @GetMapping("/matricula/{id}/retorno-pago")
    public String retornoPago(Principal p, @PathVariable Long id, RedirectAttributes flash) {
        long inicio = System.nanoTime();
        Matricula m = matriculaService.confirmarPago(p.getName(), id);
        long ms = (System.nanoTime() - inicio) / 1_000_000;
        switch (m.getEstado()) {
            case CONFIRMADA -> flash.addFlashAttribute("ok", "¡Matrícula confirmada! Comprobante "
                    + m.getComprobante().getNumero() + " generado en " + ms + " ms.");
            case ANULADA -> flash.addFlashAttribute("error",
                    "El pago fue rechazado. La matrícula quedó anulada y los cupos se liberaron.");
            default -> flash.addFlashAttribute("aviso", "El pago aún no ha sido confirmado por la pasarela.");
        }
        return "redirect:/estudiante/matricula";
    }

    @PostMapping("/matricula/{id}/cancelar")
    public String cancelar(Principal p, @PathVariable Long id, RedirectAttributes flash) {
        try {
            matriculaService.cancelar(p.getName(), id);
            flash.addFlashAttribute("aviso", "Cancelaste la matrícula y los cupos se liberaron.");
        } catch (ReglaNegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/estudiante/matricula";
    }

    @GetMapping("/matricula/{id}/comprobante.pdf")
    public ResponseEntity<byte[]> comprobante(Principal p, @PathVariable Long id) {
        Matricula m = matriculaService.consultar(p.getName(), id);
        byte[] pdf = comprobanteService.generarPdf(m);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"comprobante-" + m.getComprobante().getNumero() + ".pdf\"")
                .body(pdf);
    }

    @GetMapping("/horario")
    public String horario(Principal p, Model model) {
        model.addAttribute("estudiante", ofertaService.descripcionEstudiante(p.getName()));
        model.addAttribute("horario", horarioService.horarioDe(p.getName()));
        model.addAttribute("dias", DiaSemana.values());
        return "estudiante/horario";
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public String reglaIncumplida(ReglaNegocioException e, RedirectAttributes flash) {
        flash.addFlashAttribute("error", e.getMessage());
        return "redirect:/estudiante/matricula";
    }
}
