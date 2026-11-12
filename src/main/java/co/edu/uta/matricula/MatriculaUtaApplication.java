package co.edu.uta.matricula;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/** Punto de entrada del prototipo del Sistema de Matrícula UTA. */
@SpringBootApplication
@EnableScheduling
public class MatriculaUtaApplication {

    public static void main(String[] args) {
        SpringApplication.run(MatriculaUtaApplication.class, args);
    }

    /** Reloj inyectable: permite simular el paso del tiempo en las pruebas (vencimiento de 15 min). */
    @Bean
    public Clock reloj() {
        return Clock.systemDefaultZone();
    }
}
