# 🗄️ AguaMap — Configuración de Supabase (Tablas, Seguridad y Fotos)

Esta guía tiene **todo lo que debes crear en Supabase** para que la app deje de usar
datos de prueba (mock) y empiece a guardar/leer de verdad: puntos de agua, comentarios,
reportes (con foto) y noticias.

> ✅ **No necesitas tocar el código de la app.** La app ya está programada con estos
> nombres de tablas y columnas. Solo copia y pega el SQL y crea el bucket de fotos.
>
> ⚠️ **La app no se rompe si todavía no creas esto.** Mientras las tablas no existan,
> AguaMap sigue funcionando con datos locales (offline-first). En cuanto crees las tablas,
> se conecta sola.

---

## 📍 Paso 0 — Dónde está cada cosa en Supabase

1. Entra a tu proyecto en [https://supabase.com](https://supabase.com).
2. Menú lateral:
   - **SQL Editor** → para pegar el SQL de las tablas (Pasos 1 y 2).
   - **Storage** → para crear el bucket de las fotos (Paso 3).
   - **Authentication → Providers → Email** → para la confirmación de correo (Paso 4).

---

## 🧱 Paso 1 — Crear las tablas

Abre **SQL Editor → New query**, pega TODO este bloque y pulsa **Run**.

```sql
-- ============================================================
--  AGUAMAP - ESQUEMA DE BASE DE DATOS
-- ============================================================

-- 1) PUNTOS DE AGUA -----------------------------------------
create table if not exists public.puntos_agua (
    id            text primary key,                 -- la app envía un UUID
    nombre        text not null,
    direccion     text,
    calificacion  numeric default 5,
    horario       text,
    estado        text default 'OPERATIVO',         -- OPERATIVO | MANTENIMIENTO
    tipo          text default 'FUENTE',            -- FUENTE | POZO | FILTRADA | GRIFO
    latitud       double precision,
    longitud      double precision,
    imagen_url    text,
    user_id       uuid default auth.uid(),          -- quién lo creó (automático)
    created_at    timestamptz default now()
);

-- 2) COMENTARIOS --------------------------------------------
create table if not exists public.comentarios (
    id            text primary key,                 -- la app envía un UUID
    punto_id      text not null,                    -- a qué punto pertenece
    autor         text,
    contenido     text not null,
    calificacion  int default 5,
    fecha         text,                             -- la app guarda "Hoy" o una fecha
    user_id       uuid default auth.uid(),
    created_at    timestamptz default now()
);
create index if not exists idx_comentarios_punto on public.comentarios(punto_id);

-- 3) REPORTES (con foto opcional) ---------------------------
create table if not exists public.reportes (
    id            text primary key,                 -- la app envía un UUID
    punto_id      text not null,
    tipo          text,                             -- AVERIA | SUCIO | CERRADO | OTRO
    descripcion   text,
    imagen_url    text,                             -- URL pública de la foto subida
    fecha         text,
    user_id       uuid default auth.uid(),
    created_at    timestamptz default now()
);
create index if not exists idx_reportes_punto on public.reportes(punto_id);

-- 4) NOTICIAS DE LA COMUNIDAD -------------------------------
create table if not exists public.noticias_comunidad (
    id            bigint generated always as identity primary key,
    titulo        text not null,
    contenido     text,
    fecha         text,
    created_at    timestamptz default now()
);
```

### Valores válidos (deben escribirse EXACTAMENTE así, en mayúsculas)

| Columna | Tabla | Valores permitidos |
|---|---|---|
| `estado` | puntos_agua | `OPERATIVO`, `MANTENIMIENTO` |
| `tipo` | puntos_agua | `FUENTE`, `POZO`, `FILTRADA`, `GRIFO` |
| `tipo` | reportes | `AVERIA`, `SUCIO`, `CERRADO`, `OTRO` |

> La app envía estos valores automáticamente (son los `name()` de los enums de Kotlin).
> Solo respétalos si insertas filas a mano.

---

## 🔐 Paso 2 — Activar la seguridad (RLS) y permisos

Supabase bloquea todo por defecto. Pega este segundo bloque en el **SQL Editor** y pulsa **Run**
para permitir **leer a todos** y **escribir solo a usuarios logueados**.

```sql
-- ============================================================
--  AGUAMAP - SEGURIDAD (Row Level Security)
-- ============================================================

-- Activamos RLS en todas las tablas
alter table public.puntos_agua       enable row level security;
alter table public.comentarios       enable row level security;
alter table public.reportes          enable row level security;
alter table public.noticias_comunidad enable row level security;

-- ---------- PUNTOS DE AGUA ----------
create policy "puntos_lectura_publica"
    on public.puntos_agua for select
    using (true);

create policy "puntos_insertar_logueado"
    on public.puntos_agua for insert
    to authenticated with check (true);

create policy "puntos_actualizar_logueado"
    on public.puntos_agua for update
    to authenticated using (true);

-- ---------- COMENTARIOS ----------
create policy "comentarios_lectura_publica"
    on public.comentarios for select
    using (true);

create policy "comentarios_insertar_logueado"
    on public.comentarios for insert
    to authenticated with check (true);

-- ---------- REPORTES ----------
create policy "reportes_lectura_publica"
    on public.reportes for select
    using (true);

create policy "reportes_insertar_logueado"
    on public.reportes for insert
    to authenticated with check (true);

-- ---------- NOTICIAS ----------
create policy "noticias_lectura_publica"
    on public.noticias_comunidad for select
    using (true);
```

> 💡 **¿Por qué "lectura pública"?** Para que los **invitados** (sin iniciar sesión)
> puedan ver los puntos, comentarios y noticias. Las **escrituras** (crear punto,
> comentar, reportar) requieren estar logueado, por eso usan `to authenticated`.

---

## 📸 Paso 3 — Configurar las FOTOS de los reportes (Supabase Storage)

Cuando un usuario reporta un problema, puede **adjuntar una foto**. Esa imagen se sube a
**Supabase Storage**. Sigue estos pasos:

### 3.1 Crear el bucket

1. Ve a **Storage** (menú lateral) → **New bucket**.
2. **Name:** escribe exactamente **`reportes`** (en minúsculas).
3. Marca la opción **Public bucket** (✅ activada) — así las fotos se pueden mostrar en la app.
4. Pulsa **Save**.

> El nombre `reportes` **debe ser idéntico**, porque la app sube las fotos a ese bucket
> (ver `RemoteDataSource.subirImagenReporte`).

### 3.2 Dar permisos de subida (políticas del bucket)

Ve al **SQL Editor** y ejecuta esto para permitir que un usuario **logueado** suba fotos
y que **cualquiera** pueda verlas:

```sql
-- Permitir SUBIR fotos al bucket "reportes" a usuarios logueados
create policy "reportes_subir_foto"
    on storage.objects for insert
    to authenticated
    with check (bucket_id = 'reportes');

-- Permitir VER (leer) las fotos del bucket "reportes" a cualquiera
create policy "reportes_ver_foto"
    on storage.objects for select
    using (bucket_id = 'reportes');
```

### 3.3 ¿Cómo funciona en la app? (ya está programado)

1. En la pantalla de detalle de un punto → botón **"Reportar Problema"**.
2. En el diálogo aparece **"Adjuntar foto"** → abre la galería del teléfono.
3. Al enviar, la app:
   - Sube la imagen a `storage/v1/object/reportes/<id-del-reporte>.jpg`.
   - Obtiene su **URL pública**:
     `https://<tu-proyecto>.supabase.co/storage/v1/object/public/reportes/<id>.jpg`
   - Guarda esa URL en la columna `imagen_url` de la tabla `reportes`.
4. La foto se muestra automáticamente en la lista de reportes del punto.

> 📌 La foto es **opcional**: si el usuario no adjunta nada, el reporte se guarda igual.
> Para subir fotos hay que estar **logueado** (los invitados no pueden subir por RLS).

---

## ✉️ Paso 4 — (IMPORTANTE) Confirmación de correo

Por defecto Supabase **exige confirmar el correo** antes de poder iniciar sesión. Si quieres
que el usuario entre apenas se registra (como en las pruebas):

1. Ve a **Authentication → Providers → Email**.
2. **Desactiva** "Confirm email" (Confirmar correo).
3. Guarda.

> Si lo dejas activado, tras registrarse el usuario deberá abrir el correo y confirmar
> antes de poder hacer login. La app igual lo registra, pero el token de sesión llegará
> recién después de confirmar.

---

## 🌱 (Opcional) Paso 5 — Datos de ejemplo

Si quieres ver puntos reales en el mapa de una vez (en lugar de los 4 de prueba locales),
inserta estos en el **SQL Editor**:

```sql
insert into public.puntos_agua (id, nombre, direccion, calificacion, horario, estado, tipo, latitud, longitud) values
('11111111-1111-1111-1111-111111111111', 'Fuente Los Postes', 'Paradero Los Postes, SJL', 4.8, '24h', 'OPERATIVO', 'FUENTE', -11.9904, -77.0006),
('22222222-2222-2222-2222-222222222222', 'Punto Eco-Filter Zárate', 'Av. Gran Chimú 452', 4.5, '08:00 - 22:00', 'OPERATIVO', 'FILTRADA', -12.0225, -77.0012),
('33333333-3333-3333-3333-333333333333', 'Pozo Huiracocha', 'Parque Zonal Huiracocha', 4.2, 'Cerrado', 'MANTENIMIENTO', 'POZO', -11.9961, -76.9958),
('44444444-4444-4444-4444-444444444444', 'Grifo Caja de Agua', 'Estación Caja de Agua', 4.9, '24h', 'OPERATIVO', 'GRIFO', -12.0272, -77.0142);

insert into public.noticias_comunidad (titulo, contenido, fecha) values
('Nueva red de fuentes en Zárate', 'Se han instalado 15 nuevas fuentes de agua potable cerca a la Av. Gran Chimú.', '2026-03-28'),
('Mantenimiento Programado', 'Sedapal realizará limpieza de reservorios en Campoy este fin de semana.', '2026-04-02');
```

> Si insertas puntos con UUID (como arriba), los comentarios y reportes sobre ESOS puntos
> sí se guardarán en Supabase. Los 4 puntos de prueba locales (id "1".."4") son solo
> respaldo offline y no se sincronizan.

---

## 🔑 (Recomendado) Paso 6 — Mover tus credenciales a `local.properties`

La app ahora lee la URL y la API key desde `local.properties` (que **no** se sube a Git).
Añade estas dos líneas a tu archivo `local.properties` (en la raíz del proyecto):

```properties
SUPABASE_URL=
SUPABASE_ANON_KEY=
```

> Si no las pones, la app usa por defecto las que ya tenías (no se rompe nada). Pero
> ponerlas aquí es más seguro y profesional. La **URL debe terminar en `/`**.

---

## ✅ Checklist final

- [ ] Paso 1: tablas creadas (`puntos_agua`, `comentarios`, `reportes`, `noticias_comunidad`).
- [ ] Paso 2: RLS activado + políticas creadas.
- [ ] Paso 3: bucket **`reportes`** creado (público) + políticas de Storage.
- [ ] Paso 4: confirmación de correo configurada como prefieras.
- [ ] Paso 6: credenciales en `local.properties` (opcional pero recomendado).

Cuando termines este checklist, **AguaMap quedará 100% conectada a Supabase.** 🎉
