package co.edu.uta.matricula.dominio;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Pago asociado a una matrícula. Su estado lo informa la pasarela de pagos (RF-04). */
@Entity
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String referencia;

    @Column(precision = 14, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(length = 12)
    private EstadoPago estado;

    private LocalDateTime fechaValidacion;

    protected Pago() {
    }

    public Pago(String referencia, BigDecimal valor) {
        this.referencia = referencia;
        this.valor = valor;
        this.estado = EstadoPago.PENDIENTE;
    }

    public void registrarRespuesta(EstadoPago nuevoEstado, LocalDateTime fecha) {
        this.estado = nuevoEstado;
        this.fechaValidacion = fecha;
    }

    public boolean estaAprobado() {
        return estado == EstadoPago.APROBADO;
    }

    public Long getId() {
        return id;
    }

    public String getReferencia() {
        return referencia;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public EstadoPago getEstado() {
        return estado;
    }

    public LocalDateTime getFechaValidacion() {
        return fechaValidacion;
    }
}
