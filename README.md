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
```

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

> **Usuario administrador por defecto (seed V3):** CUIL `20-00000000-1`, contraseña `Admin1234`, rol `ADMINISTRADOR`. Existe en todas las bases (nuevas o baselineadas) tras arrancar la app. Se puede loguear con `POST /api/auth/login` o eliminar/modificar si se prefiere.

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
│   └── UserResponseDTO.java
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
│   ├── StockRepository.java
│   ├── SupplierRatingRepository.java
│   └── UserRepository.java
└── service/
    ├── LocationService.java
    ├── PickOrderService.java
    ├── ProductService.java
    ├── ProveedorService.java
    ├── PurchaseOrderService.java
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

Todas las rutas están bajo el prefijo `/api/`. Los controladores permiten CORS desde `http://localhost:4200` (Angular).

### Usuarios

| Método | Ruta | Descripción | Body |
|--------|------|-------------|------|
| `POST` | `/api/usuarios` | Crear usuario | `{ "nombre", "apellido", "cuil", "contrasena" }` |
| `POST` | `/api/usuarios/login` | Login | `{ "cuil", "contrasena" }` |
| `GET` | `/api/usuarios` | Listar todos los usuarios | — |
| `GET` | `/api/usuarios/{id}` | Buscar usuario por ID | — |
| `DELETE` | `/api/usuarios/{id}` | Eliminar usuario | — |

### Autenticación

| Método | Ruta | Descripción | Body |
|--------|------|-------------|------|
| `POST` | `/api/auth/login` | Login | `{ "cuil", "contrasena" }` |
| `POST` | `/api/auth/logout` | Logout (requiere sesión activa) | — |

El login usa autenticación basada en sesión (`HttpSession`), no JWT. Al loguearse, Spring Security valida las credenciales (CUIL + contraseña contra el hash BCrypt guardado) y crea una sesión identificada por la cookie `JSESSIONID`, que el cliente debe reenviar en pedidos posteriores para mantenerse autenticado. La sesión expira tras 90 minutos de inactividad.

Roles disponibles: `OPERARIO`, `ADMINISTRADOR` (ver enum `Role`).

> **Nota:** la autorización por rol sobre el resto de los endpoints (qué rutas requieren qué rol) todavía no está implementada — ver `TODO.md`, sección de seguridad.

### Productos

| Método | Ruta | Descripción | Body |
|--------|------|-------------|------|
| `POST` | `/api/productos` | Crear producto | `{ "nombreProducto", "descripcionProducto", "codigoBarras"?, "idProveedor", "origenCodigoBarras", "cantidadDisponible"?, "cantidadPendiente"? }` |
| `GET` | `/api/productos` | Listar todos los productos (con stock) | — |
| `GET` | `/api/productos/{id}` | Buscar producto por ID | — |
| `PUT` | `/api/productos/{id}` | Actualizar producto | `{ "nombreProducto", "descripcionProducto", "codigoBarras"?, "idProveedor", "origenCodigoBarras", "cantidadDisponible"?, "cantidadPendiente"? }` |
| `DELETE` | `/api/productos/{id}` | Eliminar producto | — |
| `GET` | `/api/productos/{id}/stock` | Obtener stock de un producto | — |
| `PUT` | `/api/productos/{id}/stock` | Actualizar stock de un producto | `{ "cantidadDisponible"?, "cantidadPendiente"? }` |

> Si `codigoBarras` se envía vacío o nulo al crear, se genera automáticamente un código interno con formato `INT-XXXXXX`.

### Proveedores

| Método | Ruta | Descripción | Body |
|--------|------|-------------|------|
| `POST` | `/api/proveedores` | Crear proveedor | `{ "cuit", "razonSocial", "telefono"?, "mail"?, "direccion"? }` |
| `GET` | `/api/proveedores` | Listar todos los proveedores | — |
| `GET` | `/api/proveedores/{id}` | Buscar proveedor por ID | — |
| `PUT` | `/api/proveedores/{id}` | Actualizar proveedor | `{ "cuit", "razonSocial", "telefono"?, "mail"?, "direccion"? }` |
| `DELETE` | `/api/proveedores/{id}` | Eliminar proveedor | — |

### Ubicaciones

| Método | Ruta | Descripción | Body |
|--------|------|-------------|------|
| `POST` | `/api/ubicaciones` | Crear ubicación | `{ "name" }` |
| `GET` | `/api/ubicaciones` | Listar todas las ubicaciones | — |
| `GET` | `/api/ubicaciones/{id}` | Buscar ubicación por ID | — |
| `PUT` | `/api/ubicaciones/{id}` | Actualizar ubicación | `{ "name" }` |
| `DELETE` | `/api/ubicaciones/{id}` | Eliminar ubicación | — |

### Valoraciones de Proveedor

| Método | Ruta | Descripción | Body |
|--------|------|-------------|------|
| `POST` | `/api/valoraciones-proveedor` | Crear valoración | `{ "idSupplier", "deliveryTime"?, "deliveryMethod"?, "priceQualityRatio"? }` |
| `GET` | `/api/valoraciones-proveedor` | Listar todas las valoraciones (filtrable por `?idSupplier=`) | — |
| `GET` | `/api/valoraciones-proveedor/{id}` | Buscar valoración por ID | — |
| `PUT` | `/api/valoraciones-proveedor/{id}` | Actualizar valoración | `{ "idSupplier", "deliveryTime"?, "deliveryMethod"?, "priceQualityRatio"? }` |
| `DELETE` | `/api/valoraciones-proveedor/{id}` | Eliminar valoración | — |

> La fecha/hora (`dateTime`) se asigna automáticamente al crear y al actualizar. El proveedor debe existir al crear. Al actualizar **no se cambia el proveedor** de la valoración (el campo `idSupplier` del body se ignora en `PUT`); solo se actualizan `deliveryTime`, `deliveryMethod` y `priceQualityRatio`.

### Órdenes de Retiro

| Método | Ruta | Descripción | Body |
|--------|------|-------------|------|
| `POST` | `/api/ordenes-retiro` | Crear orden de retiro | `{ "idUsuario", "lineasRetiro": [{ "idProducto", "cantidad" }] }` |
| `GET` | `/api/ordenes-retiro` | Listar órdenes (resumen, sin líneas) | — |
| `GET` | `/api/ordenes-retiro/{id}` | Buscar orden por ID (con líneas) | — |
| `PUT` | `/api/ordenes-retiro/{id}` | Actualizar orden | `{ "idUsuario", "lineasRetiro": [{ "idProducto", "cantidad" }] }` |
| `DELETE` | `/api/ordenes-retiro/{id}` | Eliminar orden (revierte stock) | — |

> Al crear/actualizar una orden de retiro, el stock se ajusta automáticamente: se resta de `cantidadDisponible` y se suma a `cantidadPendiente`. Al eliminar, se revierte el ajuste.

### Órdenes de Compra

| Método | Ruta | Descripción | Body |
|--------|------|-------------|------|
| `POST` | `/api/ordenes-compra` | Crear orden de compra | `{ "idSupplier", "lines": [{ "idProduct", "amount" }] }` |
| `GET` | `/api/ordenes-compra` | Listar órdenes (resumen, sin líneas) | — |
| `GET` | `/api/ordenes-compra/{id}` | Buscar orden por ID (con líneas) | — |
| `PUT` | `/api/ordenes-compra/{id}` | Actualizar orden | `{ "idSupplier", "lines": [{ "idProduct", "amount" }] }` |
| `PUT` | `/api/ordenes-compra/{id}/recibir` | Marcar orden como recibida (suma al stock disponible) | — |
| `DELETE` | `/api/ordenes-compra/{id}` | Eliminar orden (revierte stock si ya fue recibida) | — |

> Al **recibir** una orden de compra, cada línea suma su cantidad a `cantidadDisponible` del producto. Si se actualiza una orden ya recibida, se revierte el ajuste de las líneas viejas y se aplica el de las nuevas; al eliminar una recibida, se revierte el stock. El estado inicial es `PENDIENTE`.

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

1. **Orphan Stock al eliminar producto**: `ProductService.deleteById()` no elimina el `Stock` asociado — quedan filas huérfanas.
2. **Login duplicado**: `POST /api/usuarios/login` y `POST /api/auth/login` hacen lo mismo.
3. **N+1 query en listar productos**: Cada producto dispara una query separada para buscar su stock.
4. **Sin autenticación real**: El login retorna datos pero no genera token ni sesión.
5. **Sin handler global de excepciones**: Los errores no manejados retornan 500 genéricos.
6. **Test único**: Solo existe `contextLoads()`, sin tests de integración ni unitarios.

> Ver `AGENTS.md` para la lista completa de issues conocidos.
