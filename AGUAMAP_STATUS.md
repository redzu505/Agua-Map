# 🌊 AguaMap - Estado del Proyecto (SJL Edition)

## 📋 Resumen Operativo
AguaMap es una aplicación nativa diseñada para centralizar la localización de puntos de hidratación en **San Juan de Lurigancho**. Actualmente, el proyecto se encuentra en una fase de **Frontend Estabilizado** con una arquitectura escalable.

---

## 🎨 Interfaz de Usuario (Frontend)
Se ha implementado una estética **"Ocean & Cloud"** (Azul Glacial, Celeste y Blanco) enfocada en la pureza y frescura.

### Pantallas Funcionales:
- **Login / Registro:** Flujo completo de autenticación visual con acceso para invitados. Diseño optimizado con `OutlinedTextFields` personalizados.
- **Mapa Interactivo:** Integración con **MapLibre v10** y **OpenFreeMap**. Centrado automáticamente en SJL (-11.9763, -77.0002) con gestos habilitados.
- **Lista de Puntos:** Buscador y filtros dinámicos (Fuentes, Pozos, Agua Filtrada, Grifos) con tarjetas informativas detalladas.
- **Comunidad SJL:** Feed en tiempo real con reportes de zonas específicas (Los Postes, Zárate, Campoy). Incluye "Tip del Día" y noticias de infraestructura.
- **Perfil de Guardián:** Sistema de métricas de impacto (litros ahorrados y botellas evitadas) para incentivar el uso responsable.

---

## ⚙️ Backend y Datos (Arquitectura)
El proyecto sigue los principios de **Clean Architecture**:
- **UI:** Implementada al 100% con Jetpack Compose.
- **Domain:** Modelos definidos (`WaterPoint`, `WaterPointStatus`, `WaterPointType`).
- **Data:** 
    - **Mock Data:** Datos localizados en SJL para pruebas.
    - **RemoteDataSource:** Estructura base preparada para integración con API REST (Retrofit/Ktor).
    - **LocalDataSource:** Preparado para persistencia (Room).

---

## 🚀 Próximos Pasos y Propuestas

### Implementación Técnica (Inmediato):
1.  **Puntos Dinámicos:** Migrar los puntos de agua de una lista estática a marcadores reales en el mapa usando coordenadas Lat/Lng.
2.  **Geolocalización:** Calcular la distancia real en tiempo real entre el usuario y el punto de agua más cercano.
3.  **Backend Real:** Implementar una API (Firebase o Node.js) para que los reportes de la comunidad se guarden y compartan de verdad.

### Ideas para Agregar Valor:
-  **Alertas de Corte:** Notificaciones push cuando Sedapal programe cortes de agua en zonas específicas de SJL.
-  **QR en Fuentes:** Sistema para escanear un código QR en la fuente física y "check-in" para sumar puntos al perfil.
-  **Calidad del Agua:** Permitir que los usuarios califiquen la presión y el sabor del agua en cada punto.
-  **Modo Offline:** Descarga de mapas de la zona para encontrar agua incluso sin datos móviles.

---
**Estado Actual:** `90% UI / 10% Backend`
**Foco:** Localización SJL, Perú.
