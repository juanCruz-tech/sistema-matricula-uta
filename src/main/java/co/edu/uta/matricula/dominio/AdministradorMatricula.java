package co.edu.uta.matricula.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** Funcionario de Registro Académico que gestiona cupos y reportes. Hereda de Usuario. */
@Entity
@DiscriminatorValue("ADMINISTRADOR")
public class AdministradorMatricula extends Usuario {

    @Column(length = 80)
    private String dependencia;

    @Column(length = 80)
    private String cargo;

    protected AdministradorMatricula() {
    }

    public AdministradorMatricula(String idUsuario, String nombre, String correo, String contrasenaHash,
                                  String dependencia, String cargo) {
        super(idUsuario, nombre, correo, contrasenaHash, Rol.ADMINISTRADOR);
        this.dependencia = dependencia;
        this.cargo = cargo;
    }

    public String getDependencia() {
        return dependencia;
    }

    public String getCargo() {
        return cargo;
    }
}
