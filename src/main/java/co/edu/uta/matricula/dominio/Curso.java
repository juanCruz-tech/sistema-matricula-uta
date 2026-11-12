package co.edu.uta.matricula.dominio;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Grupo de una asignatura en un periodo, con su cupo y sus franjas horarias.
 * Invariante: 0 ≤ cuposDisponibles ≤ cupoMaximo.
 */
@Entity
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 5)
    private String grupo;

    private int cupoMaximo;

    private int cuposDisponibles;

    @Column(length = 80)
    private String docente;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private Asignatura asignatura;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private PeriodoAcademico periodo;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "curso_franja", joinColumns = @JoinColumn(name = "curso_id"))
    private List<FranjaHoraria> franjas = new ArrayList<>();

    protected Curso() {
    }

    public Curso(Asignatura asignatura, PeriodoAcademico periodo, String grupo, int cupoMaximo, String docente,
                 List<FranjaHoraria> franjas) {
        if (cupoMaximo < 1) {
            throw new IllegalArgumentException("El cupo máximo debe ser al menos 1");
        }
        if (franjas == null || franjas.isEmpty()) {
            throw new IllegalArgumentException("Un curso necesita al menos una franja horaria");
        }
        this.asignatura = asignatura;
        this.periodo = periodo;
        this.grupo = grupo;
        this.cupoMaximo = cupoMaximo;
        this.cuposDisponibles = cupoMaximo;
        this.docente = docente;
        this.franjas = new ArrayList<>(franjas);
    }

    public boolean tieneCupo() {
        return cuposDisponibles > 0;
    }

    /**
     * Reserva un cupo en memoria. En producción la reserva concurrente se hace con una
     * operación atómica en la base de datos (ICursoRepository.decrementarCupo).
     */
    public boolean reservarCupo() {
        if (!tieneCupo()) {
            return false;
        }
        cuposDisponibles--;
        return true;
    }

    public void liberarCupo() {
        if (cuposDisponibles < cupoMaximo) {
            cuposDisponibles++;
        }
    }

    /** Ajuste del cupo por parte del administrador, respetando los cupos ya ocupados. */
    public void ajustarCupoMaximo(int nuevoMaximo) {
        int ocupados = cupoMaximo - cuposDisponibles;
        if (nuevoMaximo < ocupados) {
            throw new IllegalArgumentException(
                    "El nuevo cupo (" + nuevoMaximo + ") es menor que los cupos ya ocupados (" + ocupados + ")");
        }
        this.cupoMaximo = nuevoMaximo;
        this.cuposDisponibles = nuevoMaximo - ocupados;
    }

    /** Un curso se cruza con otro si alguna de sus franjas se cruza con alguna del otro. */
    public boolean seCruzaCon(Curso otro) {
        for (FranjaHoraria f : franjas) {
            for (FranjaHoraria g : otro.franjas) {
                if (f.seCruzaCon(g)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String nombreCompleto() {
        return asignatura.getNombre() + " — Grupo " + grupo;
    }

    public String horarioTexto() {
        return String.join(" · ", franjas.stream().map(FranjaHoraria::descripcion).toList());
    }

    public Long getId() {
        return id;
    }

    public String getGrupo() {
        return grupo;
    }

    public int getCupoMaximo() {
        return cupoMaximo;
    }

    public int getCuposDisponibles() {
        return cuposDisponibles;
    }

    public String getDocente() {
        return docente;
    }

    public Asignatura getAsignatura() {
        return asignatura;
    }

    public PeriodoAcademico getPeriodo() {
        return periodo;
    }

    public List<FranjaHoraria> getFranjas() {
        return Collections.unmodifiableList(franjas);
    }
}
