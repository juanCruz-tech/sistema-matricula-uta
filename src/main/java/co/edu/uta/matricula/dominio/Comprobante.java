package co.edu.uta.matricula.dominio;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Comprobante de una matrícula confirmada (RF-01). Se compone dentro de Matricula.
 * La generación del PDF la hace ComprobanteService para que el dominio no dependa
 * de la librería de PDF (ajuste de diseño declarado en la Fase 3).
 */
@Entity
public class Comprobante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String numero;

    private LocalDateTime fechaEmision;

    @Column(length = 12)
    private String codigoVerificacion;

    protected Comprobante() {
    }

    public Comprobante(String numero, LocalDateTime fechaEmision, String codigoVerificacion) {
        this.numero = numero;
        this.fechaEmision = fechaEmision;
        this.codigoVerificacion = codigoVerificacion;
    }

    public Long getId() {
        return id;
    }

    public String getNumero() {
        return numero;
    }

    public LocalDateTime getFechaEmision() {
        return fechaEmision;
    }

    public String getCodigoVerificacion() {
        return codigoVerificacion;
    }
}
