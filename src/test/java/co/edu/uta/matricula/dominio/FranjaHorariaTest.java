package co.edu.uta.matricula.dominio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas unitarias de FranjaHoraria.seCruzaCon() (RF-02, detección de cruces). */
class FranjaHorariaTest {

    private static FranjaHoraria f(DiaSemana d, int desde, int hasta) {
        return new FranjaHoraria(d, LocalTime.of(desde, 0), LocalTime.of(hasta, 0), "A-101");
    }

    @Test
    @DisplayName("Dos franjas del mismo día que se solapan se cruzan")
    void solapadasSeCruzan() {
        assertThat(f(DiaSemana.LUNES, 9, 11).seCruzaCon(f(DiaSemana.LUNES, 10, 12))).isTrue();
        assertThat(f(DiaSemana.LUNES, 10, 12).seCruzaCon(f(DiaSemana.LUNES, 9, 11))).isTrue();
    }

    @Test
    @DisplayName("Una franja contenida dentro de otra se cruza")
    void contenidaSeCruza() {
        assertThat(f(DiaSemana.MARTES, 7, 11).seCruzaCon(f(DiaSemana.MARTES, 8, 9))).isTrue();
    }

    @Test
    @DisplayName("Franjas consecutivas (una termina cuando empieza la otra) no se cruzan")
    void consecutivasNoSeCruzan() {
        assertThat(f(DiaSemana.LUNES, 7, 9).seCruzaCon(f(DiaSemana.LUNES, 9, 11))).isFalse();
    }

    @Test
    @DisplayName("Franjas de días distintos no se cruzan")
    void diasDistintosNoSeCruzan() {
        assertThat(f(DiaSemana.LUNES, 7, 9).seCruzaCon(f(DiaSemana.MARTES, 7, 9))).isFalse();
    }

    @Test
    @DisplayName("No se permite una franja con hora de inicio igual o posterior a la de fin")
    void franjaInvalida() {
        assertThatThrownBy(() -> f(DiaSemana.LUNES, 11, 9)).isInstanceOf(IllegalArgumentException.class);
    }
}
