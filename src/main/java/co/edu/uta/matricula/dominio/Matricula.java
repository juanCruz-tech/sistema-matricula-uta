package co.edu.uta.matricula.dominio;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Clase central del sistema. Implementa la máquina de estados de la Figura 8 de la Fase 2:
 * BORRADOR → PENDIENTE_PAGO → CONFIRMADA, con paso a ANULADA si el pago se rechaza,
 * vence el tiempo de espera o el estudiante cancela.
 */
@Entity
public class Matricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Estudiante estudiante;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private PeriodoAcademico periodo;

    private LocalDateTime fechaRegistro;

    /** Momento en que se reservaron los cupos; desde aquí corre el tiempo de espera del pago. */
    private LocalDateTime fechaReserva;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private EstadoMatricula estado;

    private int totalCreditos;

    @Column(length = 200)
    private String motivoAnulacion;

    @OneToMany(mappedBy = "matricula", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleMatricula> detalles = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Pago pago;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Comprobante comprobante;

    protected Matricula() {
    }

    public Matricula(Estudiante estudiante, PeriodoAcademico periodo, LocalDateTime ahora) {
        this.estudiante = estudiante;
        this.periodo = periodo;
        this.fechaRegistro = ahora;
        this.estado = EstadoMatricula.BORRADOR;
    }

    /** Agrega un curso validando que no se repita la asignatura ni se cruce el horario. */
    public void agregarCurso(Curso curso, LocalDateTime ahora) {
        exigirEstado(EstadoMatricula.BORRADOR, "agregar cursos");
        for (Curso inscrito : cursosInscritos()) {
            if (inscrito.getAsignatura().getCodigo().equals(curso.getAsignatura().getCodigo())) {
                throw new ReglaNegocioException("Ya seleccionaste un grupo de " + curso.getAsignatura().getNombre() + ".");
            }
            if (inscrito.seCruzaCon(curso)) {
                throw new ReglaNegocioException("Cruce de horario: " + curso.nombreCompleto()
                        + " se cruza con " + inscrito.nombreCompleto() + ".");
            }
        }
        detalles.add(new DetalleMatricula(this, curso, ahora));
        totalCreditos = calcularCreditos();
    }

    public void retirarCurso(Curso curso) {
        exigirEstado(EstadoMatricula.BORRADOR, "retirar cursos");
        detalles.removeIf(d -> d.getCurso() == curso
                || (d.getCurso().getId() != null && d.getCurso().getId().equals(curso.getId())));
        totalCreditos = calcularCreditos();
    }

    /** BORRADOR → PENDIENTE_PAGO. Los cupos ya fueron reservados por CupoService. */
    public void confirmarSeleccion(LocalDateTime ahora) {
        exigirEstado(EstadoMatricula.BORRADOR, "confirmar la selección");
        if (detalles.isEmpty()) {
            throw new ReglaNegocioException("Debes seleccionar al menos un curso.");
        }
        this.estado = EstadoMatricula.PENDIENTE_PAGO;
        this.fechaReserva = ahora;
    }

    public void asociarPago(Pago pago) {
        exigirEstado(EstadoMatricula.PENDIENTE_PAGO, "asociar el pago");
        this.pago = pago;
    }

    /** PENDIENTE_PAGO → CONFIRMADA. Solo si la pasarela aprobó el pago. */
    public void confirmar(Comprobante comprobante) {
        exigirEstado(EstadoMatricula.PENDIENTE_PAGO, "confirmar la matrícula");
        if (pago == null || !pago.estaAprobado()) {
            throw new ReglaNegocioException("La matrícula no se puede confirmar sin un pago aprobado.");
        }
        this.comprobante = comprobante;
        this.estado = EstadoMatricula.CONFIRMADA;
    }

    /**
     * Pasa la matrícula a ANULADA y devuelve los cursos cuyos cupos deben liberarse.
     * Se permite desde BORRADOR, PENDIENTE_PAGO o CONFIRMADA (retiro).
     */
    public List<Curso> anular(String motivo) {
        if (estado == EstadoMatricula.ANULADA) {
            throw new ReglaNegocioException("La matrícula ya está anulada.");
        }
        boolean habiaReserva = estado != EstadoMatricula.BORRADOR;
        List<Curso> aLiberar = habiaReserva ? cursosInscritos() : List.of();
        detalles.forEach(DetalleMatricula::anular);
        this.estado = EstadoMatricula.ANULADA;
        this.motivoAnulacion = motivo;
        return aLiberar;
    }

    /** True si lleva más de {@code minutos} esperando el pago (transición after(15 min)). */
    public boolean estaVencida(LocalDateTime ahora, long minutos) {
        return estado == EstadoMatricula.PENDIENTE_PAGO
                && fechaReserva != null
                && fechaReserva.plusMinutes(minutos).isBefore(ahora);
    }

    public int calcularCreditos() {
        return detalles.stream().filter(DetalleMatricula::estaInscrito)
                .mapToInt(d -> d.getCurso().getAsignatura().getCreditos()).sum();
    }

    public BigDecimal calcularValor(BigDecimal valorCredito) {
        return valorCredito.multiply(BigDecimal.valueOf(calcularCreditos()));
    }

    public List<Curso> cursosInscritos() {
        return detalles.stream().filter(DetalleMatricula::estaInscrito).map(DetalleMatricula::getCurso).toList();
    }

    public boolean estaActiva() {
        return estado != EstadoMatricula.ANULADA;
    }

    private void exigirEstado(EstadoMatricula esperado, String accion) {
        if (estado != esperado) {
            throw new ReglaNegocioException("No se puede " + accion + " cuando la matrícula está en estado " + estado + ".");
        }
    }

    public Long getId() {
        return id;
    }

    public Estudiante getEstudiante() {
        return estudiante;
    }

    public PeriodoAcademico getPeriodo() {
        return periodo;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public LocalDateTime getFechaReserva() {
        return fechaReserva;
    }

    public EstadoMatricula getEstado() {
        return estado;
    }

    public int getTotalCreditos() {
        return totalCreditos;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public List<DetalleMatricula> getDetalles() {
        return Collections.unmodifiableList(detalles);
    }

    public Pago getPago() {
        return pago;
    }

    public Comprobante getComprobante() {
        return comprobante;
    }
}
