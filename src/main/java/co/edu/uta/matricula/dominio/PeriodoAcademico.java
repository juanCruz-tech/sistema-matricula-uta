package co.edu.uta.matricula.dominio;

import jakarta.persistence.*;

import java.time.LocalDate;

/** Periodo académico (por ejemplo 2027-1). Solo se matricula en un periodo abierto. */
@Entity
public class PeriodoAcademico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String codigo;

    private LocalDate fechaInicio;

    private LocalDate fechaFin;

    private boolean abiertoMatricula;

    protected PeriodoAcademico() {
    }

    public PeriodoAcademico(String codigo, LocalDate fechaInicio, LocalDate fechaFin, boolean abiertoMatricula) {
        this.codigo = codigo;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.abiertoMatricula = abiertoMatricula;
    }

    public boolean estaAbierto() {
        return abiertoMatricula;
    }

    public void cerrarMatricula() {
        this.abiertoMatricula = false;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }
}
