package co.edu.uta.matricula.servicio;

import co.edu.uta.matricula.dominio.*;
import co.edu.uta.matricula.integracion.pagos.IPasarelaPagos;
import co.edu.uta.matricula.persistencia.ICursoRepository;
import co.edu.uta.matricula.persistencia.IMatriculaRepository;
import co.edu.uta.matricula.persistencia.IUsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orquesta el caso de uso «Registrar matrícula» (RF-01, RF-03, RF-04).
 * Depende de interfaces (IPasarelaPagos, IMatriculaRepository) y no de clases concretas (DIP).
 */
@Service
public class MatriculaService {

    private final IMatriculaRepository matriculas;
    private final ICursoRepository cursos;
    private final IUsuarioRepository usuarios;
    private final CupoService cupoService;
    private final OfertaService ofertaService;
    private final IPasarelaPagos pasarela;
    private final Clock reloj;
    private final BigDecimal valorCredito;
    private final long minutosEsperaPago;

    public MatriculaService(IMatriculaRepository matriculas, ICursoRepository cursos, IUsuarioRepository usuarios,
                            CupoService cupoService, OfertaService ofertaService, IPasarelaPagos pasarela, Clock reloj,
                            @Value("${matricula.valor-credito:350000}") BigDecimal valorCredito,
                            @Value("${matricula.minutos-espera-pago:15}") long minutosEsperaPago) {
        this.matriculas = matriculas;
        this.cursos = cursos;
        this.usuarios = usuarios;
        this.cupoService = cupoService;
        this.ofertaService = ofertaService;
        this.pasarela = pasarela;
        this.reloj = reloj;
        this.valorCredito = valorCredito;
        this.minutosEsperaPago = minutosEsperaPago;
    }

    /**
     * Registra la selección de cursos: valida reglas, reserva los cupos de forma atómica y deja la
     * matrícula en PENDIENTE_PAGO con su orden de pago. Si un solo cupo falla, toda la operación se
     * revierte (la transacción devuelve los cupos ya reservados).
     */
    @Transactional
    public Matricula registrarMatricula(String idUsuario, List<Long> cursoIds) {
        if (cursoIds == null || cursoIds.isEmpty()) {
            throw new ReglaNegocioException("Debes seleccionar al menos un curso.");
        }
        Estudiante estudiante = estudiante(idUsuario);
        if (!estudiante.estaHabilitado()) {
            throw new ReglaNegocioException("Tu estado académico no te permite matricularte. Comunícate con Registro Académico.");
        }
        PeriodoAcademico periodo = ofertaService.periodoAbierto();
        if (matriculas.buscarActiva(estudiante, periodo).isPresent()) {
            throw new ReglaNegocioException("Ya tienes una matrícula activa en el periodo " + periodo.getCodigo() + ".");
        }

        LocalDateTime ahora = LocalDateTime.now(reloj);
        Matricula matricula = new Matricula(estudiante, periodo, ahora);
        for (Long id : cursoIds.stream().distinct().toList()) {
            Curso curso = cursos.findById(id).orElseThrow(() -> new ReglaNegocioException("El curso " + id + " no existe."));
            if (!curso.getPeriodo().getId().equals(periodo.getId())) {
                throw new ReglaNegocioException(curso.nombreCompleto() + " no pertenece al periodo abierto.");
            }
            matricula.agregarCurso(curso, ahora);
        }
        for (Curso curso : matricula.cursosInscritos()) {
            if (!cupoService.reservar(curso.getId())) {
                throw new ReglaNegocioException("Sin cupo: " + curso.nombreCompleto()
                        + " ya no tiene cupos disponibles. No se reservó ningún curso; ajusta tu selección.");
            }
        }
        matricula.confirmarSeleccion(ahora);
        String referencia = pasarela.solicitarPago("Matrícula " + periodo.getCodigo() + " — " + estudiante.getCodigo(),
                matricula.calcularValor(valorCredito));
        matricula.asociarPago(new Pago(referencia, matricula.calcularValor(valorCredito)));
        return matriculas.save(matricula);
    }

    /**
     * Consulta la pasarela y aplica el resultado: APROBADO → CONFIRMADA con comprobante;
     * RECHAZADO → ANULADA y liberación de cupos; PENDIENTE → sin cambios.
     */
    @Transactional
    public Matricula confirmarPago(String idUsuario, Long matriculaId) {
        Matricula matricula = propia(idUsuario, matriculaId);
        if (matricula.getEstado() != EstadoMatricula.PENDIENTE_PAGO) {
            return matricula;
        }
        EstadoPago resultado = pasarela.verificarPago(matricula.getPago().getReferencia());
        LocalDateTime ahora = LocalDateTime.now(reloj);
        switch (resultado) {
            case APROBADO -> {
                matricula.getPago().registrarRespuesta(EstadoPago.APROBADO, ahora);
                matricula.confirmar(nuevoComprobante(matricula, ahora));
            }
            case RECHAZADO -> {
                matricula.getPago().registrarRespuesta(EstadoPago.RECHAZADO, ahora);
                anularYLiberar(matricula, "Pago rechazado por la pasarela");
            }
            case PENDIENTE -> { /* el estudiante aún no completa el pago */ }
        }
        return matricula;
    }

    @Transactional
    public Matricula cancelar(String idUsuario, Long matriculaId) {
        Matricula matricula = propia(idUsuario, matriculaId);
        if (matricula.getEstado() != EstadoMatricula.PENDIENTE_PAGO) {
            throw new ReglaNegocioException("Solo se puede cancelar una matrícula pendiente de pago.");
        }
        anularYLiberar(matricula, "Cancelada por el estudiante");
        return matricula;
    }

    /** Transición after(15 min): anula las matrículas cuyo pago no llegó a tiempo y libera sus cupos. */
    @Transactional
    public int anularVencidas() {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        List<Matricula> vencidas = matriculas.findByEstadoAndFechaReservaBefore(
                EstadoMatricula.PENDIENTE_PAGO, ahora.minusMinutes(minutosEsperaPago));
        for (Matricula m : vencidas) {
            if (m.getPago() != null) {
                m.getPago().registrarRespuesta(EstadoPago.RECHAZADO, ahora);
            }
            anularYLiberar(m, "Tiempo de espera del pago vencido (" + minutosEsperaPago + " min)");
        }
        return vencidas.size();
    }

    @Transactional(readOnly = true)
    public Optional<Matricula> matriculaActual(String idUsuario) {
        Estudiante estudiante = estudiante(idUsuario);
        PeriodoAcademico periodo = ofertaService.periodoAbierto();
        List<Matricula> historial = matriculas.buscarHistorial(estudiante, periodo);
        historial.forEach(this::inicializar);
        return historial.stream().findFirst();
    }

    @Transactional(readOnly = true)
    public Matricula consultar(String idUsuario, Long matriculaId) {
        return propia(idUsuario, matriculaId);
    }

    public BigDecimal getValorCredito() {
        return valorCredito;
    }

    public long getMinutosEsperaPago() {
        return minutosEsperaPago;
    }

    private void anularYLiberar(Matricula matricula, String motivo) {
        for (Curso curso : new ArrayList<>(matricula.anular(motivo))) {
            cupoService.liberar(curso.getId());
        }
    }

    private Comprobante nuevoComprobante(Matricula m, LocalDateTime ahora) {
        String numero = "UTA-" + m.getPeriodo().getCodigo() + "-" + String.format("%06d", m.getId());
        String codigo = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        return new Comprobante(numero, ahora, codigo);
    }

    private Estudiante estudiante(String idUsuario) {
        return usuarios.buscarEstudiante(idUsuario)
                .orElseThrow(() -> new ReglaNegocioException("El usuario no es un estudiante."));
    }

    private Matricula propia(String idUsuario, Long matriculaId) {
        Matricula m = matriculas.findById(matriculaId)
                .orElseThrow(() -> new ReglaNegocioException("La matrícula no existe."));
        if (!m.getEstudiante().getIdUsuario().equals(idUsuario)) {
            throw new ReglaNegocioException("La matrícula no pertenece a este usuario.");
        }
        inicializar(m);
        return m;
    }

    /** Carga las asociaciones perezosas que las vistas necesitan. */
    private void inicializar(Matricula m) {
        m.getDetalles().forEach(d -> d.getCurso().getFranjas().size());
        if (m.getPago() != null) m.getPago().getReferencia();
        if (m.getComprobante() != null) m.getComprobante().getNumero();
        m.getPeriodo().getCodigo();
        m.getEstudiante().getPrograma().getNombre();
    }
}
