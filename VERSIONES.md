# 🧱 AguaMap — Versiones del proyecto

Resumen de las versiones **importantes** que usa el proyecto, dónde se configuran y para qué.
Última actualización: 2026-06-17.

---

## ⚙️ Plataforma y build

| Qué | Versión | Dónde se define |
|---|---|---|
| **Gradle** | `9.4.1` | `gradle/wrapper/gradle-wrapper.properties` |
| **Android Gradle Plugin (AGP)** | `9.1.0` | `gradle/libs.versions.toml` → `agp` |
| **Kotlin** | `2.2.10` | `gradle/libs.versions.toml` → `kotlin` |
| **Java (compatibility)** | `11` | `app/build.gradle.kts` → `compileOptions` |
| **compileSdk** | `36` (minorApiLevel 1) | `app/build.gradle.kts` |
| **targetSdk** | `36` | `app/build.gradle.kts` |
| **minSdk** | `24` (Android 7.0) | `app/build.gradle.kts` |
| **App** | `versionName 1.0` · `versionCode 1` | `app/build.gradle.kts` |
| **Package / applicationId** | `com.aguamap.app` | `app/build.gradle.kts` |

> ⚠️ **Nota de equipo (AGP):** en el repo (Git) el `agp` figura como `9.2.1`, pero localmente
> se usa **`9.1.0`** para que compile con la versión de Android Studio instalada. Si a alguien
> no le compila, alinear el `agp` de `libs.versions.toml` con su Android Studio.

---

## 🎨 UI (Jetpack Compose)

| Librería | Versión | Para qué |
|---|---|---|
| **Compose BOM** | `2026.02.01` | Fija las versiones de todo Compose (UI, Material3, etc.) |
| Material 3 | (vía BOM) | Componentes de UI |
| material-icons-extended | `1.7.5` | Íconos |
| activity-compose | `1.13.0` | Integración Activity ↔ Compose |
| navigation-compose | `2.9.8` | Navegación entre pantallas |
| lifecycle-runtime-ktx | `2.10.0` | Ciclo de vida / ViewModel |
| core-ktx | `1.18.0` | Utilidades base de Android |
| graphics-path | `1.1.0` | Soporte de gráficos/paths |

> Las versiones de Compose salen del **BOM**, no se fijan una por una.

---

## 🌐 Datos y red (Supabase)

| Librería | Versión | Para qué |
|---|---|---|
| **Retrofit** | `2.11.0` | Cliente HTTP hacia Supabase (Auth + REST + Storage) |
| converter-gson | `2.11.0` | Convierte JSON ↔ objetos Kotlin |
| **OkHttp** | `4.12.0` | Motor HTTP (subir fotos a Storage con `RequestBody`) |
| logging-interceptor | `4.12.0` | Logs de red en Logcat (tag `AGUAMAP_NET`) |
| DataStore Preferences | `1.2.1` | Sesión, preferencias y favoritos (local) |

> **Backend:** Supabase (Postgres + Auth + Storage). No se usa SDK de Supabase: se consume su
> **API REST** directamente con Retrofit. **SQLite** (caché offline) es nativo de Android,
> no es una dependencia externa.

---

## 🗺️ Mapa, ubicación e imágenes

| Librería | Versión | Para qué |
|---|---|---|
| **MapLibre** (android-sdk-opengl) | `13.2.0` | Mapa |
| play-services-location | `21.3.0` | Ubicación GPS |
| kotlinx-coroutines-play-services | `1.8.0` | Usar la ubicación con corrutinas |
| **Coil** (coil-compose) | `2.7.0` | Cargar imágenes (fotos de reportes, etc.) |

> **Rutas por calle:** se usa el servicio externo **OSRM** (`router.project-osrm.org`) — es una
> API pública, no una librería con versión.

---

## 🧪 Tests

| Librería | Versión |
|---|---|
| JUnit | `4.13.2` |
| androidx.test.ext:junit | `1.1.5` |
| espresso-core | `3.5.1` |

---

## 📌 Resumen rápido (lo más importante)

- **Gradle 9.4.1 · AGP 9.1.0 · Kotlin 2.2.10 · Java 11**
- **minSdk 24 · target/compileSdk 36**
- **Compose BOM 2026.02.01 · Material 3**
- **Retrofit 2.11.0 + OkHttp 4.12.0** (Supabase REST) · **DataStore 1.2.1**
- **MapLibre 13.2.0 · Coil 2.7.0 · play-services-location 21.3.0**

> Para cambiar versiones: la mayoría están en **`gradle/libs.versions.toml`**; las de red
> (Retrofit/OkHttp/Coil) y servicios Google están escritas directo en **`app/build.gradle.kts`**.
