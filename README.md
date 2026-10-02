# alamano-auth-service

Auth Service de ALaManoApp — cubre **HU1**: "Como usuario, quiero registrarme e iniciar sesión con correo y contraseña, para acceder a la app".

Java 21 + Spring Boot 4.1.1, arquitectura hexagonal (`domain` / `application` / `infrastructure`).

## Antes de correrlo: genera tus propias llaves RS256

La carpeta `keys/` **no está en el repo** (y no debe estarlo — ahí vive la llave privada que firma los tokens de todo el sistema). Cada quien genera la suya en local.

**Opción A — con Git Bash / WSL (si tienes `openssl`):**
```bash
mkdir -p keys
openssl genpkey -algorithm RSA -out keys/jwt-private.pem -pkeyopt rsa_keygen_bits:2048
openssl rsa -in keys/jwt-private.pem -pubout -out keys/jwt-public.pem
```

**Opción B — con `jshell` (viene con el JDK, sin instalar nada más):**
Corre `jshell` parado en la raíz del proyecto y pega:
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
Sal con `/exit`.

Si este servicio corre dentro del `docker-compose.yml` de `alamano-infra`, las llaves se montan desde ahí (ver ese repo) — esto solo aplica si corres `alamano-auth-service` suelto.

## Correr localmente (sin Docker Compose completo)

Necesitas un Postgres disponible en el puerto 5432, con un usuario/clave `auth_app`/`auth_app` y una base `alamano`:
```powershell
docker run --name alamano-postgres-test -e POSTGRES_USER=auth_app -e POSTGRES_PASSWORD=auth_app -e POSTGRES_DB=alamano -p 5432:5432 -d postgres:16-alpine
```

Luego, desde IntelliJ (botón derecho sobre `AlamanoAuthServiceApplication` > Run) o por terminal:
```powershell
.\mvnw.cmd spring-boot:run
```

El servicio queda escuchando en `http://localhost:8081`.

### ⚠️ Si ya tienes otro Postgres corriendo en el puerto 5432

Es común en una máquina compartida entre varias materias — si al correr la app te sale `FATAL: password authentication failed for user "auth_app"` (no "connection refused"), es señal de que **otro** Postgres ya está ocupando el 5432 y tu app se conectó a ese por error, no al tuyo.

Verifica con `docker ps -a` si tienes más de un contenedor de Postgres. Si es así, levanta el tuyo en otro puerto:
```powershell
docker run --name alamano-postgres-test -e POSTGRES_USER=auth_app -e POSTGRES_PASSWORD=auth_app -e POSTGRES_DB=alamano -p 5433:5432 -d postgres:16-alpine
```
Y en IntelliJ: **Run > Edit Configurations** > tu configuración de `AlamanoAuthServiceApplication` > campo **Environment variables**, agrega:
```
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/alamano?currentSchema=auth
```
Esto es solo para tu máquina — no se sube al repo (vive en `.idea/workspace.xml`, que está en `.gitignore`), y no afecta a nadie más ni al `docker-compose.yml` real, que sigue usando el 5432 normalmente.

## Probar los endpoints

En PowerShell, usa `Invoke-RestMethod` en vez de `curl.exe` — evita problemas de escape de comillas con JSON:

**Registrar:**
```powershell
$body = @{ nombre = "Ana Torres"; correo = "ana@example.com"; password = "Segura123" } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8081/auth/register" -Method Post -ContentType "application/json" -Body $body
```

**Login:**
```powershell
$body = @{ correo = "ana@example.com"; password = "Segura123" } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8081/auth/login" -Method Post -ContentType "application/json" -Body $body
```

En Mac/Linux, `curl` normal funciona sin este problema:
```bash
curl -X POST http://localhost:8081/auth/register -H "Content-Type: application/json" -d '{"nombre":"Ana Torres","correo":"ana@example.com","password":"Segura123"}'
```

## Estructura

```
src/main/java/com/alamano/auth/
├── domain/              Usuario, Rol, UsuarioRepository (puerto) — sin Spring, sin JPA
├── application/         RegisterUseCase, LoginUseCase, TokenService (puerto)
└── infrastructure/
    ├── web/             AuthController, DTOs, manejador de excepciones
    ├── persistence/      Entidad JPA + adaptador que implementa UsuarioRepository
    └── security/         Carga de llaves RS256, JwtTokenService, BCrypt
```

## Tareas de este repo (Azure DevOps)
- **1.1** `[Auth] Registro de usuarios con validación y hash bcrypt` — ✅ confirmado funcionando
- **1.2** `[Auth] Login, emisión de JWT RS256 con rol y filtro de verificación` — ✅ confirmado funcionando
- **1.4** `[QA] Pruebas unitarias y de integración` — pendiente
