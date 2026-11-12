package co.edu.uta.matricula.dominio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static co.edu.uta.matricula.dominio.Fixtures.curso;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas unitarias de Curso: invariante 0 ≤ cuposDisponibles ≤ cupoMaximo (RF-03). */
class CursoTest {

    @Test
    @DisplayName("Reservar descuenta un cupo y no permite bajar de cero")
    void reservarRespetaElLimite() {
        Curso c = curso("T-1", 3, 2, DiaSemana.LUNES, 7, 9);
        assertThat(c.reservarCupo()).isTrue();
        assertThat(c.reservarCupo()).isTrue();
        assertThat(c.reservarCupo()).isFalse();
        assertThat(c.getCuposDisponibles()).isZero();
        assertThat(c.tieneCupo()).isFalse();
    }

    @Test
    @DisplayName("Liberar devuelve un cupo pero nunca supera el cupo máximo")
    void liberarNoSuperaElMaximo() {
        Curso c = curso("T-2", 3, 1, DiaSemana.LUNES, 7, 9);
        c.liberarCupo();
        assertThat(c.getCuposDisponibles()).isEqualTo(1);
        c.reservarCupo();
        c.liberarCupo();
        assertThat(c.getCuposDisponibles()).isEqualTo(1);
    }

    @Test
    @DisplayName("El cupo máximo no puede quedar por debajo de los cupos ya ocupados")
    void ajusteDeCupo() {
        Curso c = curso("T-3", 3, 5, DiaSemana.LUNES, 7, 9);
        c.reservarCupo();
        c.reservarCupo();
        c.ajustarCupoMaximo(10);
        assertThat(c.getCuposDisponibles()).isEqualTo(8);
        assertThatThrownBy(() -> c.ajustarCupoMaximo(1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Dos cursos se cruzan si alguna de sus franjas se solapa")
    void cruceEntreCursos() {
        Curso a = curso("T-4", 3, 5, DiaSemana.LUNES, 9, 11);
        Curso b = curso("T-5", 3, 5, DiaSemana.LUNES, 10, 12);
        Curso c = curso("T-6", 3, 5, DiaSemana.MARTES, 10, 12);
        assertThat(a.seCruzaCon(b)).isTrue();
        assertThat(a.seCruzaCon(c)).isFalse();
    }
}
