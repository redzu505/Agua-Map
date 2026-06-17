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

-- 5) FAVORITOS / PUNTOS GUARDADOS ---------------------------
-- Cada fila es "el usuario X guardó el punto Y". El user_id se llena solo
-- con auth.uid(). La restricción unique evita guardar el mismo punto dos veces.
create table if not exists public.favoritos (
    id            bigint generated always as identity primary key,
    user_id       uuid default auth.uid(),
    punto_id      text not null,
    created_at    timestamptz default now(),
    unique (user_id, punto_id)
);
create index if not exists idx_favoritos_user on public.favoritos(user_id);
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
alter table public.favoritos         enable row level security;

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

-- ---------- FAVORITOS (cada usuario SOLO ve y gestiona los suyos) ----------
create policy "favoritos_leer_propios"
    on public.favoritos for select
    to authenticated using (user_id = auth.uid());

create policy "favoritos_insertar_propios"
    on public.favoritos for insert
    to authenticated with check (user_id = auth.uid());

create policy "favoritos_borrar_propios"
    on public.favoritos for delete
    to authenticated using (user_id = auth.uid());
```

> 💡 **¿Por qué "lectura pública"?** Para que los **invitados** (sin iniciar sesión)
> puedan ver los puntos, comentarios y noticias. Las **escrituras** (crear punto,
> comentar, reportar) requieren estar logueado, por eso usan `to authenticated`.
>
> 🔒 **Favoritos:** son privados. Cada usuario solo puede ver, guardar y borrar
> **sus propios** puntos guardados (por eso las políticas usan `user_id = auth.uid()`).

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

## 👮 Paso 7 — Roles: que SOLO el administrador cree puntos

La app distingue 3 roles: **invitado** (mira y filtra), **usuario** (comenta y reporta) y
**admin** (además **crea/edita/borra puntos de agua**). El rol vive en una tabla `perfiles`.

> Este bloque es **aditivo y seguro**: agrega la tabla `perfiles` y **cambia las políticas de
> `puntos_agua`** para que solo el admin escriba. No borra datos. Pégalo completo en el **SQL Editor**.

```sql
-- ============================================================
--  AGUAMAP - ROLES (admin / usuario)
-- ============================================================

-- 1) Tabla de perfiles con el rol de cada usuario
create table if not exists public.perfiles (
    id        uuid primary key references auth.users(id) on delete cascade,
    rol       text not null default 'usuario',   -- 'usuario' | 'admin'
    creado_en timestamptz default now()
);

-- 2) Al registrarse, se crea su perfil automáticamente como 'usuario'
create or replace function public.crear_perfil_nuevo()
returns trigger language plpgsql security definer as $$
begin
    insert into public.perfiles (id, rol) values (new.id, 'usuario')
    on conflict (id) do nothing;
    return new;
end; $$;

drop trigger if exists al_registrarse_crear_perfil on auth.users;
create trigger al_registrarse_crear_perfil
    after insert on auth.users for each row execute function public.crear_perfil_nuevo();

-- 3) Backfill: crea el perfil de los usuarios que YA estaban registrados
insert into public.perfiles (id, rol)
select id, 'usuario' from auth.users
on conflict (id) do nothing;

-- 4) RLS: cada quien puede leer SU propio perfil (así la app lee su rol)
alter table public.perfiles enable row level security;
drop policy if exists "perfil_leer_propio" on public.perfiles;
create policy "perfil_leer_propio" on public.perfiles
    for select to authenticated using (id = auth.uid());

-- 5) Helper: ¿el usuario actual es admin?
create or replace function public.es_admin() returns boolean
language sql security definer as $$
    select exists(select 1 from public.perfiles where id = auth.uid() and rol = 'admin');
$$;

-- 6) puntos_agua: la lectura sigue pública, pero crear/editar/borrar = SOLO ADMIN
drop policy if exists "puntos_insertar_logueado" on public.puntos_agua;
drop policy if exists "puntos_actualizar_logueado" on public.puntos_agua;
create policy "puntos_admin_insert" on public.puntos_agua
    for insert to authenticated with check (public.es_admin());
create policy "puntos_admin_update" on public.puntos_agua
    for update to authenticated using (public.es_admin());
create policy "puntos_admin_delete" on public.puntos_agua
    for delete to authenticated using (public.es_admin());
```

### 👑 Cómo nombrar a un administrador

Un usuario se vuelve admin **solo desde aquí** (no desde la app, por seguridad). Hay 2 formas:

**Opción A — SQL (por correo):**
```sql
update public.perfiles set rol = 'admin'
where id = (select id from auth.users where email = 'CORREO_DEL_ADMIN@gmail.com');
```

**Opción B — Table Editor (visual):** Table Editor → tabla `perfiles` → busca la fila del
usuario → cambia `rol` de `usuario` a `admin` → Guarda.

> 🔎 Para saber el correo/`id` de cada usuario: **Authentication → Users**.

> ⚠️ **Importante:** mientras NADIE sea admin, **nadie verá el botón de crear puntos** en la app
> (es a propósito: "fail-closed"). Marca a tu cuenta como admin con el paso de arriba para empezar.

---

## ✅ Checklist final

- [ ] Paso 1: tablas creadas (`puntos_agua`, `comentarios`, `reportes`, `noticias_comunidad`, `favoritos`).
- [ ] Paso 2: RLS activado + políticas creadas.
- [ ] Paso 3: bucket **`reportes`** creado (público) + políticas de Storage.
- [ ] Paso 4: confirmación de correo configurada como prefieras.
- [ ] Paso 6: credenciales en `local.properties` (opcional pero recomendado).
- [ ] Paso 7: tabla `perfiles` + políticas de admin creadas, y **tu cuenta marcada como `admin`**.

Cuando termines este checklist, **AguaMap quedará 100% conectada a Supabase.** 🎉
