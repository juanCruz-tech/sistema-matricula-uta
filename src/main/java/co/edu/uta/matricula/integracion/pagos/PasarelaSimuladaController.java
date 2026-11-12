package co.edu.uta.matricula.integracion.pagos;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Pantalla de la pasarela simulada. Representa el sitio externo del banco: el usuario decide
 * si el pago se aprueba o se rechaza y vuelve al sistema de matrícula.
 */
@Controller
@RequestMapping("/pasarela-simulada")
public class PasarelaSimuladaController {

    private final PasarelaSimulada pasarela;

    public PasarelaSimuladaController(PasarelaSimulada pasarela) {
        this.pasarela = pasarela;
    }

    @GetMapping("/{referencia}")
    public String pantalla(@PathVariable String referencia, @RequestParam String retorno, Model model) {
        model.addAttribute("referencia", referencia);
        model.addAttribute("orden", pasarela.consultarOrden(referencia));
        model.addAttribute("retorno", retornoSeguro(retorno));
        return "pasarela/simulada";
    }

    @PostMapping("/{referencia}")
    public String responder(@PathVariable String referencia, @RequestParam boolean aprobado,
                            @RequestParam String retorno) {
        pasarela.responder(referencia, aprobado);
        return "redirect:" + retornoSeguro(retorno);
    }

    /** Evita redirecciones abiertas: solo se permite volver a una ruta interna del estudiante. */
    private static String retornoSeguro(String retorno) {
        return retorno != null && retorno.startsWith("/estudiante/") && !retorno.contains("//")
                ? retorno : "/estudiante/matricula";
    }
}
