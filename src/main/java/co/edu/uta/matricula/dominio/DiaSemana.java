package co.edu.uta.matricula.dominio;

/** Días en los que puede dictarse una franja horaria. */
public enum DiaSemana {
    LUNES("Lunes"), MARTES("Martes"), MIERCOLES("Miércoles"), JUEVES("Jueves"), VIERNES("Viernes"), SABADO("Sábado");

    private final String etiqueta;

    DiaSemana(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
