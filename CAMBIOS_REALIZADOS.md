# 🛠️ AguaMap — Cambios realizados y estado final

Este documento resume **todo lo que se implementó** para conectar AguaMap a Supabase,
sin romper la lógica que ya tenías. La app **compila correctamente** (`BUILD SUCCESSFUL`).

> 🔑 **Idea clave:** todo se hizo con estrategia **OFFLINE-FIRST**. La app intenta usar
> Supabase y, si algo falla (sin internet o tablas aún no creadas), usa los datos locales.
> Por eso **nada se rompe** aunque todavía no configures el backend.
> Para activar el backend, sigue la guía **[SUPABASE_TABLAS.md](SUPABASE_TABLAS.md)**.

---

## 1. Resumen: antes ❌ vs ahora ✅

| Funcionalidad | Antes | Ahora |
|---|---|---|
| Registro / Login | ✅ Conectado a Supabase | ✅ Igual, + guarda la sesión |
| **Mantener sesión al cerrar la app** | ❌ Se deslogueaba siempre | ✅ **Auto-login** con token persistido |
| **Cerrar sesión** | ⚠️ Solo limpiaba RAM | ✅ **Logout real** (invalida token en Supabase) |
| **Editar perfil** | ❌ Stub vacío (`TODO`) | ✅ **PUT real** a Supabase Auth + diálogo en UI |
| Puntos de agua | ❌ Lista fija (mock) | ✅ Leen/escriben en Supabase (fallback a mock) |
| Comentarios | ❌ Solo local, autor `"Vecino SJL"` | ✅ Supabase + **autor = usuario logueado** |
| Reportes | ❌ Solo local | ✅ Supabase + **foto a Storage** |
| **Foto en reportes** | ❌ No existía | ✅ **Selector de foto + subida a Storage + se muestra** |
| Noticias comunidad | ❌ 2 noticias fijas | ✅ Leen de Supabase (fallback a mock) |
| Distancia a puntos | ✅ Ya se calculaba con GPS | ✅ Igual (sin cambios) |
| API key / URL | ⚠️ Escritas en el código | ✅ En **BuildConfig** (`local.properties`) |

---

## 2. Archivos NUEVOS creados

| Archivo | Para qué sirve |
|---|---|
| `domain/AuthSession.kt` | Modelo que junta al usuario con sus tokens (access/refresh). |
| `data/local/SessionManager.kt` | Guarda/lee la sesión (token + usuario) en DataStore. Es el corazón del auto-login. |
| `data/remote/DataDtos.kt` | Objetos que mapean las filas JSON de Supabase (puntos, comentarios, reportes, noticias). |
| `SUPABASE_TABLAS.md` | Guía con el SQL de las tablas, seguridad (RLS) y configuración de fotos (Storage). |
| `CAMBIOS_REALIZADOS.md` | Este documento. |

---

## 3. Archivos MODIFICADOS (qué cambió en cada uno)

### Capa de datos remota
- **`data/remote/RetrofitClient.kt`** → la URL ahora viene de `BuildConfig.SUPABASE_URL`.
- **`data/remote/SupabaseApiService.kt`** → se agregaron endpoints:
  - Auth: `updateUser` (editar perfil), `signOut` (logout).
  - REST: `getPuntos`, `crearPunto`, `getComentarios`, `crearComentario`, `getReportes`,
    `crearReporte`, `getNoticias`.
  - Storage: `subirArchivo` (subir foto al bucket `reportes`).
- **`data/remote/RemoteDataSource.kt`** → implementa todos esos métodos, mapea DTO ↔ dominio,
  usa la API key desde `BuildConfig`, y el login ahora devuelve `AuthSession` (con tokens).
- **`data/remote/Authmodels.kt`** → se agregó `UpdateUserRequest` para editar el perfil.

### Capa de datos local
- **`data/local/DatabaseHelper.kt`** → la tabla de reportes ahora guarda `imagen_url`
  (se subió la versión de BD de 1 → 2).

### Repositorio
- **`data/repository/AppRepository.kt`** → reescrito con **offline-first**:
  - Recibe el `SessionManager` para obtener el token automáticamente.
  - `getWaterPoints / getComments / getReports / getNews`: intentan Supabase y, si falla, usan local/mock.
  - `addWaterPoint / addComment / addReport`: guardan en local **y** replican en Supabase.
  - `addReport`: si hay foto, primero la **sube a Storage** y guarda la URL.
  - Nuevos: `actualizarPerfil`, `cerrarSesionRemota`.

### ViewModels
- **`viewmodel/AuthViewModel.kt`**:
  - Recibe el `SessionManager`.
  - **Auto-login**: en el `init` revisa si hay sesión guardada (`authCheckState`).
  - `iniciarSesion` / `registrarUsuario`: **persisten la sesión** (token + datos).
  - `actualizarDatosUsuario`: ahora hace el **PUT real** a Supabase y persiste el cambio.
  - `cerrarSesion`: hace **logout real** + borra la sesión local.
- **`viewmodel/HomeViewModel.kt`**:
  - `setCurrentUser(...)`: recuerda el nombre del usuario logueado.
  - `addComment`: usa ese nombre como autor (ya no `"Vecino SJL"` fijo).
  - `addReport(report, imageBytes)`: acepta la foto opcional.

### UI / Navegación
- **`MainActivity.kt`** → crea el `SessionManager`, lo inyecta, y muestra un **splash** mientras
  decide si arrancar en Login o directo en Home (auto-login).
- **`navigation/AppNavigation.kt`** → acepta `startDestination`; informa al `HomeViewModel` quién
  es el usuario; pasa el callback para guardar el perfil.
- **`ui/ProfileScreen.kt`** → nuevo **diálogo "Editar perfil"** (nombre + teléfono), abierto desde
  el item "Editar perfil".
- **`ui/HomeScreen.kt`** → propaga `userPhone` y `onSaveProfile` a la pestaña de Perfil.
- **`ui/WaterPointDetailScreen.kt`** → el diálogo de reporte ahora tiene **"Adjuntar foto"**
  (galería), muestra vista previa, y las fotos de reportes se ven en la lista.
- **`res/values/strings.xml`** → textos nuevos para la foto del reporte.

### Configuración
- **`app/build.gradle.kts`** → activado `buildConfig`, se inyectan `SUPABASE_URL` y
  `SUPABASE_ANON_KEY` desde `local.properties` (con valores por defecto para no romper el build),
  y se agregó la dependencia **OkHttp 4.12.0** (necesaria para subir fotos).

---

## 4. Cómo funciona ahora cada flujo

### 🔐 Sesión (lo más importante que se arregló)
1. Te logueas → Supabase devuelve `access_token` → se guarda en `SessionManager` (DataStore).
2. Cierras la app y la vuelves a abrir → `AuthViewModel` detecta la sesión guardada →
   **entras directo al Home** (sin volver a loguearte).
3. Cierras sesión → se invalida el token en Supabase y se borra de tu teléfono.

### 💧 Puntos de agua / 💬 Comentarios / 📰 Noticias
- Al abrir, la app pide los datos a Supabase. Si responde, los muestra y los cachea en local.
- Si no hay internet o las tablas no existen, muestra la **caché local** o los datos de prueba.
- Al crear algo, se guarda en local **y** se intenta enviar a Supabase.

### 📸 Reportes con foto
1. Detalle del punto → **"Reportar Problema"**.
2. Eliges el tipo, escribes la descripción y (opcional) **"Adjuntar foto"** desde la galería.
3. Al enviar: la foto sube a **Storage** → se obtiene su URL → se guarda el reporte con esa URL.
4. La foto aparece en la lista de reportes del punto.

### ✏️ Editar perfil
- Perfil → **"Editar perfil"** → cambias nombre/teléfono → se actualiza en Supabase y en la app.

---

## 5. Lo que TÚ debes hacer para activarlo (en Supabase)

Todo esto está explicado paso a paso en **[SUPABASE_TABLAS.md](SUPABASE_TABLAS.md)**:

1. **Crear las tablas** (`puntos_agua`, `comentarios`, `reportes`, `noticias_comunidad`).
2. **Activar RLS** y pegar las políticas de permisos.
3. **Crear el bucket `reportes`** (público) y sus políticas → para las fotos.
4. Configurar la **confirmación de correo** como prefieras.
5. (Opcional) Poner tus credenciales en `local.properties`.

> Mientras no hagas esto, la app funciona con datos locales. En cuanto lo hagas, se conecta sola.

---

## 6. Mejoras futuras sugeridas (opcionales, "para después")

Estas NO son urgentes; la app ya funciona. Son ideas para pulir más adelante:

- **Refrescar el token** automáticamente cuando expire (usar el `refresh_token` que ya guardamos).
- **Tomar foto con la cámara** además de elegir de la galería (hoy solo galería).
- **Estados de carga/errores** al editar perfil y al subir foto (hoy es silencioso si falla).
- **Sincronización de reportes offline** (la columna `sincronizado` ya existe en la BD local).
- **Inyección de dependencias con Hilt** (hoy es manual en `MainActivity`, funciona bien igual).
- **"¿Olvidaste tu contraseña?"** (recuperación por correo de Supabase).
- Reemplazar las tarjetas de ejemplo "quemadas" en Comunidad (Lucía Rodríguez, etc.) por datos reales.

---

## 7. Cómo probar que todo quedó bien

1. **Compila:** `./gradlew assembleDebug` → debe decir `BUILD SUCCESSFUL`. ✅ (ya verificado)
2. **Sin configurar Supabase:** la app abre, muestra los 4 puntos de prueba, puedes comentar y
   reportar localmente. (Comportamiento idéntico al de antes → no se rompió nada.)
3. **Con Supabase configurado** (siguiendo `SUPABASE_TABLAS.md`):
   - Regístrate / inicia sesión → cierra la app → vuelve a abrir → **debe entrar directo** (auto-login).
   - Crea un punto → aparece en la tabla `puntos_agua` de Supabase.
   - Comenta → aparece en `comentarios` con **tu nombre** como autor.
   - Reporta con foto → la imagen aparece en **Storage → reportes** y se ve en la app.
   - Edita tu perfil → el cambio se refleja en **Authentication → Users**.

---

**Estado final:** AguaMap pasó de "solo el login conectado" a tener **toda la capa de datos
lista para Supabase** (auth con sesión persistente, puntos, comentarios, reportes con foto y
noticias), manteniendo intacta la lógica y el diseño que ya tenías. 🎉
