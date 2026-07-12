# AguaMap

Aplicación Android nativa para la localización colaborativa de puntos de abastecimiento
de agua potable en el distrito de San Juan de Lurigancho (SJL), Lima. Permite a los vecinos
encontrar fuentes de agua confiables, reportar averías con evidencia fotográfica y validación
por GPS, y participar en una comunidad de "Guardianes del Agua".

Proyecto de ingeniería de software con enfoque en los Objetivos de Desarrollo Sostenible
(ODS 6: Agua Limpia y Saneamiento; ODS 12: Producción y Consumo Responsables).

## Descripción

AguaMap centraliza información georreferenciada sobre puntos de agua (fuentes, pozos, agua
filtrada, grifos), su estado operativo, horarios y valoración de la comunidad. Está construida
con una estrategia offline-first: la app sigue funcionando sin conexión usando una caché local
y sincroniza con el backend cuando se recupera la red.

## Características principales

- Autenticación con Supabase Auth (tokens JWT con refresco automático) y tres roles:
  Invitado (solo lectura), Ciudadano y Administrador.
- Registro con validación por campo (DNI de 8 dígitos, teléfono de 9 dígitos, correo con
  formato válido, contraseña mínima de 6 caracteres) y aceptación de Términos y Condiciones.
- Mapa interactivo con MapLibre GL, marcadores georreferenciados y trazado de ruta a pie
  por calles mediante OSRM.
- Ficha de detalle del punto con estado, horario, tipo, calificación promedio, coordenadas,
  comentarios y reportes.
- Filtro por tipo y por sector de SJL, búsqueda por nombre y registro de nuevos puntos
  (rol Administrador).
- Reporte de averías con foto (cámara nativa o galería, con manejo de permisos) y validación
  de proximidad GPS menor a 100 metros mediante la fórmula de Haversine.
- Cola de reportes offline en SQLite con sincronización diferida mediante WorkManager.
- Valoración de puntos del 1 al 5 (un voto por usuario, modificable; el promedio se recalcula
  automáticamente en el servidor) y comentarios de texto con moderación léxica automática.
- Módulo comunidad con noticias/alertas y reportes recientes.
- Perfil con métricas reales del usuario (reportes y comentarios) y preferencias persistidas
  en DataStore (sector, radio de búsqueda, modo oscuro y alto contraste).

## Arquitectura

El proyecto sigue el patrón MVVM con una capa de repositorio como única fuente de verdad,
que decide entre datos remotos (Supabase) y la caché local (offline-first). La inyección de
dependencias es manual (se construyen en `MainActivity`), sin frameworks de DI.

Capas principales:

- UI: pantallas en Jetpack Compose (`ui/`) y navegación con Navigation Compose.
- ViewModel: gestión de estado con `StateFlow` (`viewmodel/`).
- Repositorio: orquestación de datos y lógica offline-first (`data/repository/`).
- Datos remotos: consumo de la API REST de Supabase con Retrofit (`data/remote/`).
- Datos locales: caché en SQLite y preferencias/sesión en DataStore (`data/local/`).

## Persistencia de datos

- Local, caché offline (SQLite con `SQLiteOpenHelper`): tablas `puntos_agua`,
  `noticias_comunidad`, `reportes_locales` (con bandera de sincronización) y
  `comentarios_cache`.
- Local, sesión y preferencias (Jetpack DataStore): tokens de sesión y ajustes de usuario.
- Remoto (Supabase / PostgreSQL): tablas `perfiles`, `puntos_agua`, `comentarios`, `reportes`,
  `noticias_comunidad`, `favoritos` y `valoraciones`, más Supabase Storage para las fotos de
  los reportes. Se consume por REST directamente con Retrofit (sin SDK de Supabase).

## Stack tecnológico

- Lenguaje: Kotlin 2.2.10 (Coroutines y Flow).
- UI: Jetpack Compose (BOM 2026.02.01) con Material 3.
- Navegación: Navigation Compose 2.9.8.
- Mapas: MapLibre GL (android-sdk-opengl) 13.2.0.
- Ubicación: play-services-location 21.3.0 (Fused Location) + coroutines-play-services.
- Red: Retrofit 2.11.0 + converter-gson, OkHttp 4.12.0 (Supabase Auth, REST y Storage).
- Imágenes: Coil 2.7.0.
- Persistencia local: SQLite nativo (`SQLiteOpenHelper`) y DataStore Preferences 1.2.1.
- Sincronización en segundo plano: WorkManager 2.10.0.
- Rutas: servicio externo OSRM (API pública).
- Backend: Supabase (PostgreSQL, Auth y Storage).

## Build

- Gradle 9.4.1, Android Gradle Plugin 9.1.0, Java 11.
- minSdk 24 (Android 7.0), target/compileSdk 36.
- Package: `com.aguamap.app`.

## Cómo ejecutar

1. Clonar el repositorio y abrirlo en Android Studio.
2. Crear un archivo `local.properties` en la raíz del proyecto con las credenciales de Supabase
   (la URL debe terminar en `/`):

   ```properties
   SUPABASE_URL=https://<tu-proyecto>.supabase.co/
   SUPABASE_ANON_KEY=<tu-anon-key>
   ```

   Si no se definen, la app compila igual y funciona con la caché local (offline-first).
3. Configurar la base de datos en Supabase siguiendo `SUPABASE_TABLAS.md` (tablas, seguridad
   RLS y bucket de fotos) y `SUPABASE_ACTUALIZACIONES.md` (roles, favoritos y valoraciones).
4. Ejecutar la app desde Android Studio en un emulador o dispositivo con Android 7.0 o superior.

## Documentación del repositorio

- `SUPABASE_TABLAS.md`: estado actual del esquema de la base de datos.
- `SUPABASE_ACTUALIZACIONES.md`: migraciones y funciones adicionales (roles, favoritos, valoraciones).
- `VERSIONES.md`: versiones de librerías y del entorno de build.
- `ROLES_Y_PERMISOS.md`: modelo de roles y permisos.

## Estado del proyecto

Prototipo funcional en desarrollo, con fines académicos e informativos. La información de
puntos de agua se genera de forma colaborativa. La aplicación no tiene relación oficial con
SEDAPAL ni con entidades gubernamentales.

