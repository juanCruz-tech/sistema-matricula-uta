package co.edu.uta.matricula.carga;

import co.edu.uta.matricula.dominio.*;
import co.edu.uta.matricula.persistencia.*;
import co.edu.uta.matricula.servicio.OfertaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CP-07 · RNF-02 (eficiencia de desempeño): prueba de carga por HTTP contra la aplicación real.
 * N estudiantes (por defecto 200) inician sesión y luego, todos al mismo tiempo, consultan la
 * oferta, registran su matrícula y confirman el pago. Se mide el tiempo de respuesta de cada paso.
 * Ejecutar con: mvnw test -Pcarga   (o -Dcarga.usuarios=500 para cambiar la cantidad).
 */
@Tag("carga")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"pasarela.simulada.aprobacion-automatica=true", "spring.datasource.hikari.maximum-pool-size=30",
                "server.tomcat.threads.max=200", "matricula.revision-vencidas-ms=3600000"})
class CP07CargaTest {

    private static final Pattern CSRF = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");
    private static final Pattern ID_MATRICULA = Pattern.compile("/estudiante/matricula/(\\d+)/pagar");

    @LocalServerPort int puerto;
    @Autowired IUsuarioRepository usuarios;
    @Autowired IProgramaRepository programas;
    @Autowired IAsignaturaRepository asignaturas;
    @Autowired ICursoRepository cursos;
    @Autowired IMatriculaRepository matriculas;
    @Autowired OfertaService ofertaService;
    @Autowired PasswordEncoder codificador;

    @Test
    @DisplayName("CP-07: N usuarios concurrentes completan la matrícula con tiempo promedio < 3 s")
    void cargaConcurrente() throws Exception {
        int n = Integer.getInteger("carga.usuarios", 200);
        String base = "http://localhost:" + puerto;

        // Preparación: cursos con cupo suficiente para la mitad de los usuarios (hay competencia por cupos)
        ProgramaAcademico isis = programas.findAll().stream().filter(p -> p.getCodigo().equals("ISIS")).findFirst().orElseThrow();
        PeriodoAcademico periodo = ofertaService.periodoAbierto();
        List<Curso> lunes = new ArrayList<>();
        List<Curso> martes = new ArrayList<>();
        int cupo = Math.max(1, n / 10);
        for (int i = 0; i < 5; i++) {
            Asignatura a = asignaturas.save(new Asignatura("CG-L" + i, "Carga lunes " + i, 3, isis));
            lunes.add(cursos.save(new Curso(a, periodo, "1", cupo, "Docente",
                    List.of(new FranjaHoraria(DiaSemana.LUNES, LocalTime.of(7 + 2 * i, 0), LocalTime.of(8 + 2 * i, 0), "C-" + i)))));
            Asignatura b = asignaturas.save(new Asignatura("CG-M" + i, "Carga martes " + i, 3, isis));
            martes.add(cursos.save(new Curso(b, periodo, "1", cupo, "Docente",
                    List.of(new FranjaHoraria(DiaSemana.MARTES, LocalTime.of(7 + 2 * i, 0), LocalTime.of(8 + 2 * i, 0), "C-" + i)))));
        }
        String hash = codificador.encode("Carga123*");
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String id = "carga" + i;
            usuarios.save(new Estudiante(id, "Usuario de carga " + i, id + "@carga.uta.edu.co", hash, "C" + i, isis, EstadoAcademico.ACTIVO));
            ids.add(id);
        }

        // Fase 1: inicio de sesión de todos y calentamiento del servidor (no se mide)
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(n, 400));
        List<Future<Cliente>> sesiones = new ArrayList<>();
        for (String id : ids) sesiones.add(pool.submit(() -> Cliente.iniciar(base, id, "Carga123*")));
        List<Cliente> clientes = new ArrayList<>();
        for (Future<Cliente> f : sesiones) clientes.add(f.get(120, TimeUnit.SECONDS));

        // Fase 2: todos a la vez — oferta, registro de matrícula y confirmación del pago
        Random azar = new Random(42);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Medicion>> tareas = new ArrayList<>();
        for (Cliente c : clientes) {
            long l = lunes.get(azar.nextInt(5)).getId();
            long m = martes.get(azar.nextInt(5)).getId();
            tareas.add(pool.submit(() -> {
                salida.await();
                return c.matricularse(l, m);
            }));
        }
        long inicio = System.nanoTime();
        salida.countDown();
        List<Medicion> med = new ArrayList<>();
        for (Future<Medicion> f : tareas) med.add(f.get(300, TimeUnit.SECONDS));
        long totalMs = (System.nanoTime() - inicio) / 1_000_000;
        pool.shutdown();

        // Resultados
        long confirmadas = med.stream().filter(x -> x.resultado == Resultado.CONFIRMADA).count();
        long sinCupo = med.stream().filter(x -> x.resultado == Resultado.SIN_CUPO).count();
        long errores = med.stream().filter(x -> x.resultado == Resultado.ERROR).count();
        Estadistica oferta = Estadistica.de(med.stream().mapToLong(x -> x.ofertaMs).toArray());
        Estadistica registro = Estadistica.de(med.stream().mapToLong(x -> x.registroMs).toArray());
        Estadistica confirmacion = Estadistica.de(med.stream().filter(x -> x.confirmacionMs >= 0).mapToLong(x -> x.confirmacionMs).toArray());
        Estadistica transaccion = Estadistica.de(med.stream().mapToLong(x -> x.registroMs + Math.max(0, x.confirmacionMs)).toArray());

        boolean sobrecupo = false;
        for (Curso c : concat(lunes, martes)) {
            int disponibles = cursos.findById(c.getId()).orElseThrow().getCuposDisponibles();
            if (disponibles < 0 || disponibles > c.getCupoMaximo()) sobrecupo = true;
        }

        String informe = String.format(Locale.ROOT, """
                ===== CP-07 · Prueba de carga (RNF-02) =====
                Usuarios concurrentes: %d | Procesadores: %d | Duración total de la ráfaga: %d ms
                Resultado: %d confirmadas, %d rechazadas por falta de cupo, %d errores | Sobrecupo detectado: %s
                Paso                          promedio   p95       máximo   (ms)
                Consultar oferta              %8.1f %8d %8d
                Registrar matrícula           %8.1f %8d %8d
                Confirmar pago                %8.1f %8d %8d
                Transacción (registro+pago)   %8.1f %8d %8d
                """, n, Runtime.getRuntime().availableProcessors(), totalMs, confirmadas, sinCupo, errores, sobrecupo ? "SÍ" : "no",
                oferta.promedio, oferta.p95, oferta.max, registro.promedio, registro.p95, registro.max,
                confirmacion.promedio, confirmacion.p95, confirmacion.max, transaccion.promedio, transaccion.p95, transaccion.max);
        System.out.println(informe);
        Files.writeString(Path.of("target", "carga-" + n + ".txt"), informe);

        assertThat(errores).isZero();
        assertThat(sobrecupo).isFalse();
        assertThat(transaccion.promedio).as("tiempo promedio de la transacción").isLessThan(3000);
    }

    private static List<Curso> concat(List<Curso> a, List<Curso> b) {
        List<Curso> r = new ArrayList<>(a);
        r.addAll(b);
        return r;
    }

    enum Resultado { CONFIRMADA, SIN_CUPO, ERROR }

    record Medicion(long ofertaMs, long registroMs, long confirmacionMs, Resultado resultado) {
    }

    record Estadistica(double promedio, long p95, long max) {
        static Estadistica de(long[] v) {
            if (v.length == 0) return new Estadistica(0, 0, 0);
            long[] o = v.clone();
            Arrays.sort(o);
            return new Estadistica(Arrays.stream(o).average().orElse(0),
                    o[(int) Math.ceil(o.length * 0.95) - 1], o[o.length - 1]);
        }
    }

    /** Un navegador simulado con su propia sesión (cookie) y token CSRF. */
    static final class Cliente {
        private final HttpClient http;
        private final String base;

        private Cliente(String base) {
            this.base = base;
            this.http = HttpClient.newBuilder().cookieHandler(new CookieManager())
                    .followRedirects(HttpClient.Redirect.NEVER).connectTimeout(Duration.ofSeconds(30)).build();
        }

        static Cliente iniciar(String base, String usuario, String clave) throws IOException, InterruptedException {
            Cliente c = new Cliente(base);
            String token = c.csrf(c.get("/login").body());
            HttpResponse<String> r = c.post("/login", "username=" + usuario + "&password=" + enc(clave) + "&_csrf=" + token);
            if (r.headers().firstValue("Location").orElse("").contains("error")) {
                throw new IllegalStateException("No pudo iniciar sesión: " + usuario);
            }
            c.get("/estudiante/oferta");   // calentamiento: el servidor ya atendió esta pantalla antes de medir
            c.get("/estudiante/horario");
            return c;
        }

        Medicion matricularse(long cursoLunes, long cursoMartes) {
            try {
                long t0 = System.nanoTime();
                HttpResponse<String> oferta = get("/estudiante/oferta");
                long ofertaMs = ms(t0);
                String token = csrf(oferta.body());

                long t1 = System.nanoTime();
                HttpResponse<String> reg = post("/estudiante/matricula",
                        "cursoIds=" + cursoLunes + "&cursoIds=" + cursoMartes + "&_csrf=" + token);
                String destino = reg.headers().firstValue("Location").orElse("");
                if (destino.endsWith("/estudiante/oferta")) {
                    return new Medicion(ofertaMs, ms(t1), -1, Resultado.SIN_CUPO);
                }
                HttpResponse<String> vista = get("/estudiante/matricula");
                long registroMs = ms(t1);
                Matcher mm = ID_MATRICULA.matcher(vista.body());
                if (!mm.find()) {
                    return new Medicion(ofertaMs, registroMs, -1, Resultado.ERROR);
                }
                long t2 = System.nanoTime();
                HttpResponse<String> conf = get("/estudiante/matricula/" + mm.group(1) + "/retorno-pago");
                HttpResponse<String> fin = get("/estudiante/matricula");
                long confMs = ms(t2);
                boolean ok = conf.statusCode() == 302 && fin.body().contains("Confirmada");
                return new Medicion(ofertaMs, registroMs, confMs, ok ? Resultado.CONFIRMADA : Resultado.ERROR);
            } catch (Exception e) {
                return new Medicion(0, 0, -1, Resultado.ERROR);
            }
        }

        private HttpResponse<String> get(String ruta) throws IOException, InterruptedException {
            return http.send(HttpRequest.newBuilder(URI.create(base + ruta)).timeout(Duration.ofSeconds(120)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
        }

        private HttpResponse<String> post(String ruta, String cuerpo) throws IOException, InterruptedException {
            return http.send(HttpRequest.newBuilder(URI.create(base + ruta)).timeout(Duration.ofSeconds(120))
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString(cuerpo)).build(),
                    HttpResponse.BodyHandlers.ofString());
        }

        private String csrf(String html) {
            Matcher m = CSRF.matcher(html);
            if (!m.find()) throw new IllegalStateException("Sin token CSRF");
            return m.group(1);
        }

        private static String enc(String s) {
            return URLEncoder.encode(s, StandardCharsets.UTF_8);
        }

        private static long ms(long desde) {
            return (System.nanoTime() - desde) / 1_000_000;
        }
    }
}
