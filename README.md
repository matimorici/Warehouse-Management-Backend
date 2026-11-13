# Warehouse Management Backend (WMS)

Backend REST API para la gestión de almacén. Sistema diseñado para administrar productos, proveedores, stock y órdenes de retiro.

## Tecnologías

- **Java 25** + **Spring Boot 4.1.0**
- **PostgreSQL** (base de datos)
- **Spring Data JPA** (acceso a datos)
- **Flyway** (migraciones del schema de base de datos)
- **Spring Security** (autenticación — en fase preliminar)
- **Lombok** (reducción de boilerplate)
- **ZXing 3.5.3** (generación de códigos de barras)
- **Maven** (build tool, con wrapper incluido)
- **Angular** (frontend, separado — `localhost:4200`)

## Requisitos previos

- **Java 25** o superior
- **Maven** (o usar el wrapper `./mvnw` incluido en el proyecto)
- **PostgreSQL** corriendo en `localhost:5432`
- **Angular CLI** (para el frontend, si trabajás en eso)

## Instalación

### 1. Clonar el repositorio

```bash
git clone <url-del-repo>
cd Warehouse-Management-Backend
```

### 2. Configurar la base de datos

El sistema necesita una base de datos PostgreSQL llamada `wms_db`. Solo creala (las tablas las crea Flyway automáticamente):

```sql
CREATE DATABASE wms_db;
```

Eso es todo. Al arrancar la aplicación, **Flyway aplica las migraciones del schema** (`db/migration/`) automáticamente y crea todas las tablas, la secuencia `codigo_interno_seq`, los índices y las constraints. Ver [Migraciones de base de datos](#migraciones-de-base-de-datos) más abajo para entender el porqué y el cómo.

### 3. Configurar credenciales

Las credenciales de conexión a PostgreSQL **no están hardcodeadas** en el repositorio — `application.properties` usa placeholders (`${DB_USERNAME}`, `${DB_PASSWORD}`) que Spring Boot resuelve contra un perfil local, para que el repo se pueda compartir sin exponer contraseñas.

**Paso 1 — Crear tu archivo de credenciales local**

Copiá el archivo de ejemplo y renombralo:

```bash
# Git Bash / macOS / Linux
cp wms/src/main/resources/application-local.properties.example wms/src/main/resources/application-local.properties
```

```cmd
:: CMD de Windows
copy wms\src\main\resources\application-local.properties.example wms\src\main\resources\application-local.properties
```

Editá `application-local.properties` con las credenciales reales de tu PostgreSQL local:

```properties
DB_USERNAME=postgres
DB_PASSWORD=tu_contraseña
server.servlet.session.cookie.secure=false
```

> La cookie de sesión es `Secure` por defecto (solo viaja por HTTPS). La línea `server.servlet.session.cookie.secure=false` es solo para desarrollo local por HTTP. **No la pongas en producción.**

> Este archivo está en `.gitignore` — nunca se sube al repositorio. Cada integrante del equipo crea el suyo con sus propias credenciales locales.

**Paso 2 — Activar el perfil `local`**

Sin esto, Spring Boot no va a leer `application-local.properties` y falla al arrancar (`Could not resolve placeholder 'DB_USERNAME'`).

- **Desde IntelliJ:** `Run → Edit Configurations` → seleccionar la configuración de `WmsApplication` → campo **Active profiles** → escribir `local`.
- **Desde la terminal (Bash o CMD, el comando es el mismo):**
```bash
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```
### 4. Ejecutar

Desde la carpeta `wms/`, con el perfil `local` activado (ver paso anterior):

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

O simplemente `./mvnw spring-boot:run` si ya activaste el perfil `local` en tu Run Configuration de IntelliJ.

La aplicación arranca en **http://localhost:8080**.

### Build completo

```bash
./mvnw clean compile    # Compilar
./mvnw package          # Generar JAR
./mvnw test             # Ejecutar tests
```

## Migraciones de base de datos

El schema no se mantiene a mano: lo gobierna **Flyway**, una herramienta de migraciones versionadas. Cada cambio estructural es un script SQL numerado que se aplica **una sola vez, en orden**, y queda registrado en una tabla de control. Todo es automático al arrancar la app.

### Qué es y cómo funciona

- Las migraciones viven en `wms/src/main/resources/db/migration/`, nombradas como `V<n>__<descripcion>.sql` (ej. `V1__create_schema.sql`, `V2__agregar_columna_x.sql`).
- Al arrancar, Spring Boot ejecuta Flyway **antes que Hibernate**: compara el número de versión más alto ya aplicado contra los scripts disponibles y aplica los que faltan, en orden de `V1` en adelante.
- Cada migración corre dentro de una transacción: si falla a mitad de camino, se revierte y el arranque aborta (no se deja la base a medias).
- Una vez aplicado, un script **jamás se vuelve a ejecutar**, aunque el archivo se edite. Eso significa que los scripts ya aplicados son **inmutables**: para cambiar algo, se escribe una migración nueva (`V2`, `V3`, ...) que agregue/altere, nunca se toca la anterior.
- Flyway lleva la cuenta en la tabla `flyway_schema_history` de la propia base.
- **Flyway Community no tiene rollback automático** (down-migrations). Si una migración nueva sale mal, no se "deshace": se escribe una migración compensatoria `V<n+1>` que revierta el cambio. Conviene probar cada migración en una base de desarrollo antes de mergearla cerca de una entrega.

### Migraciones actuales

| Versión | Archivo | Qué hace |
|---------|---------|----------|
| V1 | `V1__create_schema.sql` | Crea todo el schema (tablas, índices, constraints, `codigo_interno_seq`). Incluye tablas del dominio que todavía no tienen entidad JPA. |
| V2 | `V2__ensure_codigo_interno_seq.sql` | Reparación: garantiza que `codigo_interno_seq` exista en bases viejas baselineadas como V1 (armadas antes de que la secuencia existiera). No-op en bases nuevas. |
| V3 | `V3__seed_admin_user.sql` | Inserta el usuario administrador por defecto. |
| V4 | `V4__agregar_fecha_baja_usuario.sql` | Agrega `usuario.fecha_baja` (NULL = activo) y el índice parcial `idx_usuario_activos`. Habilita la baja lógica sin perder trazabilidad. |
| V5 | `V5__soft_delete_and_traceability.sql` | Agrega `fecha_baja` e `id_usuario_baja` (más el `CHECK` de coherencia) a `usuario`, `producto`, `proveedor`, `ubicacion`, `orden_compra` y `orden_retiro`. En `usuario` reutiliza el `fecha_baja` de V4. Agrega `orden_compra.fecha_recepcion` (sin backfill: las órdenes recibidas históricas quedan en NULL). |

> **Usuario administrador por defecto (seed V3):** CUIL `20-00000000-1`, contraseña `Admin1234`, rol `ADMINISTRADOR`. Existe en todas las bases (nuevas o baselineadas) tras arrancar la app. Se puede loguear con `POST /api/auth/login` o desactivar/modificar si se prefiere.

#### Verificar la reparación V2 (importante, una vez por equipo)

V2 solo tiene lógica real contra una base *vieja* (sin `codigo_interno_seq`); en bases nuevas es no-op y queda sin probar. Antes de dar por cerrado el desfase del baseline, **alguien del equipo debe correr** (con PostgreSQL local):

```bash
bash scripts/verify-v2-repair.sh
```

El script crea un scratch DB (`wms_db_v2_test`), le arma el schema viejo (V1 sin la secuencia), arranca la app contra él y verifica que Flyway haga baseline V1 y aplique V2 (secuencia creada) + V3 (admin seed). **No toca `wms_db`.** Detalles en el encabezado del propio script.

### Por qué (motivación)

Antes, el schema se creaba con `sql/wms_schema.sql`, un script que **borraba todas las tablas y las recreaba** (`DROP TABLE ... CASCADE`). Eso funcionaba en desarrollo pero tenía problemas graves:

1. **No se podía evolucionar sin perder datos.** Cualquier cambio de schema (agregar una columna, una tabla, una secuencia) exigía volver a correr el script completo, que vaciaba la base.
2. **Cambios "manuales" por máquina.** Cuando se agregó `codigo_interno_seq`, cada base existente tuvo que recibir un `CREATE SEQUENCE` a mano. Dos máquinas de dos integrantes del grupo podían terminar con schemas distintos sin que nadie se diera cuenta.
3. **Sin historial ni trazabilidad.** No había forma de saber en qué versión del schema estaba cada base, ni qué cambio se aplicó cuándo.

Las migraciones resuelven todo eso: cada base arranca desde cero (`V1`) y llega al estado actual aplicando una serie de deltas idempotentes y versionados. Cualquier miembro del equipo —o la futura producción— llega al mismo schema con solo `./mvnw spring-boot:run`.

### Cómo se trabaja con ellas (reglas prácticas)

- **Base nueva:** solo `CREATE DATABASE wms_db;` y a arrancar. Flyway crea todo.
- **Base existente (creada con el viejo script):** no hay que hacer nada manual. `spring.flyway.baseline-on-migrate=true` marca la base con su estado actual como "baseline V1" en el primer arranque, sin borrar datos; de ahí en más, las migraciones nuevas se aplican normalmente. **Única excepción:** si tu base es tan vieja que no tiene `codigo_interno_seq` (se agregó al script en una revisión posterior), la migración V2 la crea automáticamente. Bases anteriores a la migración de PKs UUID→BIGINT no son compatibles: recreala desde cero.
- **Agregar un cambio estructural:** se crea `db/migration/V<n+1>__descripcion.sql` con el SQL del cambio (ej. `ALTER TABLE ...`, `CREATE TABLE ...`, `CREATE SEQUENCE ...`). No editar migraciones ya aplicadas. Al siguiente arranque se aplica solo.
- **Datos de referencia** (ej. un usuario admin inicial): se insertan también en una migración, así existen en todas las bases.
- **`ddl-auto=validate`:** se mantiene como red de seguridad. Flyway es dueño del schema y lo crea/actualiza; Hibernate solo verifica que las entidades coincidan. Si una entidad y una migración se desalinean, la app no arranca y avisa, en vez de corromper la base.
- **El script viejo** `wms_schema.sql` se conserva en `db/wms_schema.sql` solo como referencia histórica de la versión original; no se ejecuta más.

## Estructura del proyecto

```
wms/src/main/java/big_three/wms/
├── WmsApplication.java              # Punto de entrada
├── config/
│   └── SecurityConfig.java          # Configuración de seguridad
├── controller/
│   ├── AuthController.java          # Login
│   ├── LocationController.java      # CRUD ubicaciones
│   ├── PickOrderController.java     # CRUD órdenes de retiro
│   ├── ProductController.java       # CRUD productos + stock
│   ├── ProveedorController.java     # CRUD proveedores
│   ├── PurchaseOrderController.java # CRUD órdenes de compra + recibir
│   ├── ReportController.java        # Reportes de solo lectura (ADMINISTRADOR)
│   ├── SupplierRatingController.java # CRUD valoraciones de proveedor
│   └── UserController.java          # CRUD usuarios
├── dto/
│   ├── LocationCreateDTO.java
│   ├── LocationResponseDTO.java
│   ├── LoginRequestDTO.java
│   ├── PickOrderCreateDTO.java
│   ├── PickOrderLineCreateDTO.java
│   ├── PickOrderLineResponseDTO.java
│   ├── PickOrderResponseDTO.java
│   ├── ProductCreateDTO.java
│   ├── ProductResponseDTO.java
│   ├── ProveedorCreateDTO.java
│   ├── ProveedorResponseDTO.java
│   ├── PurchaseOrderCreateDTO.java
│   ├── PurchaseOrderLineCreateDTO.java
│   ├── PurchaseOrderLineResponseDTO.java
│   ├── PurchaseOrderResponseDTO.java
│   ├── StockResponseDTO.java
│   ├── StockUpdateDTO.java
│   ├── SupplierRatingCreateDTO.java
│   ├── SupplierRatingResponseDTO.java
│   ├── UserCreateDTO.java
│   ├── UserResponseDTO.java
│   └── report/
│       ├── ActividadOperarioDTO.java
│       ├── ProductoMasRetiradoDTO.java
│       ├── ProductoSinRotacionDTO.java
│       ├── ProductoUbicacionActualDTO.java
│       ├── RetiroPorPeriodoDTO.java
│       ├── StockBajoDTO.java
│       ├── StockPorProveedorDTO.java
│       └── UbicacionOcupacionDTO.java
├── exception/
│   └── InvalidCredentialsException.java
├── model/
│   ├── Location.java               # Ubicación
│   ├── PickOrder.java               # Orden de retiro
│   ├── PickOrderLine.java           # Línea de orden de retiro (composite PK)
│   ├── Product.java                 # Producto (enum OrigenCodigoBarras)
│   ├── Proveedor.java               # Proveedor
│   ├── PurchaseOrder.java           # Orden de compra (enum Status)
│   ├── PurchaseOrderLine.java       # Línea de orden de compra (composite PK)
│   ├── Stock.java                   # Stock (1:1 con Product)
│   ├── SupplierRating.java          # Valoración de proveedor
│   └── User.java                    # Usuario
├── repository/
│   ├── LocationRepository.java
│   ├── PickOrderLineRepository.java
│   ├── PickOrderRepository.java
│   ├── ProductRepository.java
│   ├── ProveedorRepository.java
│   ├── PurchaseOrderLineRepository.java
│   ├── PurchaseOrderRepository.java
│   ├── ReportRepository.java
│   ├── StockRepository.java
│   ├── SupplierRatingRepository.java
│   └── UserRepository.java
└── service/
    ├── LocationService.java
    ├── PickOrderService.java
    ├── ProductService.java
    ├── ProveedorService.java
    ├── PurchaseOrderService.java
    ├── ReportPeriod.java
    ├── ReportService.java
    ├── SupplierRatingService.java
    └── UserService.java
```

## Entidades

| Entidad | Tabla | Descripción |
|---------|-------|-------------|
| **Location** | `ubicacion` | Ubicaciones del almacén. Solo tiene nombre (`name` → `nombre_ubicacion`). |
| **User** | `usuario` | Usuarios del sistema (nombre, apellido, CUIL, rol, contraseña hasheada con BCrypt). Rol por defecto: `OPERARIO`. |
| **Product** | `producto` | Productos del almacén. Tiene código de barras (interno o de fábrica), descripción y relación con un proveedor. |
| **Proveedor** | `proveedor` | Proveedores de productos (CUIT, razón social, teléfono, mail, dirección). |
| **Stock** | `stock` | Stock de cada producto. Relación 1:1 con Product (comparten PK). Tiene cantidad disponible y cantidad pendiente. |
| **PickOrder** | `orden_retiro` | Orden de retiro de productos. Asociada a un usuario por `id_usuario` y compuesta por una o más líneas. |
| **PickOrderLine** | `linea_retiro` | Línea de una orden de retiro. PK compuesta: `id_orden_retiro` + `id_producto`. Cantidad a retirar. |
| **PurchaseOrder** | `orden_compra` | Orden de compra a un proveedor. Tiene estado (`PENDIENTE`/`RECIBIDA`/`CANCELADA`), fecha/hora y una o más líneas. |
| **PurchaseOrderLine** | `linea_compra` | Línea de una orden de compra. PK compuesta: `id_orden_compra` + `id_producto`. Cantidad. |
| **SupplierRating** | `valoracion_proveedor` | Valoración de un proveedor (tiempo de entrega, forma de entrega, relación precio/calidad). Asociada a un proveedor por `id_supplier` (raw Long). |

## API REST

Todas las rutas están bajo el prefijo `/api/`. CORS se configura de forma global en `SecurityConfig`: permite el origen definido en la propiedad `app.cors.allowed-origin` (`http://localhost:4200` por defecto, para Angular) e incluye credenciales, necesarias para la sesión y el token CSRF. Para un deploy, cambiá esa propiedad al dominio real del frontend.

### Usuarios

| Método | Ruta | Descripción | Body |
|--------|------|-------------|------|
| `POST` | `/api/usuarios` | Crear usuario | `{ "nombre", "apellido", "cuil", "contrasena", "rol" }` (`rol` opcional; por defecto `OPERARIO`) |
| `POST` | `/api/usuarios/login` | Login | `{ "cuil", "contrasena" }` |
| `GET` | `/api/usuarios` | Listar usuarios activos | — |
| `GET` | `/api/usuarios/{id}` | Buscar usuario activo por ID | — |
| `DELETE` | `/api/usuarios/{id}` | Desactivar usuario (baja lógica) | — |

> **Baja lógica:** `DELETE /api/usuarios/{id}` no borra la fila. Setea `usuario.fecha_baja` y `usuario.id_usuario_baja` (quién la hizo); los listados y búsquedas por ID solo devuelven usuarios con `fecha_baja IS NULL`, y un usuario ya dado de baja se considera inexistente. Reglas: no podés darte de baja a vos mismo (`"No podés darte de baja a vos mismo"`) y no se puede dar de baja al último `ADMINISTRADOR` activo (`"No se puede dar de baja al último administrador"`). Ambas violaciones responden `400`. El CUIL sigue siendo único a nivel global, incluso si el usuario está dado de baja; el mensaje de duplicado lo aclara: `"Ya existe un usuario con ese CUIL (puede estar dado de baja): <cuil>"`. Esto preserva la trazabilidad de `orden_retiro` y `movimiento_fisico`, que referencian al usuario.

### Autenticación

| Método | Ruta | Descripción | Body |
|--------|------|-------------|------|
| `POST` | `/api/auth/login` | Login | `{ "cuil", "contrasena" }` |
| `POST` | `/api/auth/logout` | Logout (requiere sesión activa) | — |

- El login usa autenticación basada en sesión (`HttpSession`), no JWT. Al loguearse, Spring Security valida las credenciales (CUIL + contraseña contra el hash BCrypt guardado) y crea una sesión identificada por la cookie `JSESSIONID`, que el cliente debe reenviar en pedidos posteriores para mantenerse autenticado. La sesión expira tras 90 minutos de inactividad.
- Los usuarios dados de baja (`fecha_baja` no nulo) no pueden autenticarse: `CustomUserDetailsService` carga el usuario por CUIL y marca el `UserDetails` como `.disabled(true)`, por lo que el login falla con el mismo mensaje genérico y cuenta como intento fallido. **Limitación conocida:** una sesión abierta antes de la baja sigue siendo válida hasta que expira (90 minutos); no se invalidan sesiones al dar de baja.
- El sistema tiene protección CSRF habilitada: todo `POST`/`PUT`/`DELETE`, incluido `/api/auth/login`, requiere el token CSRF. El backend lo entrega en la cookie `XSRF-TOKEN`; el cliente debe reenviarlo en el header `X-XSRF-TOKEN`. El cliente debe pedir primero cualquier endpoint (el backend responde con la cookie) y luego copiar su valor al header en cada `POST`/`PUT`/`DELETE`, además de enviar las cookies (`withCredentials: true` en Angular). Atención: el interceptor `HttpClientXsrfModule` de Angular solo actúa sobre URLs relativas; con URL absoluta hace falta un proxy de desarrollo o un interceptor propio.
- Roles disponibles: `OPERARIO`, `ADMINISTRADOR` (ver enum `Role`).
- Tras 5 intentos fallidos de login para un mismo CUIL, el login se bloquea por 15 minutos y responde `429`. Un login exitoso reinicia el contador. El contador vive en memoria: se reinicia si el backend se reinicia.
- Los eventos de seguridad (login exitoso o fallido, bloqueos, logout y accesos denegados) se registran en el log con el CUIL y la IP de origen. Nunca se registran contraseñas ni tokens.

**Autorización por rol:**

| Ruta | Acceso |
|------|--------|
| `POST /api/auth/login` | Público |
| `/api/usuarios/**` | Solo `ADMINISTRADOR` |
| `GET /api/proveedores/**` | `OPERARIO` o `ADMINISTRADOR` |
| `POST`/`PUT`/`DELETE /api/proveedores/**` | Solo `ADMINISTRADOR` |
| Productos, órdenes de retiro y de compra, ubicaciones, valoraciones de proveedor, movimientos físicos | `OPERARIO` o `ADMINISTRADOR` |
| `/api/reportes/**` | Solo `ADMINISTRADOR` |
| Cualquier otra ruta | Requiere estar autenticado |

Un pedido sin sesión recibe `403`. Un pedido con sesión pero sin el rol requerido también recibe `403`.

### Productos

| Método | Ruta | Descripción | Body |
|--------|------|-------------|------|
| `POST` | `/api/productos` | Crear producto | `{ "nombreProducto", "descripcionProducto", "codigoBarras"?, "idProveedor", "origenCodigoBarras", "cantidadDisponible"?, "cantidadPendiente"? }` |
| `GET` | `/api/productos` | Listar todos los productos (con stock) | — |
| `GET` | `/api/productos/{id}` | Buscar producto por ID | — |
| `PUT` | `/api/productos/{id}` | Actualizar producto | `{ "nombreProducto", "descripcionProducto", "codigoBarras"?, "idProveedor", "origenCodigoBarras", "cantidadDisponible"?, "cantidadPendiente"? }` |
| `DELETE` | `/api/productos/{id}` | Dar de baja el producto (lógica) | — |
| `GET` | `/api/productos/{id}/stock` | Obtener stock de un producto | — |
| `PUT` | `/api/productos/{id}/stock` | Actualizar stock de un producto | `{ "cantidadDisponible"?, "cantidadPendiente"? }` |

> Si `codigoBarras` se envía vacío o nulo al crear, se genera automáticamente un código interno con formato `INT-XXXXXX`.
>
> **Baja lógica:** `DELETE /api/productos/{id}` no borra la fila ni su `Stock`. Setea `producto.fecha_baja` e `id_usuario_baja`; el producto deja de aparecer en listados/búsquedas y se considera inexistente para `GET`, `PUT`, stock, código de barras y ajustes de stock. Solo se permite si `cantidadDisponible == 0` (si no hay fila de stock se considera 0; `cantidadPendiente` no se exige); si hay stock disponible responde `400` con `"No se puede dar de baja un producto con stock disponible"`. El código de barras sigue siendo único global, incluso para productos dados de baja (`"Ya existe un producto con ese código de barras (puede estar dado de baja): <codigo>"`).

### Proveedores

| Método | Ruta | Descripción | Body |
|--------|------|-------------|------|
| `POST` | `/api/proveedores` | Crear proveedor | `{ "cuit", "razonSocial", "telefono"?, "mail"?, "direccion"? }` |
| `GET` | `/api/proveedores` | Listar todos los proveedores | — |
| `GET` | `/api/proveedores/{id}` | Buscar proveedor por ID | — |
| `PUT` | `/api/proveedores/{id}` | Actualizar proveedor | `{ "cuit", "razonSocial", "telefono"?, "mail"?, "direccion"? }` |
| `DELETE` | `/api/proveedores/{id}` | Dar de baja el proveedor (lógica) | — |

> **Baja lógica:** `DELETE /api/proveedores/{id}` no borra la fila. Setea `proveedor.fecha_baja` e `id_usuario_baja`; el proveedor deja de aparecer en listados/búsquedas y se considera inexistente al crear productos u órdenes de compra, al crear valoraciones y al consultar valoraciones por proveedor. Solo se permite si no tiene productos activos ni órdenes de compra `PENDIENTE` activas; si los tiene responde `400` con `"No se puede dar de baja un proveedor con productos activos u órdenes pendientes"`. El CUIT sigue siendo único global (`"Ya existe un proveedor con ese CUIT (puede estar dado de baja): <cuit>"`).

### Ubicaciones

| Método | Ruta | Descripción | Body |
|--------|------|-------------|------|
| `POST` | `/api/ubicaciones` | Crear ubicación | `{ "name" }` |
| `GET` | `/api/ubicaciones` | Listar todas las ubicaciones | — |
| `GET` | `/api/ubicaciones/{id}` | Buscar ubicación por ID | — |
| `PUT` | `/api/ubicaciones/{id}` | Actualizar ubicación | `{ "name" }` |
| `DELETE` | `/api/ubicaciones/{id}` | Dar de baja la ubicación (lógica) | — |

> **Baja lógica:** `DELETE /api/ubicaciones/{id}` no borra la fila. Setea `ubicacion.fecha_baja` e `id_usuario_baja`; la ubicación deja de aparecer en listados/búsquedas y se considera inexistente al crear movimientos físicos. Solo se permite si ningún producto activo está actualmente en esa ubicación (ubicación actual = `id_ubicacion_hasta` del último `movimiento_fisico` del producto); si hay productos responde `400` con `"No se puede dar de baja una ubicación que contiene productos"`.

### Valoraciones de Proveedor

| Método | Ruta | Descripción | Body |
|--------|------|-------------|------|
| `POST` | `/api/valoraciones-proveedor` | Crear valoración | `{ "idSupplier", "deliveryTime"?, "deliveryMethod"?, "priceQualityRatio"? }` |
| `GET` | `/api/valoraciones-proveedor` | Listar todas las valoraciones (filtrable por `?idSupplier=`) | — |
| `GET` | `/api/valoraciones-proveedor/{id}` | Buscar valoración por ID | — |
| `PUT` | `/api/valoraciones-proveedor/{id}` | Actualizar valoración | `{ "idSupplier", "deliveryTime"?, "deliveryMethod"?, "priceQualityRatio"? }` |
| `DELETE` | `/api/valoraciones-proveedor/{id}` | Eliminar valoración | — |

> La fecha/hora (`dateTime`) se asigna automáticamente al crear y al actualizar. El proveedor debe existir y estar activo al crear y al filtrar por `?idSupplier=`; un proveedor dado de baja se considera inexistente (`"Proveedor no encontrado"`). Al actualizar **no se cambia el proveedor** de la valoración (el campo `idSupplier` del body se ignora en `PUT`); solo se actualizan `deliveryTime`, `deliveryMethod` y `priceQualityRatio`.

### Órdenes de Retiro

| Método | Ruta | Descripción | Body |
|--------|------|-------------|------|
| `POST` | `/api/ordenes-retiro` | Crear orden de retiro | `{ "idUsuario", "lineasRetiro": [{ "idProducto", "cantidad" }] }` |
| `GET` | `/api/ordenes-retiro` | Listar órdenes (resumen, sin líneas) | — |
| `GET` | `/api/ordenes-retiro/{id}` | Buscar orden por ID (con líneas) | — |
| `PUT` | `/api/ordenes-retiro/{id}` | Actualizar orden | `{ "idUsuario", "lineasRetiro": [{ "idProducto", "cantidad" }] }` |
| `DELETE` | `/api/ordenes-retiro/{id}` | Dar de baja la orden (lógica; revierte stock) | — |

> Al crear/actualizar una orden de retiro, el stock se ajusta automáticamente: se resta de `cantidadDisponible` y se suma a `cantidadPendiente`. Al eliminar, se revierte el ajuste.
>
> **Baja lógica:** `DELETE` no borra la fila ni sus líneas. Setea `orden_retiro.fecha_baja` y `id_usuario_baja`; una orden dada de baja deja de aparecer en los listados y se considera inexistente para `GET /{id}`, `PUT` y un nuevo `DELETE`.

### Órdenes de Compra

| Método | Ruta | Descripción | Body |
|--------|------|-------------|------|
| `POST` | `/api/ordenes-compra` | Crear orden de compra | `{ "idSupplier", "lines": [{ "idProduct", "amount" }] }` |
| `GET` | `/api/ordenes-compra` | Listar órdenes (resumen, sin líneas) | — |
| `GET` | `/api/ordenes-compra/{id}` | Buscar orden por ID (con líneas) | — |
| `PUT` | `/api/ordenes-compra/{id}` | Actualizar orden | `{ "idSupplier", "lines": [{ "idProduct", "amount" }] }` |
| `PUT` | `/api/ordenes-compra/{id}/recibir` | Marcar orden como recibida (suma al stock disponible) | — |
| `DELETE` | `/api/ordenes-compra/{id}` | Dar de baja la orden (lógica; revierte stock si ya fue recibida) | — |

> Al **recibir** una orden de compra, cada línea suma su cantidad a `cantidadDisponible` del producto y se registra `fecha_recepcion`. Si se actualiza una orden ya recibida, se revierte el ajuste de las líneas viejas y se aplica el de las nuevas, sin modificar `fecha_recepcion`; al eliminar una recibida, se revierte el stock. El estado inicial es `PENDIENTE`.
>
> **Baja lógica:** `DELETE` no borra la fila ni sus líneas. Setea `orden_compra.fecha_baja` y `id_usuario_baja`; una orden dada de baja deja de aparecer en los listados y se considera inexistente para `GET /{id}`, `PUT`, `PUT /recibir` y un nuevo `DELETE`.
>
> La respuesta (`PurchaseOrderResponseDTO`) incluye `receptionDateTime` entre `status` y `lines`; vale `null` hasta que la orden se recibe. Las órdenes recibidas antes de la migración V5 también quedan con `receptionDateTime = null` (no se inventó un backfill).
>
> **Referencias activas:** al crear o actualizar una orden de retiro, el usuario y los productos referenciados deben estar activos. Al crear o actualizar una orden de compra, el proveedor y los productos deben estar activos. Un recurso dado de baja se considera inexistente (`"Usuario no encontrado"`, `"Proveedor no encontrado"`, `"Producto no encontrado: <id>"`). Como las órdenes dadas de baja conservan sus líneas, el historial se mantiene; productos y proveedores ahora se dan de baja lógicamente (1B), sin borrar filas referenciadas.

### Reportes (solo `ADMINISTRADOR`)

Endpoints de solo lectura bajo `/api/reportes`, restringidos a `ADMINISTRADOR`. Las rutas, parámetros y campos de respuesta están en español. Las consultas se resuelven en SQL/JPQL (sin iterar entidades en Java ni N+1).

**Período compartido (`from`/`to`):** parámetros `LocalDate` con formato `yyyy-MM-dd`, opcionales. `from` es inclusivo desde las 00:00 y `to` es inclusivo hasta el final del día (internamente el límite exclusivo es el inicio del día siguiente). Sin fechas, el rango es `1970-01-01 00:00` a `9999-01-01 00:00` (exclusivo). Si `from` es posterior a `to`, responde `400` con `"La fecha 'from' no puede ser posterior a 'to'"`. La resolución vive en el record `ReportPeriod` (`service`), de modo que las consultas nunca reciben parámetros nulos.

**Criterio de bajas:** los reportes históricos por período excluyen las órdenes de retiro dadas de baja (`orden_retiro.fecha_baja IS NOT NULL`). Incluyen productos y usuarios dados de baja cuando tuvieron actividad, con el campo booleano `activo` (`true` si `fecha_baja IS NULL`). Los reportes de estado actual excluyen productos, proveedores y ubicaciones dados de baja.

| Método | Ruta | Parámetros | Campos de respuesta |
|--------|------|------------|---------------------|
| `GET` | `/api/reportes/stock-bajo` | `umbral` (entero, opcional, por defecto `10`) | `idProducto`, `nombreProducto`, `codigoBarras`, `idProveedor`, `razonSocial`, `cantidadDisponible`, `cantidadPendiente`, `stockFechaHora` |
| `GET` | `/api/reportes/stock-por-proveedor` | — | `idProveedor`, `razonSocial`, `cantidadProductos`, `unidadesDisponibles`, `unidadesPendientes` |
| `GET` | `/api/reportes/productos/ubicacion-actual` | `soloSinUbicar` (booleano, opcional, por defecto `false`) | `idProducto`, `nombreProducto`, `codigoBarras`, `idUbicacion`, `nombreUbicacion`, `fechaUltimoMovimiento` |
| `GET` | `/api/reportes/ubicaciones/ocupacion` | — | `idUbicacion`, `nombreUbicacion`, `cantidadProductos` |
| `GET` | `/api/reportes/productos/mas-retirados` | `from`, `to`, `limit` (entero, opcional, por defecto `10`, entre `1` y `100`) | `idProducto`, `nombreProducto`, `codigoBarras`, `activo`, `unidadesRetiradas`, `cantidadOrdenes` |
| `GET` | `/api/reportes/productos/sin-rotacion` | `from`, `to`, `soloConStock` (booleano, opcional, por defecto `true`) | `idProducto`, `nombreProducto`, `codigoBarras`, `cantidadDisponible`, `ultimoRetiro` |
| `GET` | `/api/reportes/retiros/por-periodo` | `from`, `to`, `agrupacion` (`DIA`/`SEMANA`/`MES`, opcional, por defecto `DIA`) | `periodo`, `cantidadOrdenes`, `unidadesRetiradas` |
| `GET` | `/api/reportes/operarios/actividad` | `from`, `to` | `idUsuario`, `nombre`, `apellido`, `cuil`, `rol`, `activo`, `cantidadRetiros`, `unidadesRetiradas`, `cantidadMovimientos`, `ultimaActividad` |

- **Stock bajo:** productos activos con `cantidadDisponible <= umbral`, incluidos los de stock 0. Un producto sin fila de `stock` cuenta como 0. Orden: `cantidadDisponible` ascendente y luego `nombreProducto`. Si `umbral` es negativo responde `400` con `"El umbral no puede ser negativo"`.
- **Stock por proveedor:** incluye todos los proveedores activos, incluso los que no tienen productos (con ceros). Solo cuenta productos activos. **El schema no tiene precios**, por eso el reporte mide unidades, no dinero. Orden: `unidadesDisponibles` descendente y luego `razonSocial`.
- **Ubicación actual:** para cada producto activo, la ubicación es `id_ubicacion_hasta` de su último `movimiento_fisico`. Los productos que nunca se movieron aparecen con `idUbicacion`, `nombreUbicacion` y `fechaUltimoMovimiento` en `null`; `soloSinUbicar=true` devuelve solo esos. Si el último movimiento apunta a una ubicación dada de baja, la ubicación se muestra en `null` pero `fechaUltimoMovimiento` se conserva (el producto no se considera "sin ubicar" porque sí tuvo movimiento). Orden: `nombreUbicacion` (nulls al final) y luego `nombreProducto`.
- **Ocupación:** incluye todas las ubicaciones activas, incluso las vacías. `cantidadProductos` cuenta productos activos **distintos** cuya ubicación actual es esa. Orden: `cantidadProductos` descendente y luego `nombreUbicacion`.
- **Limitación de ocupación:** el stock no está dividido por ubicación y los movimientos físicos no mueven cantidades, por lo que el reporte cuenta productos distintos, no unidades.
- **Productos más retirados:** top de productos por unidades retiradas en el período, contando solo órdenes de retiro activas. `unidadesRetiradas` suma `linea_retiro.cantidad` y `cantidadOrdenes` cuenta órdenes distintas. Incluye productos dados de baja que tuvieron retiros, con `activo=false`. Solo aparecen productos con al menos un retiro en el período. Orden: `unidadesRetiradas` descendente y luego `nombreProducto`. `limit` fuera del rango `1..100` responde `400` con `"El límite debe estar entre 1 y 100"`.
- **Productos sin rotación:** productos activos sin ningún retiro activo en el período. `soloConStock=true` (default) limita a productos con `cantidadDisponible > 0`, el "stock muerto" que interesa al administrador. `ultimoRetiro` es la fecha del último retiro activo del producto **sin limitar por período** (`null` si nunca se retiró). Orden: `cantidadDisponible` descendente y luego `nombreProducto`.
- **Retiros por período:** serie temporal sobre órdenes de retiro activas. `agrupacion` es insensible a mayúsculas y se valida en el service (`DIA`, `SEMANA`, `MES`; otro valor responde `400` con `"La agrupación debe ser DIA, SEMANA o MES"`). El SQL usa `date_trunc` con un valor de la lista blanca (`day`/`week`/`month`), nunca el texto del cliente. **`SEMANA` usa la semana ISO: empieza el lunes**, comportamiento de `date_trunc('week')` de PostgreSQL. Solo se devuelven los buckets con datos; el relleno de ceros queda para el frontend. Orden: `periodo` ascendente.
- **Actividad de operarios:** incluye todos los usuarios, también los que no tuvieron actividad (con ceros) y los dados de baja (`activo=false`). `cantidadRetiros` cuenta órdenes de retiro activas; `unidadesRetiradas` suma sus líneas; `cantidadMovimientos` cuenta filas de `movimiento_fisico`; `ultimaActividad` es la máxima fecha entre retiros y movimientos **dentro del período** (`null` si no hubo). Para evitar el producto cartesiano, los retiros y los movimientos se agregan en consultas separadas y se combinan por `idUsuario` en el service. Orden: `(cantidadRetiros + cantidadMovimientos)` descendente, apellido, nombre. El DTO no expone la contraseña ni su hash.
- Un parámetro con tipo inválido (por ejemplo `umbral=abc` o `from=abc`) responde `400` con `{"error": "El parámetro '<nombre>' tiene un valor inválido"}`.

## Validaciones

Los DTOs de creación usan Jakarta Bean Validation. Errores de validación retornan 400 Bad Request con los mensajes en español:

- **Usuario**: nombre y apellido (3-150 chars), CUIL (formato XX-XXXXXXXX-X), contraseña (mínimo 8 chars, al menos una mayúscula y un dígito)
- **Producto**: nombre (3-150 chars), descripción (3-500 chars), código de barras (máx 50 chars, opcional), proveedor requerido, origen (`FABRICANTE` o `INTERNO`), cantidades ≥ 0
- **Proveedor**: CUIT (formato XX-XXXXXXXX-X), razón social (3-150 chars), email válido (si se provee)
- **Ubicación**: nombre (3-100 chars, no vacío)
- **Orden de retiro**: usuario requerido, líneas requeridas (mínimo 1), cantidad por línea ≥ 1
- **Orden de compra**: proveedor requerido, líneas requeridas (mínimo 1), cantidad por línea ≥ 1
- **Valoración de proveedor**: proveedor requerido, tiempo de entrega ≥ 0, forma de entrega y relación precio/calidad (máx 100 chars, opcionales)

## Issues conocidos

1. ~~**Orphan Stock al eliminar producto**~~ **RESUELTO/SUPERSEDIDO**: la baja de producto ahora es lógica (1B) y conserva intencionalmente la fila de `Stock`; ya no hay borrado físico ni filas huérfanas.
2. **Login duplicado**: `POST /api/usuarios/login` y `POST /api/auth/login` hacen lo mismo.
3. **N+1 query en listar productos**: Cada producto dispara una query separada para buscar su stock.
4. **Sin autenticación real**: El login retorna datos pero no genera token ni sesión.
5. **Sin handler global de excepciones**: Los errores no manejados retornan 500 genéricos.
6. **Test único**: Solo existe `contextLoads()`, sin tests de integración ni unitarios.

> Ver `AGENTS.md` para la lista completa de issues conocidos.
