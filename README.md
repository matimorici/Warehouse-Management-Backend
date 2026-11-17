# Warehouse Management Backend (WMS)

Backend REST API para gestionar productos, proveedores, stock, ubicaciones y órdenes de un almacén.

## Tecnologías

- **Java 25** + **Spring Boot 4.1.0**
- **PostgreSQL** como base de datos
- **Spring Data JPA** para el acceso a datos
- **Flyway** para las migraciones del schema
- **Spring Security** para autenticación y autorización
- **Lombok** para reducir boilerplate
- **ZXing 3.5.3** para generar códigos de barras
- **Maven** con wrapper incluido (`./mvnw`)
- **Angular** como frontend separado (`localhost:4200`)

## Requisitos previos

- **Java 25**.
- **PostgreSQL** corriendo en `localhost:5432`.
- **Maven** no hace falta instalarlo: el repo incluye `./mvnw`.
- **Angular CLI** solo si vas a trabajar con el frontend.

## Puesta en marcha

### 1. Crear la base de datos

Creá una base PostgreSQL llamada `wms_db`. Las tablas las crea Flyway al arrancar:

```sql
CREATE DATABASE wms_db;
```

### 2. Configurar credenciales locales

Desde la raíz del repo, copiá el archivo de ejemplo:

```bash
cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
```

Completá `DB_USERNAME` y `DB_PASSWORD` con tus credenciales de PostgreSQL. El archivo está en `.gitignore` y no se sube.

Para desarrollo local por HTTP, el ejemplo ya trae `server.servlet.session.cookie.secure=false`. Esa línea es solo para local: en producción la cookie debe seguir siendo `Secure`.

### 3. Ejecutar con el perfil `local`

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

La API queda en **http://localhost:8080**. En IntelliJ, poné `local` en **Active profiles** de la configuración de `WmsApplication`.

### Build y tests

```bash
./mvnw clean compile
./mvnw test
./mvnw package
```

`./mvnw test` incluye tests que usan PostgreSQL local (no hay perfil H2).

## Usuario administrador por defecto

La migración `V3` crea el usuario `20-00000000-1` con contraseña `Admin1234` y rol `ADMINISTRADOR`. Se puede usar con `POST /api/auth/login`.

## Migraciones

Flyway corre automáticamente al arrancar, antes que Hibernate, y aplica los scripts de `src/main/resources/db/migration/`.
Las migraciones ya aplicadas son inmutables; un cambio nuevo va en `V<n+1>__descripcion.sql`.
No hay rollback automático: si una migración sale mal, se escribe una compensatoria `V<n+1>`.
`ddl-auto=validate` queda como red de seguridad: Hibernate solo verifica que las entidades coincidan con el schema.

## Autenticación y autorización

- Login por sesión (`HttpSession`), no JWT. La cookie `JSESSIONID` expira a los 90 minutos de inactividad.
- CSRF: el backend emite la cookie `XSRF-TOKEN`; en `POST`/`PUT`/`DELETE` (incluido login) hay que enviar el header `X-XSRF-TOKEN`. En Angular usá `withCredentials: true`; con URL absoluta, el interceptor XSRF puede no aplicar, así que usá un proxy o un interceptor propio.

| Ruta | Acceso |
|------|--------|
| `POST /api/auth/login` | Público |
| `/api/usuarios/**` | Solo `ADMINISTRADOR` |
| `GET /api/proveedores/**` | `OPERARIO` o `ADMINISTRADOR` |
| `POST`/`PUT`/`DELETE /api/proveedores/**` | Solo `ADMINISTRADOR` |
| Productos, órdenes de retiro y compra, ubicaciones, valoraciones y movimientos físicos | `OPERARIO` o `ADMINISTRADOR` |
| `/api/reportes/**` | Solo `ADMINISTRADOR` |
| Cualquier otra ruta | Autenticado |

Sin sesión o sin el rol requerido, la respuesta es `403`.

Tras 5 intentos fallidos para un mismo CUIL, el login se bloquea 15 minutos y responde `429`; un login exitoso reinicia el contador.
Los eventos de seguridad (login, logout, bloqueos y accesos denegados) se registran con CUIL e IP, sin contraseñas ni tokens.

## API REST

Todas las rutas están bajo `/api/`. CORS es global: permite el origen de `app.cors.allowed-origin` (`http://localhost:4200` por defecto) con credenciales.

| Método | Ruta | Descripción |
|--------|------|-------------|
| `POST` | `/api/auth/login` | Inicia sesión |
| `POST` | `/api/auth/logout` | Cierra la sesión |
| `POST` | `/api/usuarios` | Crea un usuario |
| `GET` | `/api/usuarios[/{id}]` | Lista usuarios activos o busca uno por ID |
| `DELETE` | `/api/usuarios/{id}` | Baja lógica del usuario |
| `POST` | `/api/productos` | Crea un producto y su stock |
| `GET` | `/api/productos[/{id}]` | Lista productos con stock o busca uno por ID |
| `PUT` | `/api/productos/{id}` | Actualiza un producto |
| `DELETE` | `/api/productos/{id}` | Baja lógica (solo sin stock disponible) |
| `GET` | `/api/productos/{id}/stock` | Consulta el stock |
| `PUT` | `/api/productos/{id}/stock` | Actualiza el stock |
| `GET` | `/api/productos/{id}/barcode` | Devuelve el código de barras como PNG |
| `POST` | `/api/proveedores` | Crea un proveedor |
| `GET` | `/api/proveedores[/{id}]` | Lista proveedores activos o busca uno por ID |
| `PUT` | `/api/proveedores/{id}` | Actualiza un proveedor |
| `DELETE` | `/api/proveedores/{id}` | Baja lógica (sin productos activos ni órdenes pendientes) |
| `POST` | `/api/ubicaciones` | Crea una ubicación |
| `GET` | `/api/ubicaciones[/{id}]` | Lista ubicaciones activas o busca una por ID |
| `PUT` | `/api/ubicaciones/{id}` | Actualiza una ubicación |
| `DELETE` | `/api/ubicaciones/{id}` | Baja lógica (sin productos activos en la ubicación) |
| `POST` | `/api/valoraciones-proveedor` | Crea una valoración |
| `GET` | `/api/valoraciones-proveedor[/{id}]` | Lista valoraciones (acepta `?idSupplier=`) o busca una por ID |
| `PUT` | `/api/valoraciones-proveedor/{id}` | Actualiza una valoración (no cambia el proveedor) |
| `DELETE` | `/api/valoraciones-proveedor/{id}` | Elimina una valoración |
| `POST` | `/api/ordenes-retiro` | Crea una orden de retiro |
| `GET` | `/api/ordenes-retiro[/{id}]` | Lista órdenes (resumen) o busca una con líneas |
| `PUT` | `/api/ordenes-retiro/{id}` | Actualiza una orden de retiro |
| `DELETE` | `/api/ordenes-retiro/{id}` | Baja lógica y revierte stock |
| `POST` | `/api/ordenes-compra` | Crea una orden de compra |
| `GET` | `/api/ordenes-compra[/{id}]` | Lista órdenes (resumen) o busca una con líneas |
| `PUT` | `/api/ordenes-compra/{id}` | Actualiza una orden de compra |
| `PUT` | `/api/ordenes-compra/{id}/recibir` | Marca la orden como recibida y suma stock |
| `DELETE` | `/api/ordenes-compra/{id}` | Baja lógica y revierte stock si fue recibida |
| `POST` | `/api/movimientos-fisicos` | Registra un movimiento físico (append-only) |
| `GET` | `/api/movimientos-fisicos` | Lista movimientos (append-only); filtros `?idProduct=`, `?idUser=`, `?from=&to=` |
| `GET` | `/api/reportes/**` | Reportes de solo lectura (detalle en `AGENTS.md`) |

- Si `codigoBarras` se omite o va vacío al crear un producto, se genera automáticamente con formato `INT-XXXXXX`.
- Crear o actualizar una orden de retiro resta `cantidadDisponible` y suma `cantidadPendiente`; al darla de baja se revierte.
- Recibir una orden de compra suma `cantidadDisponible`; si ya fue recibida, actualizarla revierte y reaplica el stock, y al darla de baja se revierte.
- Los `DELETE` de usuarios, productos, proveedores, ubicaciones y órdenes son bajas lógicas.
- Los errores de validación devuelven `400` con mensajes en español.

## Documentación adicional

- `AGENTS.md`: referencia técnica detallada (entidades, reglas de negocio, tests, seguridad y convenciones).
- `TODO.md`: backlog y pendientes del proyecto.
