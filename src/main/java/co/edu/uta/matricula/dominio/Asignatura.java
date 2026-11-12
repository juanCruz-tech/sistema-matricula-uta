package co.edu.uta.matricula.dominio;

import jakarta.persistence.*;

/** Asignatura del plan de estudios; un Curso es un grupo de una asignatura. */
@Entity
public class Asignatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 12)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    private int creditos;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private ProgramaAcademico programa;

    protected Asignatura() {
    }

    public Asignatura(String codigo, String nombre, int creditos, ProgramaAcademico programa) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.creditos = creditos;
        this.programa = programa;
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

    public int getCreditos() {
        return creditos;
    }

    public ProgramaAcademico getPrograma() {
        return programa;
    }
}
