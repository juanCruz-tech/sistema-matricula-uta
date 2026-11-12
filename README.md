# Sistema de Matrícula UTA — Prototipo (Fase 3)

Prototipo funcional del **Sistema de Matrícula de la Universidad Tecnológica de los Andes (UTA)**, proyecto final del curso de Ingeniería de Software (Universidad Cooperativa de Colombia, docente Fredys A. Simanca H.).

Implementa de punta a punta los requerimientos **Must** de la Fase 1 sobre la arquitectura de la Fase 2 (monolito modular en capas con MVC, ADR-001):

| Requerimiento | Qué hace el prototipo |
| --- | --- |
| RF-01 | El estudiante selecciona materias, reserva cupos, paga y obtiene un comprobante en PDF. |
| RF-02 | Horario semanal visible apenas se confirma la matrícula, con detección de cruces. |
| RF-03 | Cupos en tiempo real con reserva atómica: nunca hay sobrecupo, ni con cientos de solicitudes simultáneas. |
| RF-04 | La matrícula solo se confirma con pago aprobado; si se rechaza o vence (15 min), se anula y libera los cupos. |
| RF-05 (Should) | Reporte de matriculados por facultad y programa, exportable a PDF. |
| RNF-04 | Inicio de sesión, contraseñas con BCrypt, acceso por rol y protección CSRF. |

No implementado: RF-06 (notificación por correo, prioridad Could). Ver la sección «Defectos conocidos y deuda técnica» del documento final.

## Tecnologías

Java 17 · Spring Boot 3.3 (Web MVC, Thymeleaf, Security, Data JPA) · H2 (base embebida) o PostgreSQL · OpenPDF · JUnit 5, Spring Test, ArchUnit, JaCoCo.

## Cómo ejecutarlo en Windows

1. **Instalar Java 17**: descargar *Eclipse Temurin 17 (LTS)* desde <https://adoptium.net/es/temurin/releases/?version=17> (instalador `.msi` para Windows x64). En el instalador, activar la opción **«Set JAVA_HOME variable»**.
2. **Instalar Git**: <https://git-scm.com/download/win> (dejar las opciones por defecto).
3. Abrir **PowerShell** y comprobar: `java -version` debe mostrar la versión 17.
4. Descargar el proyecto:
   ```powershell
   git clone https://github.com/<usuario-de-juan>/sistema-matricula-uta.git
   cd sistema-matricula-uta
   ```
5. Ejecutar (la primera vez descarga Maven y las librerías, tarda unos minutos):
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```
6. Abrir el navegador en **<http://localhost:8080>**. Para detenerlo: `Ctrl + C` en PowerShell.

> En macOS o Linux se usa `./mvnw` en lugar de `.\mvnw.cmd`.

## Usuarios de prueba (datos ficticios)

| Usuario | Clave | Rol | Para qué sirve en la demo |
| --- | --- | --- | --- |
| `laura.gomez` | `Uta2027*` | Estudiante (Ing. de Sistemas) | Flujo completo de matrícula |
| `andres.rojas` | `Uta2027*` | Estudiante (Ing. de Sistemas) | Ver un curso sin cupo y un pago rechazado |
| `daniel.mejia` | `Uta2027*` | Estudiante inactivo | Ver que no puede matricularse |
| `admin.registro` | `Admin2027*` | Administrador | Cupos y reportes |

Los datos se cargan al iniciar y se borran al detener la aplicación (base en memoria).

## Pruebas

```powershell
.\mvnw.cmd test                              # 37 pruebas unitarias, funcionales y de arquitectura
.\mvnw.cmd test -Pcarga                      # prueba de carga con 200 usuarios simultáneos
.\mvnw.cmd test -Pcarga -Dcarga.usuarios=500 # con otra cantidad de usuarios
```

El informe de cobertura queda en `target/site/jacoco/index.html` y los resultados de carga en `target/carga-<N>.txt`.

| Caso | Requerimiento | Clase de prueba |
| --- | --- | --- |
| CP-01 | RF-01 | `funcional/CP01MatriculaTest` |
| CP-02 | RF-02 | `funcional/CP02HorarioTest` |
| CP-03 | RF-03 | `funcional/CP03CuposConcurrenciaTest` |
| CP-04 | RF-04 | `funcional/CP04PagoTest` |
| CP-05 | RF-05 | `funcional/CP05ReporteTest` |
| CP-06 | RNF-04 | `funcional/CP06SeguridadTest` |
| CP-07 | RNF-02 | `carga/CP07CargaTest` |
| CP-08 | RNF-03 | `arquitectura/CP08ArquitecturaTest` |

## Estructura y arquitectura

```
src/main/java/co/edu/uta/matricula/
├── presentacion/   Controladores MVC (EstudianteController, AdminController)  ← capa de presentación
├── servicio/       MatriculaService, CupoService, HorarioService, ReporteService… ← capa de negocio
├── dominio/        Matricula, Curso, FranjaHoraria, Pago… (diagrama de clases)   ← modelo
├── persistencia/   Repositorios (IMatriculaRepository, ICursoRepository…)        ← persistencia
├── integracion/    IPasarelaPagos + PasarelaSimulada                             ← integración
├── seguridad/      Spring Security (componente reutilizable)
└── config/         Datos de demostración
src/main/resources/templates/   Vistas Thymeleaf (la «V» de MVC)
```

La prueba `CP08ArquitecturaTest` falla si alguien rompe las reglas de capas. Las decisiones de implementación que se apartaron de la Fase 2 están en `docs/adr/ADR-002-ajustes-de-implementacion.md`.

## Equipo

| Integrante | Rol IEEE 1074 |
| --- | --- |
| Juan Esteban Cruz Borja | Analista |
| Samuel Camilo Maldonado Mariño | Diseñador / Arquitecto |
| Sayra Nicole Herrera Herrera | Gestora de proyecto / Probadora |
| Ronald Lozada | Programador |

**Declaración de uso de IA:** el código se desarrolló con asistencia de herramientas de inteligencia artificial. El equipo lo revisó, lo ejecutó y lo verificó con las pruebas automatizadas incluidas.
