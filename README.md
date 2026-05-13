# 💧 AguaMap: Resiliencia Hídrica y Gestión Comunitaria en SJL

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-blue.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Modern_UI-green.svg)](https://developer.android.com/jetpack/compose)
[![Clean Architecture](https://img.shields.io/badge/Architecture-Clean-orange.svg)](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
[![License](https://img.shields.io/badge/Social_Impact-ODS_6_%26_12-blue.svg)]()

## 📝 1. Resumen Ejecutivo
**AguaMap** es una plataforma nativa de Android diseñada para mitigar el estrés hídrico en el distrito de San Juan de Lurigancho (SJL), Lima. A través de un ecosistema colaborativo y georreferenciado, la aplicación permite la localización de puntos de abastecimiento de agua potable, promoviendo la transparencia en la disponibilidad del recurso y la educación ciudadana bajo el marco de los **Objetivos de Desarrollo Sostenible (ODS 6: Agua Limpia y Saneamiento y ODS 12: Producción y Consumo Responsables)**.

---

## 🚀 2. Capacidades Operativas y Funcionales

### 📍 Inteligencia Geoespacial y Navegación
*   **Geolocalización en Tiempo Real:** Integración con `FusedLocationProviderClient` para un rastreo de alta precisión de la ubicación del usuario.
*   **Métricas de Proximidad:** Implementación de la **Fórmula de Haversine** para el cálculo dinámico de distancias, permitiendo al usuario identificar el punto de suministro más cercano de forma instantánea.
*   **Cartografía Dinámica:** Motor de renderizado basado en **MapLibre GL**, optimizado para el despliegue de marcadores interactivos y seguimiento del usuario en tiempo real.

### 👤 Experiencia de Usuario (UX) de Alto Impacto
*   **Diseño "Ocean & Cloud":** Interfaz fluida basada en Material 3, optimizada para la legibilidad en condiciones de campo y alta luminosidad.
*   **Gestión Multimedia:** Sistema de carga asíncrona de imágenes mediante **Coil** para la verificación visual y validación de los puntos de hidratación.
*   **Accesibilidad Universal:** Soporte nativo para modos de alto contraste y localización total al español, asegurando la inclusión de todos los sectores de la población.

---

## 🏗️ 3. Arquitectura de Software y Estrategia de Datos

El sistema se rige bajo los principios de **Clean Architecture** y el patrón **MVVM**, garantizando un código desacoplado, mantenible y preparado para la escala industrial.

### 🗄️ Estrategia de Persistencia Híbrida (Offline-First)
Para responder a los desafíos de conectividad intermitente en las zonas periféricas de SJL, AguaMap implementa una persistencia relacional de dos niveles:

#### Nivel 1: Capa de Resiliencia Local (SQLite)
Utiliza un motor relacional embebido para gestionar el almacenamiento persistente en el dispositivo.
*   **Tabla `puntos_agua`:** Caché de mapa para disponibilidad inmediata de marcadores.
*   **Tabla `noticias_comunidad`:** Almacenamiento de alertas de cortes de Sedapal y tips de ahorro (ODS 12).
*   **Tabla `reportes_locales`:** Buffer de incidencias para envío asíncrono una vez recuperada la conexión.
*   **Tabla `detalles_puntos` y `comentarios_cache`:** Información técnica y social para la toma de decisiones informada sin dependencia de red.

#### Nivel 2: Fuente de Verdad Centralizada (PostgreSQL)
Implementada en la nube para unificar los reportes de todos los "Guardianes del Agua", permitiendo la actualización global de estados de suministro y validación comunitaria.

---

## 🛠️ 4. Stack Tecnológico Profesional
*   **Lenguaje:** Kotlin (Coroutines & Flow para gestión de estados asíncronos).
*   **UI Framework:** Jetpack Compose (Modern Declarative UI).
*   **Inyección de Dependencias:** Estructura preparada para Hilt/Dagger.
*   **Mapas:** MapLibre SDK v10 (Vector Tiles).
*   **Backend-as-a-Service:** Supabase (PostgreSQL, Auth & Storage).

---

## 🚢 5. Hoja de Ruta a Producción y Escalabilidad ($0 Cost)

El despliegue de AguaMap está diseñado para ser sostenible sin requerir inversión inicial en infraestructura mediante el uso de tecnologías *Cloud-Native* gratuitas:

1.  **Infraestructura:** Despliegue de la base de datos en **Supabase** (PostgreSQL de nivel empresarial).
2.  **Media Hosting:** Almacenamiento de evidencias fotográficas en **Supabase Storage**.
3.  **Pipeline CI/CD:** Automatización de builds y calidad de código mediante **GitHub Actions**.
4.  **Distribución Estratégica:** Fase de *Closed Beta* a través de **Firebase App Distribution** para un despliegue controlado en comunidades seleccionadas de SJL antes del lanzamiento en Google Play Store.

---

## 🏁 6. Impacto Social y Conclusión
AguaMap no es solo una aplicación, es una herramienta de **Gobernanza Ciudadana**. Al centralizar la información sobre la disponibilidad de agua, reducimos el tiempo de desplazamiento, combatimos la especulación de precios y empoderamos al ciudadano con datos reales. Esta arquitectura garantiza que, incluso sin señal de internet, la información vital siga fluyendo.
