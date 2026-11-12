package co.edu.uta.matricula.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Revisa periódicamente las matrículas pendientes de pago y anula las vencidas,
 * liberando sus cupos (transición after(15 min) de la Figura 8).
 */
@Component
public class ExpiracionPagosTarea {

    private static final Logger log = LoggerFactory.getLogger(ExpiracionPagosTarea.class);
    private final MatriculaService matriculaService;

    public ExpiracionPagosTarea(MatriculaService matriculaService) {
        this.matriculaService = matriculaService;
    }

    @Scheduled(fixedDelayString = "${matricula.revision-vencidas-ms:60000}", initialDelay = 30000)
    public void revisar() {
        int anuladas = matriculaService.anularVencidas();
        if (anuladas > 0) {
            log.info("Se anularon {} matrículas por tiempo de pago vencido", anuladas);
        }
    }
}
