package co.edu.uta.matricula.dominio;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Un curso inscrito dentro de una matrícula. Existe solo como parte de su Matricula (composición). */
@Entity
public class DetalleMatricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Matricula matricula;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private Curso curso;

    @Enumerated(EnumType.STRING)
    @Column(length = 12)
    private EstadoDetalle estado;

    private LocalDateTime fechaInscripcion;

    protected DetalleMatricula() {
    }

    DetalleMatricula(Matricula matricula, Curso curso, LocalDateTime fechaInscripcion) {
        this.matricula = matricula;
        this.curso = curso;
        this.fechaInscripcion = fechaInscripcion;
        this.estado = EstadoDetalle.INSCRITO;
    }

    public void anular() {
        this.estado = EstadoDetalle.ANULADO;
    }

    public boolean estaInscrito() {
        return estado == EstadoDetalle.INSCRITO;
    }

    public Long getId() {
        return id;
    }

    public Matricula getMatricula() {
        return matricula;
    }

    public Curso getCurso() {
        return curso;
    }

    public EstadoDetalle getEstado() {
        return estado;
    }

    public LocalDateTime getFechaInscripcion() {
        return fechaInscripcion;
    }
}
