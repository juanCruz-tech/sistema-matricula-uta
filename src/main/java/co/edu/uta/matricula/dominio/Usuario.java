package co.edu.uta.matricula.dominio;

import jakarta.persistence.*;

/**
 * Clase abstracta con los datos comunes de quien entra al sistema.
 * Estudiante y AdministradorMatricula heredan de ella (Figura 4 de la Fase 2).
 * La contraseña nunca se guarda en texto plano: solo su hash BCrypt (RNF-04).
 */
@Entity
@Table(name = "usuario")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo")
public abstract class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre de usuario con el que se inicia sesión. */
    @Column(nullable = false, unique = true, length = 40)
    private String idUsuario;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 120)
    private String correo;

    @Column(nullable = false, length = 100)
    private String contrasenaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    protected Usuario() {
    }

    protected Usuario(String idUsuario, String nombre, String correo, String contrasenaHash, Rol rol) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.correo = correo;
        this.contrasenaHash = contrasenaHash;
        this.rol = rol;
    }

    public Rol obtenerRol() {
        return rol;
    }

    public void cambiarContrasena(String nuevoHash) {
        this.contrasenaHash = nuevoHash;
    }

    public Long getId() {
        return id;
    }

    public String getIdUsuario() {
        return idUsuario;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public String getContrasenaHash() {
        return contrasenaHash;
    }
}
