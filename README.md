# ALaMano Auth Service

Servicio de identidad de ALaMano. Registra usuarios, verifica contraseñas y emite el JWT que usa todo el sistema. Es el único servicio que conoce la llave privada: no valida tokens entrantes ni consulta a los demás servicios, y Core y el Realtime Gateway verifican la firma por su cuenta con la llave pública.

Cubre **HU1**: "Como usuario, quiero registrarme e iniciar sesión con correo y contraseña, para acceder a la app".

## Capas

- `domain`: `Usuario`, `Rol` y el puerto `UsuarioRepository`. No usa Spring ni JPA.
- `application`: `RegisterUseCase`, `LoginUseCase` y el puerto `TokenService`.
- `infrastructure`: adaptadores HTTP (`web`), Postgres (`persistence`) y llaves y firma (`security`).

## Ejecutar localmente

Requiere Java 21 y Maven Wrapper. El servicio escucha en el puerto `8081`. Core usa el `8082` y el Gateway el `8083`.

Las pruebas no necesitan nada levantado, pero **correr la aplicación sí necesita Postgres y las llaves** (las dos secciones siguientes).

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Postgres en el puerto 5432, con la base `alamano` y el usuario `auth_app`:

```powershell
docker run --name alamano-postgres-test -e POSTGRES_USER=auth_app -e POSTGRES_PASSWORD=auth_app -e POSTGRES_DB=alamano -p 5432:5432 -d postgres:16-alpine
```

Flyway crea el esquema `auth` y la tabla `usuarios` al arrancar (migración `V1`). Hibernate corre con `ddl-auto: validate`: no toca el esquema, solo comprueba que coincida con las entidades.

El health check queda en `http://localhost:8081/actuator/health`. Es el que usa el `docker-compose.yml` de `alamano-infra`, y es la única ruta pública además de registro y login.

## Llaves RS256

La carpeta `keys/` **no está en el repo** y no debe estarlo: ahí vive la llave privada que firma los tokens de todo el sistema. Cada quien genera la suya.

Con `openssl` (Git Bash o WSL):

```bash
mkdir -p keys
openssl genpkey -algorithm RSA -out keys/jwt-private.pem -pkeyopt rsa_keygen_bits:2048
openssl rsa -in keys/jwt-private.pem -pubout -out keys/jwt-public.pem
```

Tiene que ser `genpkey` (formato PKCS#8), no `genrsa`: el formato PKCS#1 que produce `genrsa` no lo lee `JwtKeyConfig`.

Sin `openssl`, con `jshell` parado en la raíz del proyecto:

```java
import java.security.*
import java.nio.file.*
import java.util.Base64

var kpg = KeyPairGenerator.getInstance("RSA")
kpg.initialize(2048)
var kp = kpg.generateKeyPair()

var privPem = "-----BEGIN PRIVATE KEY-----\n" + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(kp.getPrivate().getEncoded()) + "\n-----END PRIVATE KEY-----\n"
var pubPem = "-----BEGIN PUBLIC KEY-----\n" + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(kp.getPublic().getEncoded()) + "\n-----END PUBLIC KEY-----\n"

Files.createDirectories(Path.of("keys"))
Files.writeString(Path.of("keys/jwt-private.pem"), privPem)
Files.writeString(Path.of("keys/jwt-public.pem"), pubPem)
```

La llave **pública** se copia a Core y al Gateway; la **privada** no sale de aquí. Dentro del `docker-compose.yml` de `alamano-infra` las dos se montan desde ese repo y no hay que generarlas aquí.

| Variable | Por defecto |
|---|---|
| `JWT_PRIVATE_KEY_PATH` | `./keys/jwt-private.pem` |
| `JWT_PUBLIC_KEY_PATH` | `./keys/jwt-public.pem` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/alamano?currentSchema=auth` |
| `SPRING_DATASOURCE_USERNAME` | `auth_app` |
| `SPRING_DATASOURCE_PASSWORD` | `auth_app` |

La vigencia del token se controla con `app.jwt.expiration-minutes` (60 por defecto).

## HU1: registro e inicio de sesión

Los dos endpoints son públicos; cualquier otra ruta exige autenticación.

### `POST /auth/register`

```json
{ "nombre": "Ana Torres", "correo": "ana@example.com", "password": "Segura123", "rol": "VENDEDOR" }
```

`rol` es opcional y acepta `USUARIO` o `VENDEDOR`; si se omite queda en `USUARIO`. El correo se normaliza a minúsculas y la contraseña se guarda con BCrypt, nunca en claro.

| Respuesta | Cuándo |
|---|---|
| **201** | Usuario creado. Devuelve `id`, `nombre`, `correo`, `rol` y `creadoEn`. Nunca devuelve el hash. |
| **400** | Falta un campo o la contraseña tiene menos de 6 caracteres. |
| **409** | Ese correo ya está registrado. |

### `POST /auth/login`

```json
{ "correo": "ana@example.com", "password": "Segura123" }
```

| Respuesta | Cuándo |
|---|---|
| **200** | Devuelve `token` y el objeto `usuario`. |
| **400** | Falta el correo o la contraseña. |
| **401** | Credenciales inválidas. El mensaje es el mismo si el correo no existe o si la contraseña está mal, para no revelar qué correos están registrados. |

Ejemplo de respuesta:

```json
{
  "token": "eyJhbGciOiJSUzI1NiJ9...",
  "usuario": {
    "id": 1,
    "nombre": "Ana Torres",
    "correo": "ana@example.com",
    "rol": "VENDEDOR",
    "creadoEn": "2026-10-09T12:00:00Z"
  }
}
```

## El token

Firmado con RS256. El `sub` es el id del usuario: es el mismo identificador con el que Core guarda al vendedor en su tabla `professionals`.

| Claim | Contenido |
|---|---|
| `sub` | Id del usuario, como texto |
| `correo`, `nombre` | Datos del usuario |
| `rol` | Nombre del dominio: `USUARIO` o `VENDEDOR` |
| `role` | Nombre acordado entre servicios: `CLIENT` o `PROFESSIONAL` |
| `iat`, `exp` | Emisión y vencimiento |

El rol viaja dos veces a propósito. El frontend usa `rol`, con el vocabulario del dominio en español. Core y el Gateway leen `role`, que es el que convierten en `ROLE_PROFESSIONAL` o `ROLE_CLIENT` para autorizar. Si se cambia uno de los dos nombres hay que avisarle al equipo: es un contrato entre servicios, no un detalle interno.

| Rol del dominio | Claim `role` | Permite |
|---|---|---|
| `VENDEDOR` | `PROFESSIONAL` | Conectarse al mapa, cambiar estados, publicar promociones |
| `USUARIO` | `CLIENT` | Reservar, tomar promociones, ver el mapa |

## Probar los endpoints

En PowerShell, `Invoke-RestMethod` en vez de `curl.exe`: evita los problemas de escape de comillas con JSON.

```powershell
$body = @{ nombre = "Ana Torres"; correo = "ana@example.com"; password = "Segura123"; rol = "VENDEDOR" } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8081/auth/register" -Method Post -ContentType "application/json" -Body $body

$body = @{ correo = "ana@example.com"; password = "Segura123" } | ConvertTo-Json
$login = Invoke-RestMethod -Uri "http://localhost:8081/auth/login" -Method Post -ContentType "application/json" -Body $body
$login.token
```

En Mac y Linux, `curl` funciona sin ese problema:

```bash
curl -X POST http://localhost:8081/auth/register -H "Content-Type: application/json" \
  -d '{"nombre":"Ana Torres","correo":"ana@example.com","password":"Segura123"}'
```

## Pruebas

`.\mvnw.cmd verify` corre 27 pruebas y **no necesita Postgres, Docker ni las llaves**: el
perfil `test` usa H2 en memoria, Flyway aplica las mismas migraciones y
`JwtTokenServiceTest` genera su propio par de llaves.

| Clase | Qué cubre |
|---|---|
| `RegisterUseCaseTest` | 8 · hash bcrypt, normalización del correo, rol por defecto, correo duplicado |
| `LoginUseCaseTest` | 5 · credenciales válidas e inválidas, y que el mensaje de error no delate qué correos existen |
| `AuthControllerIntegrationTest` | 10 · los dos endpoints de punta a punta contra la base, con sus 201, 400, 409 y 401 |
| `JwtTokenServiceTest` | 3 · los claims del token y la firma RS256 |
| `AlamanoAuthServiceApplicationTests` | 1 · que el contexto completo arranque |

Dos cosas que conviene saber antes de agregar pruebas aquí:

**Los dobles son a mano, no con Mockito.** Este servicio usa los *starters* modulares de
Spring Boot 4, y no dependemos de que traigan Mockito. El codificador de contraseñas y el
repositorio de la prueba de integración son los reales, no simulados.

**Las clases terminan en `Test`, nunca en `IT`.** Surefire solo ejecuta `*Test` y `*Tests`;
una clase `*IT` la corre Failsafe, que este proyecto no tiene configurado, así que se
quedaría sin ejecutar **mientras el build pasa en verde**. Si dudas de si algo corrió:

```powershell
Get-Content target\surefire-reports\*.txt | Select-String "Tests run"
```

**Sobre la URL de H2:** lleva `DATABASE_TO_LOWER=TRUE` y no es opcional. Sin eso H2 crea
`AUTH.USUARIOS` en mayúsculas, Hibernate busca `auth.usuarios` en minúsculas y la
validación del esquema falla con `missing table [usuarios]`. Por lo mismo, las consultas
con `JdbcTemplate` en las pruebas llevan el esquema: `DELETE FROM auth.usuarios`.

## Si ya tienes otro Postgres en el puerto 5432

Pasa seguido en una máquina compartida entre materias. Si al arrancar sale `FATAL: password authentication failed for user "auth_app"` (y no `connection refused`), es que hay **otro** Postgres ocupando el 5432 y la aplicación se conectó a ese. Revisa con `docker ps -a`.

Levanta el tuyo en otro puerto:

```powershell
docker run --name alamano-postgres-test -e POSTGRES_USER=auth_app -e POSTGRES_PASSWORD=auth_app -e POSTGRES_DB=alamano -p 5433:5432 -d postgres:16-alpine
```

Y en IntelliJ, **Run > Edit Configurations**, en **Environment variables** de `AlamanoAuthServiceApplication`:

```
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/alamano?currentSchema=auth
```

Eso vive en `.idea/workspace.xml`, que está en `.gitignore`: es solo para tu máquina y no afecta al `docker-compose.yml` de `alamano-infra`, que sigue usando el 5432.

## Tareas de este repo (Azure DevOps)

| Tarea | Estado |
|---|---|
| **1.1** `[Auth] Registro de usuarios con validación y hash bcrypt` | Hecha |
| **1.2** `[Auth] Login, emisión de JWT RS256 con rol` | Hecha |
| **1.4** `[QA] Pruebas unitarias y de integración` (AB#334) | Hecha. 27 pruebas en 5 clases |
