package co.edu.uta.matricula.integracion.pagos;

import co.edu.uta.matricula.dominio.EstadoPago;

import java.math.BigDecimal;

/**
 * Contrato con la pasarela de pagos (Figura 5 de la Fase 2). MatriculaService depende de esta
 * interfaz y no de una pasarela concreta (principio DIP). Integrar la pasarela real de la
 * universidad consiste en crear otro adaptador que la implemente (principio OCP).
 */
public interface IPasarelaPagos {

    /** Registra una orden de pago y devuelve su referencia. */
    String solicitarPago(String descripcion, BigDecimal valor);

    /** Consulta el estado actual de un pago. */
    EstadoPago verificarPago(String referencia);
}
