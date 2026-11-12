# ADR-002 · Ajustes de implementación del prototipo

| Campo | Contenido |
| --- | --- |
| Estado | Aceptado (Fase 3) |
| Relación | Complementa el ADR-001. El estilo arquitectónico (monolito modular en capas con MVC) **no cambia**. |
| Responsables | Samuel Maldonado (Arquitecto) y Ronald Lozada (Programador) |

## Contexto

El prototipo debía funcionar de punta a punta en tres semanas y poder ejecutarse en el computador de cualquier integrante para la demostración en vivo. Algunos elementos del diagrama de despliegue de la Fase 2 (Redis, RabbitMQ, PostgreSQL con réplica, varias instancias tras un balanceador) requieren infraestructura que no está disponible en ese entorno.

## Decisión

| Diseño Fase 2 | Implementación del prototipo | Razón |
| --- | --- | --- |
| Cupos en caché Redis con operación atómica | `UPDATE ... WHERE cupos_disponibles > 0` atómico en la base de datos (`ICursoRepository.decrementarCupo`) | Misma garantía (cero sobrecupo, verificada en CP-03) sin un servidor adicional. |
| PostgreSQL primario + réplica | H2 embebida en modo PostgreSQL; perfil `postgres` disponible | La demo corre con un solo comando, sin instalar una base de datos. |
| RabbitMQ para notificaciones | No se implementó | RF-06 es Could y quedó fuera del alcance. |
| N réplicas tras balanceador | Una instancia | No hay infraestructura; el escalado horizontal no se verificó (ver CP-07). |
| `MatriculaRepositoryJPA` escrita a mano | Implementación generada por Spring Data a partir de `IMatriculaRepository` | Menos código y el mismo contrato. |
| `IPasarelaPagos.solicitarPago(Matricula)` | `solicitarPago(descripcion, valor)` | La capa de integración no depende de las entidades del dominio. |
| `Comprobante.generarPDF()` | `ComprobanteService.generarPdf()` | El dominio no depende de la librería de PDF (SRP). |
| `AdaptadorPasarelaPSE` | No se implementó; se usa `PasarelaSimulada` | No hay acceso a la pasarela real; la interfaz permite agregarlo después (OCP). |
| Estado BORRADOR persistido | El borrador vive en la pantalla de selección; se persiste desde PENDIENTE_PAGO | Simplifica la interfaz sin cambiar las transiciones. |

## Consecuencias

- Se gana: el prototipo se ejecuta en cualquier equipo con Java 17 y conserva las garantías críticas (reserva atómica, máquina de estados, control por rol).
- Se pierde: no se pudieron verificar la disponibilidad con varias instancias (RNF-01) ni la carga de 4,000 usuarios (RNF-02); quedan como deuda técnica.
- Volver al diseño completo no exige reescribir el negocio: cambia la configuración (PostgreSQL) y se agregan adaptadores detrás de las interfaces existentes.
