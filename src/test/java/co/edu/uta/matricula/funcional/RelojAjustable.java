package co.edu.uta.matricula.funcional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/** Reloj que las pruebas pueden adelantar para simular el paso del tiempo (vencimiento del pago). */
public class RelojAjustable extends Clock {

    private volatile Duration desfase = Duration.ZERO;

    public void adelantar(Duration d) {
        desfase = desfase.plus(d);
    }

    public void reiniciar() {
        desfase = Duration.ZERO;
    }

    @Override
    public ZoneId getZone() {
        return ZoneId.systemDefault();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return Instant.now().plus(desfase);
    }
}
