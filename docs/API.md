# VetSync — Documentación técnica de la API

Referencia de los endpoints REST de los tres microservicios de VetSync, sus validaciones, el formato de errores y la lista de peticiones para probar el sistema. Para instalar y arrancar los servicios, ver el [README](../README.md).

## Contenido

1. [Convenciones generales](#1-convenciones-generales)
2. [clinic-service (8081)](#2-clinic-service-8081)
3. [appointment-service (8082)](#3-appointment-service-8082)
4. [notification-service (8083)](#4-notification-service-8083)
5. [Formato de errores](#5-formato-de-errores)
6. [Contratos internos entre servicios](#6-contratos-internos-entre-servicios)
7. [Modelo de datos](#7-modelo-de-datos)
8. [Lista de peticiones de prueba](#8-lista-de-peticiones-de-prueba)

---

## 1. Convenciones generales

| Aspecto | Valor |
|---|---|
| Protocolo | HTTP/1.1, JSON (`Content-Type: application/json`) |
| Autenticación | **Ninguna.** Los servicios solo escuchan en `127.0.0.1` |
| Idioma | Campos y mensajes de error en español, sin tildes en los mensajes del servidor |
| Fechas con hora | ISO-8601 **sin zona**, en hora local de la clínica: `2026-12-01T09:00:00` |
| Fechas sin hora | ISO-8601: `2020-03-15` |
| Identificadores | Enteros de 64 bits generados por la base de datos (`1`, `2`, …) |
| Campos opcionales ausentes | Se devuelven como `null` |
| Campos desconocidos en el cuerpo | Se ignoran |
| Errores | `ProblemDetail` (RFC 7807), ver [sección 5](#5-formato-de-errores) |

| Servicio | URL base |
|---|---|
| `clinic-service` | `http://127.0.0.1:8081` |
| `appointment-service` | `http://127.0.0.1:8082` |
| `notification-service` | `http://127.0.0.1:8083` |

### Resumen de endpoints

| Servicio | Método | Ruta | Descripción | Éxito |
|---|---|---|---|---|
| clinic | `POST` | `/propietarios` | Registrar propietario | `201` |
| clinic | `POST` | `/propietarios/{propietarioId}/mascotas` | Registrar mascota de un propietario | `201` |
| clinic | `GET` | `/mascotas/{mascotaId}` | Consultar mascota con su propietario | `200` |
| clinic | `POST` | `/mascotas/{mascotaId}/historial` | Añadir entrada al historial médico | `201` |
| clinic | `GET` | `/mascotas/{mascotaId}/historial` | Consultar historial médico | `200` |
| appointment | `POST` | `/veterinarios` | Registrar veterinario | `201` |
| appointment | `GET` | `/veterinarios` | Listar veterinarios | `200` |
| appointment | `POST` | `/citas` | Agendar cita | `201` |
| appointment | `GET` | `/citas/por-recordar` | Citas programadas en un rango (uso interno) | `200` |
| notification | `GET` | `/notificaciones` | Listar recordatorios enviados | `200` |

---

## 2. clinic-service (8081)

Propietarios, mascotas e historial médico (requisito 1). Base de datos: `vetsync_clinic`.

### 2.1 Registrar propietario

`POST /propietarios` → `201 Created`

**Cuerpo**

| Campo | Tipo | Obligatorio | Reglas |
|---|---|---|---|
| `nombre` | string | Sí | No vacío, máximo 100 caracteres |
| `correo` | string | Sí | Máximo 150 caracteres, formato `usuario@dominio.ext`, **único** en el sistema |
| `telefono` | string | No | Máximo 20 caracteres |

Los textos de una línea no admiten caracteres de control (saltos de línea, tabuladores).

```json
{ "nombre": "Ana Perez", "correo": "ana@correo.com", "telefono": "555-1234" }
```

**Respuesta `201`**

```json
{ "id": 1, "nombre": "Ana Perez", "correo": "ana@correo.com", "telefono": "555-1234" }
```

**Errores:** `400` cuerpo inválido · `409` el correo ya está registrado · `503` base de datos no disponible.

### 2.2 Registrar mascota

`POST /propietarios/{propietarioId}/mascotas` → `201 Created`

| Parámetro de ruta | Tipo | Descripción |
|---|---|---|
| `propietarioId` | entero | Id de un propietario existente |

**Cuerpo**

| Campo | Tipo | Obligatorio | Reglas |
|---|---|---|---|
| `nombre` | string | Sí | No vacío, máximo 80 caracteres |
| `especie` | string | Sí | No vacío, máximo 40 caracteres |
| `raza` | string | No | Máximo 60 caracteres |
| `sexo` | enum | No | `M` (macho) o `H` (hembra) |
| `fechaNacimiento` | fecha | No | Formato `AAAA-MM-DD` |

```json
{ "nombre": "Rex", "especie": "Perro", "raza": "Labrador", "sexo": "M", "fechaNacimiento": "2020-03-15" }
```

**Respuesta `201`**

```json
{
  "id": 1,
  "nombre": "Rex",
  "especie": "Perro",
  "raza": "Labrador",
  "sexo": "M",
  "fechaNacimiento": "2020-03-15",
  "propietario": { "id": 1, "nombre": "Ana Perez", "correo": "ana@correo.com" }
}
```

**Errores:** `400` cuerpo inválido (incluye `sexo` fuera de `M`/`H` o fecha mal formada) · `404` el propietario no existe · `503`.

### 2.3 Consultar mascota

`GET /mascotas/{mascotaId}` → `200 OK`

Devuelve la mascota con los datos mínimos de su propietario. El teléfono del propietario **no** se expone. Esta es la misma forma que consume `appointment-service` (ver [sección 6](#6-contratos-internos-entre-servicios)).

**Respuesta `200`:** igual que la de [2.2](#22-registrar-mascota).

**Errores:** `400` si `mascotaId` no es numérico · `404` la mascota no existe · `503`.

### 2.4 Añadir entrada al historial médico

`POST /mascotas/{mascotaId}/historial` → `201 Created`

El historial es de **solo agregar**: no hay endpoints para editar ni borrar entradas. La fecha de la entrada la asigna el servidor con su reloj.

**Cuerpo**

| Campo | Tipo | Obligatorio | Reglas |
|---|---|---|---|
| `tipo` | enum | Sí | `CONSULTA`, `VACUNA`, `CIRUGIA`, `DESPARASITACION` u `OTRO` |
| `motivo` | string | Sí | No vacío, máximo 200 caracteres, una sola línea |
| `diagnostico` | string | No | Máximo 4000 caracteres; admite saltos de línea |
| `tratamiento` | string | No | Máximo 4000 caracteres; admite saltos de línea |
| `pesoKg` | número | No | Mayor que 0 y como máximo `999.99` |
| `veterinarioId` | entero | No | Entero positivo. Es una referencia lógica a `appointment-service` y **no se valida** |

```json
{
  "tipo": "CONSULTA",
  "motivo": "Control anual",
  "diagnostico": "Sano",
  "tratamiento": "Ninguno",
  "pesoKg": 28.5,
  "veterinarioId": 1
}
```

Versión mínima, con solo lo obligatorio:

```json
{ "tipo": "VACUNA", "motivo": "Vacuna antirrabica" }
```

**Respuesta `201`**

```json
{
  "id": 1,
  "fecha": "2026-11-06T14:30:00",
  "tipo": "CONSULTA",
  "motivo": "Control anual",
  "diagnostico": "Sano",
  "tratamiento": "Ninguno",
  "pesoKg": 28.5,
  "veterinarioId": 1
}
```

**Errores:** `400` cuerpo inválido (incluye `tipo` desconocido) · `404` la mascota no existe · `503`.

### 2.5 Consultar historial médico

`GET /mascotas/{mascotaId}/historial` → `200 OK`

Devuelve un arreglo con las entradas de la mascota, de la **más reciente a la más antigua**. Si la mascota existe pero no tiene entradas, devuelve `[]`.

**Respuesta `200`**

```json
[
  {
    "id": 2,
    "fecha": "2026-11-06T14:35:00",
    "tipo": "VACUNA",
    "motivo": "Vacuna antirrabica",
    "diagnostico": null,
    "tratamiento": null,
    "pesoKg": null,
    "veterinarioId": null
  },
  {
    "id": 1,
    "fecha": "2026-11-06T14:30:00",
    "tipo": "CONSULTA",
    "motivo": "Control anual",
    "diagnostico": "Sano",
    "tratamiento": "Ninguno",
    "pesoKg": 28.5,
    "veterinarioId": 1
  }
]
```

**Errores:** `400` si `mascotaId` no es numérico · `404` la mascota no existe · `503`.

---

## 3. appointment-service (8082)

Veterinarios y agenda de citas sin choques de horario (requisito 2). Base de datos: `vetsync_citas`.

### 3.1 Registrar veterinario

`POST /veterinarios` → `201 Created`

**Cuerpo**

| Campo | Tipo | Obligatorio | Reglas |
|---|---|---|---|
| `nombre` | string | Sí | No vacío, máximo 100 caracteres |
| `especialidad` | string | No | Si se indica, no vacía y máximo 60 caracteres |

```json
{ "nombre": "Dra. Gomez", "especialidad": "Cirugia" }
```

**Respuesta `201`**

```json
{ "id": 1, "nombre": "Dra. Gomez", "especialidad": "Cirugia" }
```

**Errores:** `400` cuerpo inválido · `503`.

### 3.2 Listar veterinarios

`GET /veterinarios` → `200 OK`

Devuelve todos los veterinarios, ordenados por nombre y, a igualdad de nombre, por id. Sin veterinarios devuelve `[]`.

```json
[
  { "id": 1, "nombre": "Dra. Gomez", "especialidad": "Cirugia" },
  { "id": 2, "nombre": "Dr. Ruiz", "especialidad": null }
]
```

**Errores:** `503`.

### 3.3 Agendar cita

`POST /citas` → `201 Created`

Agenda una cita para un veterinario y una mascota. Al agendar, el servicio consulta a `clinic-service` para comprobar que la mascota existe y copiar el nombre de la mascota y el contacto del propietario.

**Cuerpo (todos los campos son obligatorios)**

| Campo | Tipo | Reglas |
|---|---|---|
| `veterinarioId` | entero | Id de un veterinario registrado |
| `mascotaId` | entero | Id de una mascota registrada en `clinic-service` |
| `inicio` | fecha-hora | ISO-8601 sin zona, hora local de la clínica. Ver reglas de agenda abajo |
| `motivo` | string | No vacío, máximo 200 caracteres, sin caracteres de control |

```json
{ "veterinarioId": 1, "mascotaId": 1, "inicio": "2026-12-01T09:00:00", "motivo": "Control anual" }
```

> Ajusta `inicio` a una fecha **futura**: la API rechaza citas que no empiecen después de la hora actual.

**Reglas de la agenda.** Se evalúan **en este orden** y la primera que falla corta la petición:

| # | Regla | Si falla |
|---|---|---|
| 1 | `inicio` es posterior a la hora actual | `422` |
| 2 | `inicio` cae en una franja de 30 min alineada a `:00` o `:30`, sin segundos ni fracciones, y la franja termina a más tardar a la hora de cierre | `422` |
| 3 | El veterinario existe | `404` |
| 4 | La mascota existe en `clinic-service` | `404` |
| 5 | El veterinario no tiene otra cita `PROGRAMADA` con el mismo `inicio` | `409` |

Con el horario por defecto (08:00–18:00), el primer inicio válido de un día es `08:00:00` y el último es `17:30:00`. Una cita de `17:30` ocupa hasta las `18:00`.

La regla 5 también la impone la base de datos con el índice único filtrado `UX_cita_veterinario_inicio`, así que dos peticiones simultáneas tampoco pueden duplicar la franja.

**Respuesta `201`**

```json
{
  "id": 1,
  "veterinarioId": 1,
  "mascotaId": 1,
  "mascotaNombre": "Rex",
  "inicio": "2026-12-01T09:00:00",
  "motivo": "Control anual",
  "estado": "PROGRAMADA"
}
```

`estado` toma los valores `PROGRAMADA` o `CANCELADA`; toda cita nueva nace `PROGRAMADA`. La respuesta no incluye los datos de contacto del propietario.

**Errores:** `400` cuerpo inválido (campo obligatorio ausente, `motivo` vacío, demasiado largo o con saltos de línea, `inicio` mal formado) · `404` veterinario o mascota inexistente · `409` el veterinario ya tiene una cita en esa franja · `422` franja pasada, desalineada o fuera de horario · `503` base de datos o `clinic-service` no disponible.

> **Telemedicina.** La tabla `cita` tiene las columnas `modalidad` y `enlace_videollamada`, pero este endpoint **aún no las acepta ni las devuelve**. Si se envían, se ignoran, y la cita se guarda como `PRESENCIAL`.

### 3.4 Citas por recordar (uso interno)

`GET /citas/por-recordar?desde={fechaHora}&hasta={fechaHora}` → `200 OK`

Endpoint que consume `notification-service`. Devuelve las citas `PROGRAMADA` cuyo inicio cae en el rango, **con los extremos incluidos**, ordenadas por `inicio` y luego por id. Incluye el contacto del propietario, por eso es un endpoint pensado para uso entre servicios.

| Parámetro | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `desde` | fecha-hora ISO-8601 | Sí | Inicio del rango |
| `hasta` | fecha-hora ISO-8601 | Sí | Fin del rango. No puede ser anterior a `desde` |

El rango no puede superar **7 días**.

```
GET /citas/por-recordar?desde=2026-12-01T00:00:00&hasta=2026-12-02T00:00:00
```

**Respuesta `200`**

```json
[
  {
    "id": 1,
    "inicio": "2026-12-01T09:00:00",
    "mascotaNombre": "Rex",
    "contactoNombre": "Ana Perez",
    "contactoCorreo": "ana@correo.com",
    "motivo": "Control anual"
  }
]
```

Sin citas en el rango devuelve `[]`.

**Errores:** `400` falta un parámetro, no se puede leer como fecha-hora, `desde` es posterior a `hasta` o el rango supera 7 días · `503`.

---

## 4. notification-service (8083)

Recordatorios de cita por correo simulado (requisito 3). Base de datos: `vetsync_notificaciones`.

### 4.1 Cómo funcionan los recordatorios

No hay un endpoint para "enviar" recordatorios: el servicio los envía solo, en segundo plano.

1. Cada `vetsync.recordatorio.intervalo` (por defecto **60 s**) consulta a `appointment-service` las citas que empiezan entre *ahora* y *ahora + anticipación* (por defecto **24 h**). La primera revisión ocurre tras un intervalo completo desde el arranque.
2. Para cada cita que aún no tiene recordatorio, registra una fila en la tabla `notificacion` y luego "envía" el correo.
3. El correo es **simulado**: se escribe en el log del servicio con el formato `Correo simulado para <destinatario> | Asunto: … | …` y queda la fila en la base. No sale ningún correo real.
4. Hay a lo sumo **un recordatorio por cita**: la restricción única `(cita_id, tipo)` evita duplicados aunque se repita la revisión.
5. Si `appointment-service` o la base fallan durante una revisión, se registra el error y se reintenta en la siguiente, sin detener el proceso.

Contenido del mensaje:

- Asunto: `Recordatorio de cita: <nombre de la mascota>`
- Cuerpo: `Hola <nombre del contacto>, te recordamos que <mascota> tiene una cita el <dd/MM/yyyy HH:mm> (<motivo>).`

### 4.2 Listar recordatorios enviados

`GET /notificaciones?limite={n}` → `200 OK`

Es el "buzón" del correo simulado. Devuelve los recordatorios más recientes primero.

| Parámetro | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `limite` | entero | No | Cantidad máxima de resultados, de `1` a `500`. Por defecto `100` |

**Respuesta `200`**

```json
[
  {
    "id": 1,
    "citaId": 1,
    "destinatario": "ana@correo.com",
    "asunto": "Recordatorio de cita: Rex",
    "cuerpo": "Hola Ana Perez, te recordamos que Rex tiene una cita el 01/12/2026 09:00 (Control anual).",
    "enviadaEn": "2026-11-30T10:00:00"
  }
]
```

Mientras no se haya enviado nada, devuelve `[]`.

**Errores:** `400` si `limite` no es un entero o está fuera de `1`–`500` · `503`.

---

## 5. Formato de errores

Los tres servicios traducen todos los errores a `ProblemDetail` (RFC 7807) con `Content-Type: application/problem+json`. Los errores técnicos e inesperados se registran en el log del servicio y **nunca** devuelven trazas ni detalles internos.

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "El correo ana@correo.com ya esta registrado",
  "instance": "/propietarios"
}
```

### Códigos de respuesta

| Código | Significado | Cuándo ocurre |
|---|---|---|
| `400` Bad Request | Petición mal formada o inválida | Cuerpo ausente o con JSON inválido; campo obligatorio ausente; texto vacío, demasiado largo o con caracteres de control; valor de enum inválido; parámetro de ruta o de consulta ausente o con tipo incorrecto; rango de fechas o límite inválidos |
| `404` Not Found | El recurso no existe | Propietario, mascota o veterinario inexistente; ruta inexistente |
| `405` / `415` | Método o tipo de contenido no admitido | Método HTTP incorrecto; `POST` sin `Content-Type: application/json` |
| `409` Conflict | Choque con datos existentes | Correo de propietario repetido; veterinario con otra cita en la misma franja |
| `422` Unprocessable Entity | Regla de negocio incumplida | Franja pasada, desalineada o fuera del horario de atención |
| `503` Service Unavailable | Dependencia no disponible | Un adapter traduce un fallo técnico de persistencia o de la llamada a otro servicio (red, timeout, respuesta de error o incompleta). Detalle: `El servicio no esta disponible en este momento; intenta de nuevo mas tarde` |
| `500` Internal Server Error | Error inesperado | Cualquier otro fallo. Detalle: `Ocurrio un error inesperado` |

Para los `400` de cuerpo inválido, el campo `detail` lo fija Spring (por ejemplo `Failed to read request`) y no revela qué campo falló.

### Errores de negocio por servicio

| Servicio | Excepción de dominio | HTTP | Mensaje de ejemplo |
|---|---|---|---|
| clinic | `CorreoYaRegistradoException` | `409` | `El correo ana@correo.com ya esta registrado` |
| clinic | `PropietarioNoRegistradoException` | `404` | `El propietario 9999 no esta registrado` |
| clinic | `MascotaNoEncontradaException` | `404` | `La mascota 9999 no existe` |
| appointment | `FranjaHorariaInvalidaException` | `422` | `La cita debe empezar despues de la hora actual: 2020-01-01T09:00` |
| appointment | `VeterinarioNoEncontradoException` | `404` | `El veterinario 9999 no esta registrado` |
| appointment | `MascotaNoRegistradaException` | `404` | `La mascota 9999 no esta registrada` |
| appointment | `ConflictoDeHorarioException` | `409` | `El veterinario 1 ya tiene una cita en 2026-12-01T09:00` |
| appointment | `RangoDeFechasInvalidoException` | `400` | `El rango no puede superar 7 dias` |
| notification | `LimiteDeListadoInvalidoException` | `400` | `El limite debe estar entre 1 y 500` |

---

## 6. Contratos internos entre servicios

La comunicación es REST síncrona y en un solo sentido. Cada servicio declara su propia copia del DTO que consume; no se comparte código.

| Consumidor | Proveedor | Llamada | Para qué |
|---|---|---|---|
| `appointment-service` | `clinic-service` | `GET /mascotas/{id}` | Validar la mascota y copiar `mascota.nombre`, `propietario.nombre` y `propietario.correo` |
| `notification-service` | `appointment-service` | `GET /citas/por-recordar?desde&hasta` | Obtener las citas a recordar con su contacto |

Comportamiento ante fallos:

- `appointment-service` interpreta un `404` de `clinic-service` como "mascota no registrada" (`404` hacia su cliente). Cualquier otro error, un timeout o una respuesta incompleta se traduce a `503`.
- `notification-service` trata como error técnico cualquier fallo de `appointment-service`: lo registra y reintenta en la siguiente revisión.
- Timeouts de los clientes HTTP: conexión 2 s, lectura 5 s.

---

## 7. Modelo de datos

El esquema lo gestiona Flyway (`src/main/resources/db/migration/` de cada servicio). Hibernate solo lo valida (`ddl-auto: validate`). Las columnas son `snake_case` en SQL y `camelCase` en Java.

### vetsync_clinic

| Tabla | Columnas principales | Restricciones |
|---|---|---|
| `propietario` | `id`, `nombre` (100), `correo` (150), `telefono` (20, opcional), `creado_en` | `correo` único |
| `mascota` | `id`, `propietario_id`, `nombre` (80), `especie` (40), `raza` (60, opcional), `sexo` (`M`/`H`, opcional), `fecha_nacimiento` (opcional) | FK a `propietario`; `CHECK` sobre `sexo` |
| `registro_medico` | `id`, `mascota_id`, `fecha`, `tipo`, `motivo` (200), `diagnostico`, `tratamiento`, `peso_kg` `DECIMAL(5,2)`, `veterinario_id` | FK a `mascota`; `CHECK` sobre `tipo` y `peso_kg > 0`; índice `(mascota_id, fecha DESC)` |

### vetsync_citas

| Tabla | Columnas principales | Restricciones |
|---|---|---|
| `veterinario` | `id`, `nombre` (100), `especialidad` (60, opcional), `activo` | — |
| `cita` | `id`, `veterinario_id`, `mascota_id`, `mascota_nombre`, `contacto_nombre`, `contacto_correo`, `inicio`, `motivo` (200), `modalidad`, `enlace_videollamada`, `estado`, `creada_en` | FK a `veterinario`; `CHECK` de franja (minutos múltiplo de 30, segundos 0), de `modalidad` (`PRESENCIAL`/`TELEMEDICINA`) y de `estado` (`PROGRAMADA`/`CANCELADA`); **índice único filtrado `UX_cita_veterinario_inicio (veterinario_id, inicio) WHERE estado = 'PROGRAMADA'`** |

`mascota_id` referencia a `clinic-service` solo por id (sin FK entre bases). Los datos de la mascota y del contacto son una copia tomada al agendar.

### vetsync_notificaciones

| Tabla | Columnas principales | Restricciones |
|---|---|---|
| `notificacion` | `id`, `cita_id`, `tipo` (`RECORDATORIO_CITA`), `destinatario`, `asunto`, `cuerpo`, `enviada_en` | Única `(cita_id, tipo)`; índice `(enviada_en DESC, id DESC)` |

---

## 8. Lista de peticiones de prueba

Peticiones para probar los tres requisitos de punta a punta, en el orden en que deben ejecutarse. Sirven en Postman, Insomnia, el cliente HTTP de tu IDE o `curl`. Cada `POST` trae su **cuerpo JSON listo para copiar** (en Postman: `Body` → `raw` → `JSON`, con la cabecera `Content-Type: application/json`). Los `GET` no llevan cuerpo y se marcan como *sin cuerpo*.

**Antes de empezar**

- Los tres servicios y SQL Server deben estar en marcha (ver [README](../README.md#cómo-ejecutar-los-3-microservicios)).
- Con las bases recién creadas, los ids empiezan en `1`. Si los datos ya existían, **reemplaza el `1` de cada URL por el `id` que devolvió el paso anterior**.
- `inicio` de la cita debe ser futuro, en `:00` o `:30`, entre `08:00` y `17:30`. Ajusta la fecha de los ejemplos (`2026-12-01`) si ya pasó.

### 8.1 Flujo principal (camino feliz)

#### Clínica — puerto 8081

**P1. Registrar propietario** → `POST http://127.0.0.1:8081/propietarios` · esperado `201`

```json
{ "nombre": "Ana Perez", "correo": "ana@correo.com", "telefono": "555-1234" }
```

**P2. Registrar mascota** → `POST http://127.0.0.1:8081/propietarios/1/mascotas` · esperado `201` (el `1` es el id del propietario)

```json
{ "nombre": "Rex", "especie": "Perro", "raza": "Labrador", "sexo": "M", "fechaNacimiento": "2020-03-15" }
```

**P3. Consultar mascota** → `GET http://127.0.0.1:8081/mascotas/1` · sin cuerpo · esperado `200`, con el propietario anidado y sin teléfono

**P4. Añadir entrada al historial (completa)** → `POST http://127.0.0.1:8081/mascotas/1/historial` · esperado `201`

```json
{
  "tipo": "CONSULTA",
  "motivo": "Control anual",
  "diagnostico": "Sano",
  "tratamiento": "Ninguno",
  "pesoKg": 28.5,
  "veterinarioId": 1
}
```

**P5. Añadir entrada al historial (mínima)** → `POST http://127.0.0.1:8081/mascotas/1/historial` · esperado `201`

```json
{ "tipo": "VACUNA", "motivo": "Vacuna antirrabica" }
```

**P6. Consultar historial** → `GET http://127.0.0.1:8081/mascotas/1/historial` · sin cuerpo · esperado `200`, con 2 entradas: primero la vacuna (la más reciente)

#### Citas — puerto 8082

**P7. Registrar veterinario** → `POST http://127.0.0.1:8082/veterinarios` · esperado `201`

```json
{ "nombre": "Dra. Gomez", "especialidad": "Cirugia" }
```

**P8. Listar veterinarios** → `GET http://127.0.0.1:8082/veterinarios` · sin cuerpo · esperado `200`, con el veterinario del paso anterior

**P9. Agendar cita** → `POST http://127.0.0.1:8082/citas` · esperado `201`, `estado: "PROGRAMADA"`, `mascotaNombre: "Rex"`

```json
{ "veterinarioId": 1, "mascotaId": 1, "inicio": "2026-12-01T09:00:00", "motivo": "Control anual" }
```

**P10. Citas por recordar** → `GET http://127.0.0.1:8082/citas/por-recordar?desde=2026-12-01T00:00:00&hasta=2026-12-02T00:00:00` · sin cuerpo · esperado `200`, con la cita del paso anterior y el contacto de Ana Perez. El rango máximo es de 7 días.

#### Notificaciones — puerto 8083

**P11. Ver recordatorios enviados** → `GET http://127.0.0.1:8083/notificaciones` · sin cuerpo · esperado `200`

**P12. Ver recordatorios con límite** → `GET http://127.0.0.1:8083/notificaciones?limite=10` · sin cuerpo · esperado `200`

> **Para que aparezca un recordatorio** en P11, la cita de P9 debe empezar en **menos de 24 horas** (por ejemplo, hoy más tarde o mañana temprano) y debes esperar hasta 60 segundos a la siguiente revisión. Con una cita lejana, P11 devuelve `[]`: es lo esperado. Comprueba también el log de `notification-service`: debe aparecer una línea `Correo simulado para ana@correo.com …`. Pasadas más revisiones, P11 sigue mostrando **un solo** recordatorio por cita.

### 8.2 Casos de error

Cada petición trae su cuerpo completo para copiarlo tal cual, y el código con el que **debe** responder. Si responde otro código, ese caso falla.

**Antes de empezar:** ejecuta primero el flujo principal ([8.1](#81-flujo-principal-camino-feliz)), de modo que existan el propietario `1`, la mascota `1`, el veterinario `1` y la cita de **P9** (`2026-12-01T09:00:00`). Varios casos dependen de esos datos. Todos los `POST` llevan la cabecera `Content-Type: application/json`.

Las respuestas de error son `ProblemDetail`; la forma está en la [sección 5](#5-formato-de-errores).

#### clinic-service — peticiones con cuerpo

**E1. Correo repetido** → `POST http://127.0.0.1:8081/propietarios` · esperado `409` (requiere haber ejecutado P1)

```json
{ "nombre": "Ana Perez", "correo": "ana@correo.com", "telefono": "555-1234" }
```

**E2. Correo con formato inválido** → `POST http://127.0.0.1:8081/propietarios` · esperado `400`

```json
{ "nombre": "Ana Perez", "correo": "ana@correo" }
```

**E3. Falta el nombre (campo obligatorio)** → `POST http://127.0.0.1:8081/propietarios` · esperado `400`

```json
{ "correo": "nuevo@correo.com" }
```

**E4. Nombre vacío** → `POST http://127.0.0.1:8081/propietarios` · esperado `400`

```json
{ "nombre": "", "correo": "nuevo@correo.com" }
```

**E5. Teléfono de más de 20 caracteres** → `POST http://127.0.0.1:8081/propietarios` · esperado `400`

```json
{ "nombre": "Luis Mora", "correo": "luis@correo.com", "telefono": "1234567890123456789012345" }
```

**E6. JSON mal formado (llave sin cerrar)** → `POST http://127.0.0.1:8081/propietarios` · esperado `400`

```json
{ "nombre": "Ana Perez",
```

**E7. Propietario inexistente** → `POST http://127.0.0.1:8081/propietarios/9999/mascotas` · esperado `404`

```json
{ "nombre": "Rex", "especie": "Perro", "raza": "Labrador", "sexo": "M", "fechaNacimiento": "2020-03-15" }
```

**E8. Falta la especie (campo obligatorio)** → `POST http://127.0.0.1:8081/propietarios/1/mascotas` · esperado `400`

```json
{ "nombre": "Rex" }
```

**E9. Sexo fuera de `M` / `H`** → `POST http://127.0.0.1:8081/propietarios/1/mascotas` · esperado `400`

```json
{ "nombre": "Rex", "especie": "Perro", "sexo": "X" }
```

**E10. Fecha de nacimiento mal formada** → `POST http://127.0.0.1:8081/propietarios/1/mascotas` · esperado `400`

```json
{ "nombre": "Rex", "especie": "Perro", "fechaNacimiento": "15/03/2020" }
```

**E11. Tipo de entrada de historial inválido** → `POST http://127.0.0.1:8081/mascotas/1/historial` · esperado `400`

```json
{ "tipo": "OTRA", "motivo": "Control" }
```

**E12. Falta el motivo (campo obligatorio)** → `POST http://127.0.0.1:8081/mascotas/1/historial` · esperado `400`

```json
{ "tipo": "CONSULTA" }
```

**E13. Peso igual a cero** → `POST http://127.0.0.1:8081/mascotas/1/historial` · esperado `400`

```json
{ "tipo": "CONSULTA", "motivo": "Control", "pesoKg": 0 }
```

**E14. Peso por encima de 999.99 kg** → `POST http://127.0.0.1:8081/mascotas/1/historial` · esperado `400`

```json
{ "tipo": "CONSULTA", "motivo": "Control", "pesoKg": 1000 }
```

**E15. Veterinario con id no positivo** → `POST http://127.0.0.1:8081/mascotas/1/historial` · esperado `400`

```json
{ "tipo": "CONSULTA", "motivo": "Control", "veterinarioId": 0 }
```

**E16. Historial de una mascota inexistente** → `POST http://127.0.0.1:8081/mascotas/9999/historial` · esperado `404`

```json
{ "tipo": "CONSULTA", "motivo": "Control" }
```

#### clinic-service — peticiones sin cuerpo

| # | Caso | Petición | Esperado |
|---|---|---|---|
| E17 | Mascota inexistente | `GET http://127.0.0.1:8081/mascotas/9999` | `404` |
| E18 | Id de mascota no numérico | `GET http://127.0.0.1:8081/mascotas/abc` | `400` |
| E19 | Historial de una mascota inexistente | `GET http://127.0.0.1:8081/mascotas/9999/historial` | `404` |

#### appointment-service — peticiones con cuerpo

**E20. Veterinario sin nombre** → `POST http://127.0.0.1:8082/veterinarios` · esperado `400`

```json
{ "nombre": "" }
```

**E21. Falta el nombre del veterinario (campo obligatorio)** → `POST http://127.0.0.1:8082/veterinarios` · esperado `400`

```json
{ "especialidad": "Cirugia" }
```

**E22. Especialidad vacía** → `POST http://127.0.0.1:8082/veterinarios` · esperado `400`

```json
{ "nombre": "Dr. Ruiz", "especialidad": "" }
```

**E23. Cita repetida en la misma franja** → `POST http://127.0.0.1:8082/citas` · esperado `409` (requiere haber ejecutado P9)

```json
{ "veterinarioId": 1, "mascotaId": 1, "inicio": "2026-12-01T09:00:00", "motivo": "Control anual" }
```

**E24. Franja no alineada a `:00` / `:30`** → `POST http://127.0.0.1:8082/citas` · esperado `422`

```json
{ "veterinarioId": 1, "mascotaId": 1, "inicio": "2026-12-01T09:15:00", "motivo": "Control anual" }
```

**E25. Inicio con segundos** → `POST http://127.0.0.1:8082/citas` · esperado `422`

```json
{ "veterinarioId": 1, "mascotaId": 1, "inicio": "2026-12-01T09:30:30", "motivo": "Control anual" }
```

**E26. Antes de la apertura (08:00)** → `POST http://127.0.0.1:8082/citas` · esperado `422`

```json
{ "veterinarioId": 1, "mascotaId": 1, "inicio": "2026-12-01T07:30:00", "motivo": "Control anual" }
```

**E27. En la hora de cierre (18:00) o después** → `POST http://127.0.0.1:8082/citas` · esperado `422`

```json
{ "veterinarioId": 1, "mascotaId": 1, "inicio": "2026-12-01T18:00:00", "motivo": "Control anual" }
```

**E28. Fecha pasada** → `POST http://127.0.0.1:8082/citas` · esperado `422`

```json
{ "veterinarioId": 1, "mascotaId": 1, "inicio": "2020-01-01T09:00:00", "motivo": "Control anual" }
```

**E29. Veterinario inexistente** → `POST http://127.0.0.1:8082/citas` · esperado `404`

```json
{ "veterinarioId": 9999, "mascotaId": 1, "inicio": "2026-12-01T11:00:00", "motivo": "Control anual" }
```

**E30. Mascota inexistente (con veterinario válido)** → `POST http://127.0.0.1:8082/citas` · esperado `404`

```json
{ "veterinarioId": 1, "mascotaId": 9999, "inicio": "2026-12-01T11:00:00", "motivo": "Control anual" }
```

**E31. Falta `mascotaId` (campo obligatorio)** → `POST http://127.0.0.1:8082/citas` · esperado `400`

```json
{ "veterinarioId": 1, "inicio": "2026-12-01T11:00:00", "motivo": "Control anual" }
```

**E32. Falta `motivo` (campo obligatorio)** → `POST http://127.0.0.1:8082/citas` · esperado `400`

```json
{ "veterinarioId": 1, "mascotaId": 1, "inicio": "2026-12-01T11:00:00" }
```

**E33. Motivo vacío** → `POST http://127.0.0.1:8082/citas` · esperado `400`

```json
{ "veterinarioId": 1, "mascotaId": 1, "inicio": "2026-12-01T11:00:00", "motivo": "" }
```

**E34. Motivo con salto de línea** → `POST http://127.0.0.1:8082/citas` · esperado `400`

```json
{ "veterinarioId": 1, "mascotaId": 1, "inicio": "2026-12-01T11:00:00", "motivo": "Control\nanual" }
```

**E35. Motivo de más de 200 caracteres (209)** → `POST http://127.0.0.1:8082/citas` · esperado `400`

```json
{ "veterinarioId": 1, "mascotaId": 1, "inicio": "2026-12-01T11:00:00", "motivo": "Control anual Control anual Control anual Control anual Control anual Control anual Control anual Control anual Control anual Control anual Control anual Control anual Control anual Control anual Control anual" }
```

**E36. `inicio` con formato inválido** → `POST http://127.0.0.1:8082/citas` · esperado `400`

```json
{ "veterinarioId": 1, "mascotaId": 1, "inicio": "01/12/2026 11:00", "motivo": "Control anual" }
```

#### appointment-service — peticiones sin cuerpo

| # | Caso | Petición | Esperado |
|---|---|---|---|
| E37 | Rango de más de 7 días | `GET http://127.0.0.1:8082/citas/por-recordar?desde=2026-12-01T00:00:00&hasta=2026-12-15T00:00:00` | `400` |
| E38 | Rango invertido (`desde` posterior a `hasta`) | `GET http://127.0.0.1:8082/citas/por-recordar?desde=2026-12-02T00:00:00&hasta=2026-12-01T00:00:00` | `400` |
| E39 | Falta el parámetro `hasta` | `GET http://127.0.0.1:8082/citas/por-recordar?desde=2026-12-01T00:00:00` | `400` |
| E40 | Fecha con formato inválido | `GET http://127.0.0.1:8082/citas/por-recordar?desde=ayer&hasta=hoy` | `400` |

#### notification-service — peticiones sin cuerpo

| # | Caso | Petición | Esperado |
|---|---|---|---|
| E41 | Límite cero | `GET http://127.0.0.1:8083/notificaciones?limite=0` | `400` |
| E42 | Límite por encima del máximo (500) | `GET http://127.0.0.1:8083/notificaciones?limite=501` | `400` |
| E43 | Límite no numérico | `GET http://127.0.0.1:8083/notificaciones?limite=abc` | `400` |

### 8.3 Casos límite válidos y otros comportamientos

Estas peticiones **deben funcionar**. Son los extremos de las reglas anteriores y sirven para comprobar que no se rechaza de más. Ejecútalas después de 8.1; cada una ocupa una franja distinta de la de P9, así que no chocan entre sí.

**V1. Primera franja del día (08:00)** → `POST http://127.0.0.1:8082/citas` · esperado `201`

```json
{ "veterinarioId": 1, "mascotaId": 1, "inicio": "2026-12-01T08:00:00", "motivo": "Primera cita del dia" }
```

**V2. Última franja del día (17:30, termina a las 18:00)** → `POST http://127.0.0.1:8082/citas` · esperado `201`

```json
{ "veterinarioId": 1, "mascotaId": 1, "inicio": "2026-12-01T17:30:00", "motivo": "Ultima cita del dia" }
```

**V3. Mismo horario con otro veterinario** → primero registra un segundo veterinario con `POST http://127.0.0.1:8082/veterinarios` (esperado `201`):

```json
{ "nombre": "Dr. Ruiz", "especialidad": "Dermatologia" }
```

Luego agenda en la franja de P9 con el id que devolvió (aquí `2`) → `POST http://127.0.0.1:8082/citas` · esperado `201`, porque el choque se evalúa por veterinario:

```json
{ "veterinarioId": 2, "mascotaId": 1, "inicio": "2026-12-01T09:00:00", "motivo": "Control anual" }
```

**V4. Campos de telemedicina (aún no soportados)** → `POST http://127.0.0.1:8082/citas` · esperado `201`. Los campos `modalidad` y `enlaceVideollamada` se ignoran: la respuesta no los incluye y la cita queda como `PRESENCIAL`.

```json
{
  "veterinarioId": 1,
  "mascotaId": 1,
  "inicio": "2026-12-01T10:00:00",
  "motivo": "Consulta por videollamada",
  "modalidad": "TELEMEDICINA",
  "enlaceVideollamada": "https://meet.ejemplo.com/vetsync"
}
```

**V5. Límite máximo permitido** → `GET http://127.0.0.1:8083/notificaciones?limite=500` · sin cuerpo · esperado `200`

**V6. Historial con todos los campos en sus límites** → `POST http://127.0.0.1:8081/mascotas/1/historial` · esperado `201`

```json
{ "tipo": "CIRUGIA", "motivo": "Esterilizacion", "diagnostico": "Sin complicaciones", "tratamiento": "Reposo 7 dias", "pesoKg": 999.99, "veterinarioId": 1 }
```

**V7. Mascota sin datos opcionales** → `POST http://127.0.0.1:8081/propietarios/1/mascotas` · esperado `201`, con `raza`, `sexo` y `fechaNacimiento` en `null`

```json
{ "nombre": "Michi", "especie": "Gato" }
```

**V8. Propietario sin teléfono** → `POST http://127.0.0.1:8081/propietarios` · esperado `201`, con `telefono` en `null`

```json
{ "nombre": "Luis Mora", "correo": "luis@correo.com" }
```

### 8.4 Dependencias caídas

| Caso | Cómo provocarlo | Esperado |
|---|---|---|
| `clinic-service` no disponible al agendar | Detén `clinic-service` (`Ctrl+C`) y envía `POST http://127.0.0.1:8082/citas` con el cuerpo de abajo (franja libre) | `503` |
| `appointment-service` no disponible para los recordatorios | Detén `appointment-service` y espera una revisión (60 s) | `notification-service` registra el error en su log y reintenta en la siguiente revisión; `GET /notificaciones` sigue respondiendo `200` |

Cuerpo para el primer caso:

```json
{ "veterinarioId": 1, "mascotaId": 1, "inicio": "2026-12-01T14:00:00", "motivo": "Control anual" }
```

Vuelve a arrancar el servicio detenido al terminar la prueba.

### 8.5 Ejemplos con `curl`

Git Bash, Linux o macOS:

```bash
# P1. Registrar propietario
curl -i -X POST http://127.0.0.1:8081/propietarios \
  -H "Content-Type: application/json" \
  -d '{"nombre": "Ana Perez", "correo": "ana@correo.com", "telefono": "555-1234"}'

# P3. Consultar mascota
curl -i http://127.0.0.1:8081/mascotas/1

# P9. Agendar cita
curl -i -X POST http://127.0.0.1:8082/citas \
  -H "Content-Type: application/json" \
  -d '{"veterinarioId": 1, "mascotaId": 1, "inicio": "2026-12-01T09:00:00", "motivo": "Control anual"}'

# P11. Ver recordatorios enviados
curl -i http://127.0.0.1:8083/notificaciones
```

PowerShell:

```powershell
# P1. Registrar propietario
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:8081/propietarios `
  -ContentType "application/json" `
  -Body '{"nombre": "Ana Perez", "correo": "ana@correo.com", "telefono": "555-1234"}'

# P3. Consultar mascota
Invoke-RestMethod -Uri http://127.0.0.1:8081/mascotas/1

# P9. Agendar cita
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:8082/citas `
  -ContentType "application/json" `
  -Body '{"veterinarioId": 1, "mascotaId": 1, "inicio": "2026-12-01T09:00:00", "motivo": "Control anual"}'

# P11. Ver recordatorios enviados
Invoke-RestMethod -Uri http://127.0.0.1:8083/notificaciones
```

`Invoke-RestMethod` lanza una excepción en los códigos de error; para ver el código y el cuerpo de un `409` o `422`, usa `curl.exe -i` en lugar de `Invoke-RestMethod`.
