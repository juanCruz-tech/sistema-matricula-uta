package co.edu.uta.matricula.funcional;

import co.edu.uta.matricula.dominio.Curso;
import co.edu.uta.matricula.dominio.DiaSemana;
import co.edu.uta.matricula.dominio.ReglaNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CP-03 · RF-03 y fiabilidad: verificación de cupos en tiempo real bajo concurrencia.
 * 200 estudiantes intentan matricularse exactamente al mismo tiempo.
 */
class CP03CuposConcurrenciaTest extends IntegracionBase {

    private Resultado disparar(Curso curso, int estudiantes) throws Exception {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < estudiantes; i++) {
            ids.add(nuevoEstudiante());
        }
        ExecutorService pool = Executors.newFixedThreadPool(estudiantes);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger exitos = new AtomicInteger();
        AtomicInteger sinCupo = new AtomicInteger();
        AtomicInteger otros = new AtomicInteger();
        List<Future<?>> tareas = new ArrayList<>();
        for (String id : ids) {
            tareas.add(pool.submit(() -> {
                salida.await();
                try {
                    matriculaService.registrarMatricula(id, List.of(curso.getId()));
                    exitos.incrementAndGet();
                } catch (ReglaNegocioException e) {
                    if (e.getMessage().startsWith("Sin cupo")) sinCupo.incrementAndGet(); else otros.incrementAndGet();
                } catch (Exception e) {
                    otros.incrementAndGet();
                }
                return null;
            }));
        }
        long inicio = System.nanoTime();
        salida.countDown();
        for (Future<?> f : tareas) f.get(60, TimeUnit.SECONDS);
        long ms = (System.nanoTime() - inicio) / 1_000_000;
        pool.shutdown();
        return new Resultado(exitos.get(), sinCupo.get(), otros.get(), cuposDe(curso), ms);
    }

    record Resultado(int exitos, int sinCupo, int otros, int cuposFinales, long ms) {
    }

    @Test
    @DisplayName("CP-03: 200 solicitudes simultáneas por 1 cupo producen exactamente 1 reserva")
    void unCupoDoscientasSolicitudes() throws Exception {
        Curso curso = nuevoCurso(1, DiaSemana.SABADO, 8, 11);
        Resultado r = disparar(curso, 200);
        System.out.printf("[CP-03] 1 cupo / 200 solicitudes: %d reservas, %d sin cupo, %d otros, cupos finales %d, %d ms%n",
                r.exitos(), r.sinCupo(), r.otros(), r.cuposFinales(), r.ms());
        assertThat(r.exitos()).isEqualTo(1);
        assertThat(r.sinCupo()).isEqualTo(199);
        assertThat(r.otros()).isZero();
        assertThat(r.cuposFinales()).isZero();
    }

    @Test
    @DisplayName("CP-03: 200 solicitudes simultáneas por 50 cupos producen exactamente 50 reservas")
    void cincuentaCuposDoscientasSolicitudes() throws Exception {
        Curso curso = nuevoCurso(50, DiaSemana.SABADO, 13, 16);
        Resultado r = disparar(curso, 200);
        System.out.printf("[CP-03] 50 cupos / 200 solicitudes: %d reservas, %d sin cupo, %d otros, cupos finales %d, %d ms%n",
                r.exitos(), r.sinCupo(), r.otros(), r.cuposFinales(), r.ms());
        assertThat(r.exitos()).isEqualTo(50);
        assertThat(r.sinCupo()).isEqualTo(150);
        assertThat(r.cuposFinales()).isZero();
    }
}
