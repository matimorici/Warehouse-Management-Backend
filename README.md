# Warehouse Management Backend (WMS)

Backend REST API para la gestión de almacén. Sistema diseñado para administrar productos, proveedores, stock y órdenes de retiro.

## Tecnologías

- **Java 25** + **Spring Boot 4.1.0**
- **PostgreSQL** (base de datos)
- **Spring Data JPA** (acceso a datos)
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

El sistema necesita una base de datos PostgreSQL llamada `wms_db`. Creala con el siguiente SQL:

```sql
CREATE DATABASE wms_db;
```

> **Nota:** El schema de las tablas debe existir externamente. La propiedad `ddl-auto` está en modo `validate`, lo que significa que Hibernate solo valida que el schema coincida con las entidades, pero **no crea las tablas automáticamente**. Si necesitás generar el schema, podés cambiar temporalmente `ddl-auto` a `create` o `update` en `application.properties`, arrancar la app una vez para que Hibernate genere las tablas, y luego volver a `validate`.

### 3. Configurar credenciales (opcional)

Las credenciales por defecto están en `wms/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/wms_db
spring.datasource.username=postgres
spring.datasource.password=12345
```

Si tu PostgreSQL usa otras credenciales, editá ese archivo.

### 4. Ejecutar

Desde la carpeta `wms/`:

```bash
./mvnw spring-boot:run
```

La aplicación arranca en **http://localhost:8080**.

### Build completo

```bash
./mvnw clean compile    # Compilar
./mvnw package          # Generar JAR
./mvnw test             # Ejecutar tests
```

## Estructura del proyecto

```
wms/src/main/java/big_three/wms/
├── WmsApplication.java              # Punto de entrada
├── config/
│   └── SecurityConfig.java          # Configuración de seguridad
├── controller/
│   ├── AuthController.java          # Login (duplicado)
│   ├── PickOrderController.java     # CRUD órdenes de retiro
│   ├── ProductController.java       # CRUD productos + stock
│   ├── ProveedorController.java     # CRUD proveedores
│   └── UserController.java          # CRUD usuarios + login
├── dto/
│   ├── LoginRequestDTO.java
│   ├── PickOrderCreateDTO.java
│   ├── PickOrderLineCreateDTO.java
│   ├── PickOrderLineResponseDTO.java
│   ├── PickOrderResponseDTO.java
│   ├── ProductCreateDTO.java
│   ├── ProductResponseDTO.java
│   ├── ProveedorCreateDTO.java
│   ├── ProveedorResponseDTO.java
│   ├── StockResponseDTO.java
│   ├── StockUpdateDTO.java
│   ├── UserCreateDTO.java
│   └── UserResponseDTO.java
├── exception/
│   └── InvalidCredentialsException.java
├── model/
│   ├── PickOrder.java               # Orden de retiro
│   ├── PickOrderLine.java           # Línea de orden de retiro (composite PK)
│   ├── Product.java                 # Producto (enum OrigenCodigoBarras)
│   ├── Proveedor.java               # Proveedor
│   ├── Stock.java                   # Stock (1:1 con Product)
│   └── User.java                    # Usuario
├── repository/
│   ├── PickOrderLineRepository.java
│   ├── PickOrderRepository.java
│   ├── ProductRepository.java
│   ├── ProveedorRepository.java
│   ├── StockRepository.java
│   └── UserRepository.java
├── service/
│   ├── PickOrderService.java
│   ├── ProductService.java
│   ├── ProveedorService.java
│   └── UserService.java
└── util/
    └── Validations.java             # (vacía — placeholder)
```

## Entidades

| Entidad | Tabla | Descripción |
|---------|-------|-------------|
| **User** | `usuario` | Usuarios del sistema (nombre, apellido, CUIL, rol, contraseña hasheada con BCrypt). Rol por defecto: `OPERARIO`. |
| **Product** | `producto` | Productos del almacén. Tiene código de barras (interno o de fábrica), descripción y relación con un proveedor. |
| **Proveedor** | `proveedor` | Proveedores de productos (CUIT, razón social, teléfono, mail, dirección). |
| **Stock** | `stock` | Stock de cada producto. Relación 1:1 con Product (comparten PK). Tiene cantidad disponible y cantidad pendiente. |
| **PickOrder** | `orden_retiro` | Orden de retiro de productos. Asociada a un usuario por `id_usuario` y compuesta por una o más líneas. |
| **PickOrderLine** | `linea_retiro` | Línea de una orden de retiro. PK compuesta: `id_orden_retiro` + `id_producto`. Cantidad a retirar. |

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
| `POST` | `/api/auth/login` | Login (duplicado del anterior) | `{ "cuil", "contrasena" }` |

> **Nota:** La autenticación aún no está implementada. El login retorna un DTO con los datos del usuario pero no genera token ni sesión. Los endpoints de `GET /api/usuarios`, `GET /api/usuarios/{id}` y `DELETE /api/usuarios/{id}` requieren autenticación según la configuración de seguridad, pero no hay mecanismo real para proveerla.

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

### Órdenes de Retiro

| Método | Ruta | Descripción | Body |
|--------|------|-------------|------|
| `POST` | `/api/ordenes-retiro` | Crear orden de retiro | `{ "idUsuario", "lineasRetiro": [{ "idProducto", "cantidad" }] }` |
| `GET` | `/api/ordenes-retiro` | Listar órdenes (resumen, sin líneas) | — |
| `GET` | `/api/ordenes-retiro/{id}` | Buscar orden por ID (con líneas) | — |
| `PUT` | `/api/ordenes-retiro/{id}` | Actualizar orden | `{ "idUsuario", "lineasRetiro": [{ "idProducto", "cantidad" }] }` |
| `DELETE` | `/api/ordenes-retiro/{id}` | Eliminar orden (revierte stock) | — |

> Al crear/actualizar una orden de retiro, el stock se ajusta automáticamente: se resta de `cantidadDisponible` y se suma a `cantidadPendiente`. Al eliminar, se revierte el ajuste.

## Validaciones

Los DTOs de creación usan Jakarta Bean Validation. Errores de validación retornan 400 Bad Request con los mensajes en español:

- **Usuario**: nombre y apellido (3-150 chars), CUIL (formato XX-XXXXXXXX-X), contraseña (mínimo 8 chars, al menos una mayúscula y un dígito)
- **Producto**: nombre (3-150 chars), descripción (3-500 chars), código de barras (máx 50 chars, opcional), proveedor requerido, origen (`FABRICANTE` o `INTERNO`), cantidades ≥ 0
- **Proveedor**: CUIT (formato XX-XXXXXXXX-X), razón social (3-150 chars), email válido (si se provee)
- **Orden de retiro**: usuario requerido, líneas requeridas (mínimo 1), cantidad por línea ≥ 1

## Issues conocidos

1. **Orphan Stock al eliminar producto**: `ProductService.deleteById()` no elimina el `Stock` asociado — quedan filas huérfanas.
2. **Login duplicado**: `POST /api/usuarios/login` y `POST /api/auth/login` hacen lo mismo.
3. **N+1 query en listar productos**: Cada producto dispara una query separada para buscar su stock.
4. **Sin autenticación real**: El login retorna datos pero no genera token ni sesión.
5. **Sin handler global de excepciones**: Los errores no manejados retornan 500 genéricos.
6. **Test único**: Solo existe `contextLoads()`, sin tests de integración ni unitarios.

> Ver `AGENTS.md` para la lista completa de issues conocidos.
