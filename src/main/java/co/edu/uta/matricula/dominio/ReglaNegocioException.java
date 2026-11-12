package co.edu.uta.matricula.dominio;

/**
 * Se lanza cuando una operación incumple una regla del proceso de matrícula
 * (sin cupo, cruce de horario, periodo cerrado, transición de estado inválida...).
 * El mensaje está pensado para mostrarse al usuario.
 */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
