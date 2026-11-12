package co.edu.uta.matricula.integracion.pagos;

import co.edu.uta.matricula.dominio.EstadoPago;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pasarela de pagos simulada. Sustituye a la pasarela real de la universidad en el prototipo
 * y en las pruebas de carga sin cambiar el contrato IPasarelaPagos (principio LSP).
 * El "pagador" decide en la pantalla de la pasarela si el pago se aprueba o se rechaza.
 * Con aprobacion-automatica=true (solo pruebas de carga) cada pago se aprueba al crearse.
 */
@Component
public class PasarelaSimulada implements IPasarelaPagos {

    private final Map<String, Orden> ordenes = new ConcurrentHashMap<>();
    private final boolean aprobacionAutomatica;

    public PasarelaSimulada(@Value("${pasarela.simulada.aprobacion-automatica:false}") boolean aprobacionAutomatica) {
        this.aprobacionAutomatica = aprobacionAutomatica;
    }

    @Override
    public String solicitarPago(String descripcion, BigDecimal valor) {
        String referencia = "PSE-SIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        EstadoPago inicial = aprobacionAutomatica ? EstadoPago.APROBADO : EstadoPago.PENDIENTE;
        ordenes.put(referencia, new Orden(descripcion, valor, inicial));
        return referencia;
    }

    @Override
    public EstadoPago verificarPago(String referencia) {
        Orden orden = ordenes.get(referencia);
        return orden == null ? EstadoPago.RECHAZADO : orden.estado();
    }

    /** Lo que hace el usuario en la pantalla de la pasarela: aprobar o rechazar. */
    public void responder(String referencia, boolean aprobado) {
        ordenes.computeIfPresent(referencia, (ref, o) ->
                new Orden(o.descripcion(), o.valor(), aprobado ? EstadoPago.APROBADO : EstadoPago.RECHAZADO));
    }

    public Orden consultarOrden(String referencia) {
        return ordenes.get(referencia);
    }

    public record Orden(String descripcion, BigDecimal valor, EstadoPago estado) {
    }
}
