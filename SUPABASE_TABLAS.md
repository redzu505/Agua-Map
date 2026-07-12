# 🗄️ AguaMap — Base de Datos en Supabase (Estado Actual)

Este documento refleja **las tablas que existen HOY** en tu proyecto de Supabase.
Es el "estado real" de la base de datos.

> 📌 Para lo que **falta o hay que actualizar** (columna de rol, favoritos y la nueva
> valoración de puntos), mira **[SUPABASE_ACTUALIZACIONES.md](SUPABASE_ACTUALIZACIONES.md)**.
>
> ⚠️ **La app no se rompe si falta algo.** AguaMap es *offline-first*: mientras una tabla
> no exista, sigue funcionando con datos locales y se conecta sola cuando la creas.

---

## 📊 Tablas actuales

| Tabla | ¿Para qué sirve? | ¿La usa la app? |
|---|---|---|
| `perfiles` | Datos del usuario (nombre, dni, teléfono, username) | ⚠️ Parcial (ver nota) |
| `puntos_agua` | Puntos de hidratación del mapa | ✅ Sí |
| `comentarios` | Comentarios de la comunidad por punto | ✅ Sí |
| `reportes` | Reportes de avería (con foto opcional) | ✅ Sí |
| `noticias_comunidad` | Noticias de la pantalla Comunidad | ✅ Sí |

---

## 🧱 Esquema real (SQL)

```sql
-- 1) PERFILES ------------------------------------------------
-- Guarda los datos personales del usuario. Su id ES el uuid de auth.users.
create table public.perfiles (
    id              uuid not null,
    nombre_completo text not null,
    dni             text,
    telefono        text,
    username        text,
    constraint perfiles_pkey primary key (id),
    constraint perfiles_id_fkey foreign key (id) references auth.users(id)
);

-- 2) PUNTOS DE AGUA -----------------------------------------
create table public.puntos_agua (
    id            text not null,
    nombre        text not null,
    direccion     text,
    calificacion  numeric default 5,
    horario       text,
    estado        text default 'OPERATIVO',   -- OPERATIVO | MANTENIMIENTO
    tipo          text default 'FUENTE',       -- FUENTE | POZO | FILTRADA | GRIFO
    latitud       double precision,
    longitud      double precision,
    imagen_url    text,
    user_id       uuid default auth.uid(),     -- quién lo creó (automático)
    created_at    timestamptz default now(),
    constraint puntos_agua_pkey primary key (id)
);

-- 3) COMENTARIOS --------------------------------------------
create table public.comentarios (
    id            text not null,
    punto_id      text not null,
    autor         text,
    contenido     text not null,
    calificacion  integer default 5,           -- (la app ya no muestra estrellas aquí)
    fecha         text,
    user_id       uuid default auth.uid(),
    created_at    timestamptz default now(),
    constraint comentarios_pkey primary key (id)
);

-- 4) REPORTES (con foto opcional) ---------------------------
create table public.reportes (
    id            text not null,
    punto_id      text not null,
    tipo          text,                         -- AVERIA | SUCIO | CERRADO | OTRO
    descripcion   text,
    imagen_url    text,                         -- URL pública de la foto subida
    fecha         text,
    user_id       uuid default auth.uid(),
    created_at    timestamptz default now(),
    constraint reportes_pkey primary key (id)
);

-- 5) NOTICIAS DE LA COMUNIDAD -------------------------------
create table public.noticias_comunidad (
    id            bigint generated always as identity primary key,
    titulo        text not null,
    contenido     text,
    fecha         text,
    created_at    timestamptz default now()
);
```

### Valores válidos (escribir EXACTO, en mayúsculas)

| Columna | Tabla | Valores permitidos |
|---|---|---|
| `estado` | puntos_agua | `OPERATIVO`, `MANTENIMIENTO` |
| `tipo` | puntos_agua | `FUENTE`, `POZO`, `FILTRADA`, `GRIFO` |
| `tipo` | reportes | `AVERIA`, `SUCIO`, `CERRADO`, `OTRO` |

> La app envía estos valores automáticamente (son los `name()` de los enums de Kotlin).

---

## ⚠️ Notas importantes sobre el estado actual

1. **`perfiles` NO tiene columna `rol`.** La app intenta leer el rol del usuario con
   `GET perfiles?select=rol` ([PerfilDto](app/src/main/java/com/aguamap/app/data/remote/DataDtos.kt#L62)).
   Como esa columna no existe, la consulta falla y **todos quedan como `'usuario'`** por defecto.
   → Consecuencia: **nadie es admin**, y si tienes las políticas de "solo admin crea puntos",
   nadie podrá crear puntos de agua. **Solución en [SUPABASE_ACTUALIZACIONES.md](SUPABASE_ACTUALIZACIONES.md).**

2. **No existe la tabla `favoritos`.** La app la usa para "puntos guardados", pero al no estar,
   los guardados solo viven en el teléfono (offline) y **no se sincronizan** con la nube.
   → Opcional crearla (ver actualizaciones).

3. **La app NO escribe en `perfiles` al registrarse.** Envía `nombre/dni/telefono/username`
   como *user_metadata* de Supabase Auth. Si tu tabla `perfiles` se llena, es por un trigger
   propio que tengas configurado (o está vacía).

---

## 🔐 Seguridad (RLS)

Recuerda que Supabase bloquea todo por defecto. Para que la app lea/escriba necesitas RLS
activado y políticas de **lectura pública** (para invitados) y **escritura para logueados**.
El detalle de políticas está en **[SUPABASE_ACTUALIZACIONES.md](SUPABASE_ACTUALIZACIONES.md)**.

---

## 📸 Fotos de los reportes (Storage)

Los reportes pueden llevar foto (cámara o galería). Requiere un bucket llamado exactamente
**`reportes`** (público) en **Storage**, con políticas de subida para logueados y lectura
pública. La app sube a `storage/v1/object/reportes/<id>.jpg` y guarda la URL en
`reportes.imagen_url`.

---

## ✉️ Confirmación de correo

En **Authentication → Providers → Email** puedes desactivar "Confirm email" si quieres que el
usuario entre apenas se registra (útil en pruebas). Si lo dejas activado, el token de sesión
llega recién después de confirmar el correo.

---

## 🔑 Credenciales

La app lee `SUPABASE_URL` y `SUPABASE_ANON_KEY` desde `local.properties` (no se sube a Git).
La **URL debe terminar en `/`**. Si no las pones, usa las de por defecto.
