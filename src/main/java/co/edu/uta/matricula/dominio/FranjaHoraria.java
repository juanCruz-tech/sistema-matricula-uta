package co.edu.uta.matricula.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalTime;
import java.util.Objects;

/**
 * Un bloque de clase: día, hora de inicio, hora de fin y aula.
 * Un Curso se compone de una o más franjas (composición en la Figura 4).
 */
@Embeddable
public class FranjaHoraria {

    @Enumerated(EnumType.STRING)
    @Column(length = 12)
    private DiaSemana dia;

    private LocalTime horaInicio;

    private LocalTime horaFin;

    @Column(length = 20)
    private String aula;

    protected FranjaHoraria() {
    }

    public FranjaHoraria(DiaSemana dia, LocalTime horaInicio, LocalTime horaFin, String aula) {
        if (!horaInicio.isBefore(horaFin)) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la hora de fin");
        }
        this.dia = dia;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.aula = aula;
    }

    /**
     * Dos franjas se cruzan si son el mismo día y sus intervalos se solapan.
     * Una clase que termina a las 9:00 no se cruza con otra que empieza a las 9:00.
     */
    public boolean seCruzaCon(FranjaHoraria otra) {
        return dia == otra.dia
                && horaInicio.isBefore(otra.horaFin)
                && otra.horaInicio.isBefore(horaFin);
    }

    public DiaSemana getDia() {
        return dia;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public String getAula() {
        return aula;
    }

    public String descripcion() {
        return dia.getEtiqueta() + " " + horaInicio + "–" + horaFin + " (" + aula + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FranjaHoraria f)) return false;
        return dia == f.dia && Objects.equals(horaInicio, f.horaInicio)
                && Objects.equals(horaFin, f.horaFin) && Objects.equals(aula, f.aula);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dia, horaInicio, horaFin, aula);
    }
}
