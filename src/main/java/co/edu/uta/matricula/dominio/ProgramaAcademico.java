package co.edu.uta.matricula.dominio;

import jakarta.persistence.*;

/** Programa académico (carrera). Pertenece a una facultad y ofrece asignaturas. */
@Entity
public class ProgramaAcademico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String codigo;

    @Column(nullable = false, length = 80)
    private String nombre;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Facultad facultad;

    protected ProgramaAcademico() {
    }

    public ProgramaAcademico(String codigo, String nombre, Facultad facultad) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.facultad = facultad;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public Facultad getFacultad() {
        return facultad;
    }
}
