# 🆕 AguaMap — Actualizaciones de Base de Datos

Este documento reúne **lo que hay que agregar/actualizar** en Supabase respecto al estado
actual descrito en **[SUPABASE_TABLAS.md](SUPABASE_TABLAS.md)**.

- **Fecha:** 2026-07-12
- **Regla general:** todo el SQL es **aditivo y seguro** (usa `if not exists` / `add column if not exists`).
  No borra datos. Pégalo en **Supabase → SQL Editor → Run**.

| # | Actualización | ¿Para qué? | ¿Obligatorio? |
|---|---|---|---|
| 1 | Columna `rol` en `perfiles` | Que funcionen los roles y el admin cree puntos | Recomendado |
| 2 | Tabla `favoritos` | Sincronizar los puntos guardados con la nube | Opcional |
| 3 | Tabla `valoraciones` + trigger | Que los usuarios puntúen puntos (1–5) y suba/baje el promedio | **Nueva función** |

---

## 1️⃣ Arreglar los ROLES — columna `rol` en `perfiles`

**Problema:** la app lee `perfiles.rol`, pero tu tabla no la tiene → todos quedan como
`'usuario'` y nadie puede ser admin.

**Solución (aditiva):** agregar la columna y la lógica de admin.

```sql
-- 1) Agregar la columna rol (no toca los datos existentes)
alter table public.perfiles
    add column if not exists rol text not null default 'usuario';   -- 'usuario' | 'admin'

-- 2) RLS: cada quien puede leer SU propio perfil (así la app lee su rol)
alter table public.perfiles enable row level security;
drop policy if exists "perfil_leer_propio" on public.perfiles;
create policy "perfil_leer_propio" on public.perfiles
    for select to authenticated using (id = auth.uid());

-- 3) Helper: ¿el usuario actual es admin?
create or replace function public.es_admin() returns boolean
language sql security definer as $$
    select exists(select 1 from public.perfiles where id = auth.uid() and rol = 'admin');
$$;

-- 4) puntos_agua: lectura pública, pero crear/editar/borrar = SOLO ADMIN
drop policy if exists "puntos_insertar_logueado" on public.puntos_agua;
drop policy if exists "puntos_actualizar_logueado" on public.puntos_agua;
create policy "puntos_admin_insert" on public.puntos_agua
    for insert to authenticated with check (public.es_admin());
create policy "puntos_admin_update" on public.puntos_agua
    for update to authenticated using (public.es_admin());
create policy "puntos_admin_delete" on public.puntos_agua
    for delete to authenticated using (public.es_admin());
```

### 👑 Nombrar a un administrador (solo desde SQL, por seguridad)

```sql
update public.perfiles set rol = 'admin'
where id = (select id from auth.users where email = 'CORREO_DEL_ADMIN@gmail.com');
```

> ⚠️ Mientras NADIE sea admin, **nadie verá el botón de crear puntos** (es a propósito: "fail-closed").
>
> 💡 Si **no** usas roles todavía y prefieres que cualquier logueado cree puntos, NO ejecutes el
> punto 4 de arriba; en su lugar deja políticas simples:
> ```sql
> create policy "puntos_insertar_logueado" on public.puntos_agua
>     for insert to authenticated with check (true);
> ```

---

## 2️⃣ (Opcional) Sincronizar FAVORITOS — tabla `favoritos`

Sin esta tabla, los "puntos guardados" solo viven en el teléfono. Créala para que se
sincronicen con la nube.

```sql
-- Cada fila = "el usuario X guardó el punto Y". unique evita duplicados.
create table if not exists public.favoritos (
    id         bigint generated always as identity primary key,
    user_id    uuid default auth.uid(),
    punto_id   text not null,
    created_at timestamptz default now(),
    unique (user_id, punto_id)
);
create index if not exists idx_favoritos_user on public.favoritos(user_id);

-- RLS: cada usuario solo ve/gestiona los suyos
alter table public.favoritos enable row level security;
create policy "favoritos_leer_propios" on public.favoritos
    for select to authenticated using (user_id = auth.uid());
create policy "favoritos_insertar_propios" on public.favoritos
    for insert to authenticated with check (user_id = auth.uid());
create policy "favoritos_borrar_propios" on public.favoritos
    for delete to authenticated using (user_id = auth.uid());
```

---

## 3️⃣ (Nueva función) VALORACIÓN de puntos — tabla `valoraciones` + trigger

**Objetivo:** un usuario puntúa un punto de agua del **1 al 5**, **solo una vez**, puede
**modificar** su voto, y el **promedio del punto sube o baja automáticamente**.

### 3.1 Tabla `valoraciones`

Mismo patrón que `favoritos`: `unique(user_id, punto_id)` → un voto por persona y punto.

```sql
create table if not exists public.valoraciones (
    id         bigint generated always as identity primary key,
    user_id    uuid default auth.uid(),
    punto_id   text not null,
    valor      int  not null check (valor between 1 and 5),
    created_at timestamptz default now(),
    updated_at timestamptz default now(),
    unique (user_id, punto_id)     -- ← un voto por usuario y punto
);
create index if not exists idx_valoraciones_punto on public.valoraciones(punto_id);

-- RLS: lectura pública (para el promedio), escribir/editar solo lo propio
alter table public.valoraciones enable row level security;
create policy "valoraciones_lectura_publica" on public.valoraciones
    for select using (true);
create policy "valoraciones_insertar_propias" on public.valoraciones
    for insert to authenticated with check (user_id = auth.uid());
create policy "valoraciones_actualizar_propias" on public.valoraciones
    for update to authenticated using (user_id = auth.uid());
```

### 3.2 Trigger que recalcula el promedio del punto

Cuando alguien vota (o cambia/borra su voto), este trigger actualiza
`puntos_agua.calificacion` con el promedio real. Usa `security definer` para poder
escribir en `puntos_agua` aunque su edición sea "solo admin".

```sql
create or replace function public.recalcular_calificacion()
returns trigger language plpgsql security definer as $$
declare
    pid text;
begin
    pid := coalesce(new.punto_id, old.punto_id);
    update public.puntos_agua
       set calificacion = coalesce(
           (select round(avg(valor)::numeric, 1) from public.valoraciones where punto_id = pid),
           0
       )
     where id = pid;
    return null;
end; $$;

drop trigger if exists trg_recalcular_calificacion on public.valoraciones;
create trigger trg_recalcular_calificacion
    after insert or update or delete on public.valoraciones
    for each row execute function public.recalcular_calificacion();
```

### 3.3 Cómo lo usará la app (pendiente de programar)

1. **Votar / modificar:** `POST /rest/v1/valoraciones` con header
   `Prefer: resolution=merge-duplicates` → hace *upsert* sobre `unique(user_id, punto_id)`.
   El mismo endpoint sirve para el primer voto y para cambiarlo.
2. **Leer mi voto** (para pintar las estrellas ya marcadas):
   `GET /rest/v1/valoraciones?punto_id=eq.<id>&user_id=eq.<uuid>`.
   → Requiere guardar el **UUID del usuario** en la sesión (hoy la app lo descarta al loguear).
3. **Ver el promedio:** no hay que hacer nada nuevo — `puntos_agua.calificacion` ya viaja a la
   app y se muestra en mapa, lista y detalle. El trigger la mantiene actualizada.

> **Decisiones de producto pendientes** (afectan solo al código de la app, no a este SQL):
> - ¿Se puede votar sin estar a 100 m? (sugerido: **sí**, a diferencia de los reportes).
> - Invitados **no** votan (lo bloquea RLS) → se les muestra el aviso habitual.
> - Votar **requiere conexión** (auth + RLS); sin red no se encola (MVP).

---

## ✅ Checklist de esta actualización

- [ ] 1. Columna `rol` agregada a `perfiles` (+ admin nombrado).
- [ ] 2. (Opcional) Tabla `favoritos` creada.
- [ ] 3. Tabla `valoraciones` + trigger `trg_recalcular_calificacion` creados.
- [ ] Programar en la app el uso de `valoraciones` (upsert + leer mi voto + guardar UUID de sesión).
