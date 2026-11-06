# AGENTS.md — VetSync

Reglas **obligatorias** para cualquier agente o persona que modifique este repositorio.

- **OBLIGATORIO** y **PROHIBIDO** no admiten excepciones, salvo las que este documento enumera de forma expresa.
- Si una regla estorba, **detente y pregunta**. No la esquives ni la reinterpretes.
- Las reglas marcadas **[AUTO]** las verifica `mvn verify` (Checkstyle y ArchUnit). Si el build falla por ellas, corrige el código; **nunca** `config/checkstyle/checkstyle.xml` ni las pruebas `ArquitecturaTest`.

## 1. Proyecto y alcance

VetSync es un sistema de gestión de citas y telemedicina veterinaria. Uso **personal y local**: no se despliega en ningún servidor.

Solo se implementan estos tres requisitos de negocio:

1. Registro de propietarios y mascotas con historial médico.
2. Agendamiento de citas sin choques de horario para los veterinarios.
3. Notificación simulada por correo cuando la cita está próxima.

PROHIBIDO añadir cualquier cosa fuera de ellos: API gateway, service discovery, broker de mensajes, autenticación/autorización, caché, módulo `common` o librería compartida, frontend, métricas, contenedores por servicio. La telemedicina se limita a los campos `modalidad` y `enlace_videollamada` de la cita. Si crees que falta algo, **pregunta**.

## 2. Stack y comandos

| Pieza | Decisión |
|---|---|
| Lenguaje | Java 21 (`release 21`) |
| Framework | Spring Boot 3.5.x (web, data-jpa) |
| Build | Maven multi-módulo |
| Base de datos | SQL Server 2022 en Docker, una base por servicio |
| Migraciones | Flyway |
| Pruebas | JUnit 5, AssertJ, Testcontainers (SQL Server real), ArchUnit |
| Estilo | Checkstyle (falla el build) |

PROHIBIDO: Lombok, H2/HSQLDB u otra BD en memoria en las pruebas, y añadir o cambiar dependencias o versiones sin pedirlo.

| Comando | Qué hace |
|---|---|
| `docker compose up -d` | Levanta SQL Server y crea las 3 bases. Requiere `.env` (copia `.env.example`). |
| `mvn test` | Estilo + pruebas unitarias + ArchUnit. Rápido, **sin Docker**. Es el ciclo TDD. |
| `mvn verify` | Todo lo anterior + pruebas de integración `*IT` (Testcontainers, requiere Docker en marcha). |
| `mvn -pl appointment-service spring-boot:run` | Arranca un servicio. |

Una tarea no está terminada hasta que `mvn verify` esté en verde.

Sin Maven instalado, usa el wrapper del repositorio en lugar de `mvn`: `./mvnw` (Git Bash) o `.\mvnw.cmd` (PowerShell).

## 3. Arquitectura

Tres microservicios, cada uno con su propia base de datos:

| Servicio | Puerto | Base de datos | Requisito |
|---|---|---|---|
| `clinic-service` | 8081 | `vetsync_clinic` | 1: propietarios, mascotas, historial médico |
| `appointment-service` | 8082 | `vetsync_citas` | 2: veterinarios, agenda sin choques |
| `notification-service` | 8083 | `vetsync_notificaciones` | 3: recordatorios simulados |

Estructura de cada servicio (`com.vetsync.<servicio>`):

```
domain/                  entidades, value objects, enums, excepciones de dominio. Java puro.
application/
  port/in/               interfaces de casos de uso            (AgendarCitaUseCase)
  port/out/              interfaces de salida                  (CitaRepository, ClinicaGateway, CorreoSender)
  service/               implementaciones de los casos de uso  (AgendarCitaService)
infrastructure/
  web/                   controllers, DTOs REST, @RestControllerAdvice
  persistence/           entidades JPA, repositorios Spring Data, adapters (CitaRepositoryAdapter)
  client/ mail/ scheduler/   adapters hacia otros servicios, correo simulado, tareas programadas
  config/                @Configuration, @ConfigurationProperties, Clock
```

Reglas de dependencia **[AUTO]**:

- La dirección es `infrastructure → application → domain`, nunca al revés.
- `domain` no importa Spring ni `jakarta.persistence`.
- `application` no importa `infrastructure`.
- `infrastructure.web` solo conoce los puertos de entrada (`port/in`), nunca `application.service` ni `persistence`.

Reglas de servicios:

- Comunicación por REST síncrono y en un solo sentido: `appointment → clinic` (valida la mascota y obtiene el contacto) y `notification → appointment` (consulta citas por recordar). Nunca al revés.
- Un servicio solo accede a **su** base de datos. PROHIBIDO joins o FKs entre bases; se referencian por ID.
- PROHIBIDO compartir código entre servicios. Duplicar un DTO pequeño es preferible a acoplar.
- La hora se obtiene de un `java.time.Clock` inyectado. PROHIBIDO `LocalDateTime.now()`, `Instant.now()` y similares en la lógica de negocio.

## 4. Reglas obligatorias

### 4.1 Nombrado en camelCase [AUTO]

| Elemento | Formato | Ejemplo |
|---|---|---|
| Variables locales, parámetros, campos, métodos, componentes de `record`, parámetros de lambda y de `catch` | `camelCase` | `fechaNacimiento`, `buscarPorId`, `causa` |
| Clases, interfaces, enums, records | `PascalCase` | `AgendarCitaUseCase` |
| Constantes `static final` y valores de enum | `UPPER_SNAKE_CASE` (única excepción a camelCase, convención del lenguaje) | `DURACION_FRANJA_MINUTOS` |
| Paquetes | minúsculas, sin guiones bajos | `com.vetsync.clinic.domain` |
| Tablas y columnas SQL | `snake_case`, solo en SQL; JPA hace el mapeo | `contacto_correo` ↔ `contactoCorreo` |

PROHIBIDO:

- `snake_case`, `PascalCase` o mayúsculas en variables y métodos (`nombre_mascota`, `Edad`).
- Notación húngara o prefijos (`strNombre`, `iEdad`, `mCampo`, `_campo`).
- Nombres de una sola letra, salvo `i` y `j` como contador de `for`.
- Abreviaturas crípticas (`msc`, `cli`, `tmp`).
- Siglas en mayúsculas dentro del nombre: `urlBase`, no `URLBase` (por revisión).
- Escribir un nombre `snake_case` en Java para "coincidir" con la BD.

El dominio se nombra en español (`Cita`, `Mascota`); los términos técnicos y las capas, en inglés.

```java
// ✅
private final Clock reloj;
LocalDateTime inicioCita = comando.inicio();

// ❌
private final Clock Reloj;
LocalDateTime inicio_cita = comando.inicio();
String strNombre;  int x;
```

### 4.2 Uso obligatorio de interfaces [AUTO]

Todo colaborador con comportamiento tiene una interfaz y sus consumidores dependen **solo** de ella:

| Rol | Interfaz | Implementación |
|---|---|---|
| Caso de uso | `application/port/in`: `XxxUseCase` | `application/service`: `XxxService` |
| Salida (BD, otro servicio, correo, reloj externo) | `application/port/out`: `XxxRepository`, `XxxGateway`, `XxxSender` | `infrastructure`: `XxxAdapter` |

Reglas:

- Los controllers inyectan `XxxUseCase`, nunca `XxxService`.
- Inyección **por constructor**. PROHIBIDO `@Autowired` sobre campos y `new` de colaboradores dentro de la lógica.
- Interfaces pequeñas, una por rol. PROHIBIDAS las interfaces genéricas con muchos métodos (`Manager`, `Helper`, `Util`).
- PROHIBIDO el prefijo `I` y el sufijo `Impl`. La implementación se nombra por su rol (`Service`, `Adapter`) o por su tecnología (`CitaRepositoryAdapter`, `CorreoSimuladoAdapter`).
- Los repositorios de Spring Data (`CitaJpaRepository`) son un detalle interno de `persistence`: solo los usa su `Adapter`.
- Los fakes de pruebas implementan las **mismas** interfaces que los adapters reales.

Exentos de tener interfaz: entidades y value objects de dominio, `record`s/DTOs, enums, excepciones, clases `@Configuration` y `@ConfigurationProperties`, `*Application`, controllers, `@RestControllerAdvice` y código de pruebas.

```java
// ✅ depende del puerto, por constructor
public class CitaController {
    private final AgendarCitaUseCase agendarCita;

    public CitaController(AgendarCitaUseCase agendarCita) {
        this.agendarCita = agendarCita;
    }
}

// ❌ clase concreta, inyección por campo
private final AgendarCitaService servicio;
@Autowired private CitaRepositoryAdapter repositorio;
```

### 4.3 Manejo de errores con try/catch [AUTO parcial]

1. **Todo** acceso a BD, HTTP a otro servicio, sistema de archivos o envío de correo va dentro de un `try/catch` en su `Adapter` (`infrastructure`). El `catch` captura la excepción técnica **específica** y la traduce a una excepción propia, **conservando la causa**.
2. `domain` y `application` no capturan excepciones técnicas: lanzan excepciones de negocio específicas (no verificadas, mensajes en español, sin datos sensibles).
3. Hay un único `@RestControllerAdvice` por servicio que traduce excepciones a `ProblemDetail`: 404 no encontrado, 409 conflicto, 422 regla de negocio, 502/503 dependencia caída, 500 inesperado **sin** stacktrace.
4. Cada `catch` **traduce y relanza** o **registra y termina**; nunca ambas cosas en la misma capa. El registro lo hace el punto que maneja el error por última vez.
5. Todo `AutoCloseable` se usa con `try-with-resources`.
6. Cada rama `catch` y cada excepción de negocio tiene su prueba.

PROHIBIDO **[AUTO]**:

- `catch (Exception | Throwable | RuntimeException | Error)`, y declarar `throws` de `Throwable`, `RuntimeException` o `Error`.
- Bloques `catch` vacíos o que se tragan el error.
- `e.printStackTrace()`, `System.out` y `System.err`. Se usa SLF4J: `private static final Logger LOGGER`.

PROHIBIDO por revisión: `return null` desde un `catch`; usar `try/catch` como control de flujo (se valida con condiciones o value objects); perder la causa original.

```java
// ✅ adapter: excepción técnica específica → excepción de negocio con causa
@Override
public Cita guardar(Cita cita) {
    try {
        return mapper.aDominio(citaJpaRepository.saveAndFlush(mapper.aEntidad(cita)));
    } catch (DataIntegrityViolationException causa) {
        throw new ConflictoDeHorarioException("El veterinario ya tiene una cita en ese horario", causa);
    }
}

// ❌
catch (Exception e) { }
catch (Exception e) { e.printStackTrace(); }
catch (DataAccessException e) { return null; }
```

### 4.4 Prohibido `any` [AUTO]

Java es de tipado estático y no tiene `any`. Su equivalente son `Object`, los tipos crudos y los casts sin comprobar; todos están **PROHIBIDOS**:

- `Object` como tipo de variable, parámetro, retorno, campo o argumento genérico (`List<Object>`, `Map<String, Object>`). Única excepción: la firma heredada `equals(Object)`.
- Tipos crudos (`List`, `Map`, `Optional` sin parámetros de tipo). El compilador falla con `-Xlint:rawtypes -Xlint:unchecked -Werror`.
- `@SuppressWarnings("unchecked")`, `@SuppressWarnings("rawtypes")` y casts sin `instanceof` previo.
- Comodín `?` sin cota (`List<?>`), salvo cuando una API de terceros lo exija (p. ej. `MSSQLServerContainer<?>` de Testcontainers).
- Modelar datos con `Map<String, Object>` o `JsonNode`: se usan `record`s tipados.

```java
// ✅
public record ContactoDeMascota(String nombre, String correo) { }
List<RegistroMedico> historial = repositorio.buscarPorMascota(mascotaId);

// ❌
Map<String, Object> contacto = cliente.obtener(mascotaId);
List historial = repositorio.buscarPorMascota(mascotaId);
```

## 5. TDD

1. **Rojo:** escribe una prueba que falle y ejecútala; comprueba que falla por la razón correcta.
2. **Verde:** escribe el código mínimo que la haga pasar.
3. **Refactor:** limpia con las pruebas en verde.

PROHIBIDO escribir código de producción con comportamiento sin una prueba fallida previa. Se exceptúa la configuración y estructura sin comportamiento.

- **domain y application:** pruebas unitarias sin Spring, con fakes en memoria (`InMemoryCitaRepository`, `RelojFijo`) en `src/test/.../fakes`. Se prefieren los fakes a los mocks para los puertos propios.
- **Adapters y BD:** pruebas `*IT` con Testcontainers y SQL Server real. PROHIBIDAS las BD en memoria: no soportan los índices filtrados que usa el esquema.
- **Controllers:** `@WebMvcTest`.
- Nombres de prueba en camelCase descriptivo en español: `rechazaCitaCuandoElVeterinarioEstaOcupado`.
- Un comportamiento por prueba, estructura Given-When-Then, sin lógica condicional ni dependencia del reloj real ni del orden de ejecución.
- `mvn test` nunca puede requerir Docker.

## 6. SOLID (lista de verificación)

- **S:** una clase, una razón de cambio; un caso de uso por clase.
- **O:** se extiende añadiendo implementaciones de una interfaz, no editando cadenas de `if/else` por tipo.
- **L:** toda implementación respeta el contrato de su interfaz (mismas excepciones y precondiciones). Los fakes también.
- **I:** interfaces pequeñas por rol; ningún consumidor depende de métodos que no usa.
- **D:** se depende de interfaces, la inyección es por constructor y el cableado vive en `infrastructure/config`.

## 7. Base de datos y configuración

- El esquema **solo** cambia con migraciones Flyway: `src/main/resources/db/migration/V{n}__descripcion.sql`. PROHIBIDO editar una migración ya aplicada o usar `ddl-auto` distinto de `validate`.
- Las garantías críticas viven también en la BD. Ejemplo: el índice único filtrado `UX_cita_veterinario_inicio` impide dos citas `PROGRAMADA` de un veterinario en el mismo horario.
- Las franjas son de 30 minutos alineadas a la media hora. Cambiarlo exige una migración nueva y actualizar el dominio.
- **Secretos:** PROHIBIDO escribir contraseñas en código, `application.yml`, pruebas o documentación. Se leen de variables de entorno o de `.env`, que git ignora; la plantilla es `.env.example`.
- Configuración con `@ConfigurationProperties` (records). PROHIBIDO `@Value`.
- Fechas en hora local de la clínica (`DATETIME2` sin zona).
- Cada servicio mantiene sus propias migraciones y su propia base.

## 8. Definición de terminado

- [ ] La prueba se escribió primero y se vio fallar.
- [ ] `mvn verify` está en verde (Checkstyle, ArchUnit, unitarias e integración).
- [ ] Sin código comentado, `TODO` ni imports sin usar.
- [ ] Todo cambio de esquema es una migración nueva.
- [ ] Nada fuera del alcance de la sección 1.
- [ ] Se informó con honestidad de lo que no se pudo verificar.
