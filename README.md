# 🌊 AguaMap - Android App

¡Bienvenido a **AguaMap**! Este proyecto está construido con **Kotlin** y **Jetpack Compose**, siguiendo las mejores prácticas de desarrollo moderno en Android.

## 🏗️ Estructura del Proyecto

El proyecto utiliza una arquitectura de capas para separar responsabilidades, facilitar las pruebas y el mantenimiento.

### 📁 1. Frontend (Capa de Interfaz de Usuario)
Ubicada principalmente en `ui/` y gestionada por los `ViewModels`.
- **`com.aguamap.app.ui`**: Aquí encontrarás tus *Composables*.
    - **`theme/`**: Define la identidad visual (colores, tipografía y formas).
    - *Sugerencia*: Aquí deberías crear carpetas por pantalla (ej. `home/`, `map/`, `profile/`).
- **`com.aguamap.app.viewmodel`**: Actúa como el puente entre la lógica y la UI. Aquí es donde vive el estado de la pantalla.

### 🧠 2. Lógica de Negocio (Capa de Dominio)
- **`com.aguamap.app.domain`**: Es el "corazón" de la app.
    - **Modelos**: Tus clases de datos puras (Entities).
    - **Use Cases (Casos de Uso)**: Clases que contienen la lógica específica de una acción (ej. `GetWaterPointsUseCase`).

### 💾 3. Base de Datos y Red (Capa de Datos / Backend)
Todo lo relacionado con el origen de los datos reside en `data/`.
- **`com.aguamap.app.data.local`**: Aquí va la **Base de Datos local** (usualmente Room). Contiene los DAOs y la configuración de la DB.
- **`com.aguamap.app.data.remote`**: Aquí va la conexión con el **Backend/API**. Contiene las interfaces de Retrofit o el cliente de Firebase.
- **`com.aguamap.app.data.repository`**: El **Repositorio** decide si los datos vienen de la nube o de la base de datos local.

### 🚀 4. Navegación y Rutas
- **`com.aguamap.app.navigation`**: Aquí se definen las rutas de la aplicación, el `NavHost` y los destinos para moverte entre pantallas.

---

## 🛠️ Resumen de Carpetas

| Carpeta | Responsabilidad | Tipo de Contenido |
| :--- | :--- | :--- |
| **`ui/`** | Frontend | Composables, Temas, Componentes visuales. |
| **`viewmodel/`** | Controladores | Gestión de estado y eventos de UI. |
| **`domain/`** | Lógica de Negocio | Modelos de negocio, Interfaces, Casos de uso. |
| **`data/`** | Backend/Persistencia | Implementación de Repositorios, API, DB Local. |
| **`navigation/`** | Rutas | Definición de grafos de navegación. |

---

## 🚦 Punto de Entrada
La actividad principal es **`MainActivity.kt`**, encargada de inicializar el tema de la aplicación y el motor de navegación principal.

---
⭐ *Desarrollado con ❤️ en Kotlin*
