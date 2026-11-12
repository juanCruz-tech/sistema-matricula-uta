package co.edu.uta.matricula.dominio;

import jakarta.persistence.*;

/** Estudiante que se matricula. Hereda de Usuario. */
@Entity
@DiscriminatorValue("ESTUDIANTE")
public class Estudiante extends Usuario {

    @Column(length = 20)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EstadoAcademico estadoAcademico;

    private int creditosAprobados;

    @ManyToOne(fetch = FetchType.LAZY)
    private ProgramaAcademico programa;

    protected Estudiante() {
    }

    public Estudiante(String idUsuario, String nombre, String correo, String contrasenaHash,
                      String codigo, ProgramaAcademico programa, EstadoAcademico estadoAcademico) {
        super(idUsuario, nombre, correo, contrasenaHash, Rol.ESTUDIANTE);
        this.codigo = codigo;
        this.programa = programa;
        this.estadoAcademico = estadoAcademico;
    }

    /** Solo un estudiante activo puede matricularse. */
    public boolean estaHabilitado() {
        return estadoAcademico == EstadoAcademico.ACTIVO;
    }

    public String getCodigo() {
        return codigo;
    }

    public EstadoAcademico getEstadoAcademico() {
        return estadoAcademico;
    }

    public int getCreditosAprobados() {
        return creditosAprobados;
    }

    public ProgramaAcademico getPrograma() {
        return programa;
    }
}
