# 👮 AguaMap — Roles y Permisos (propuesta + lo ya implementado)

Este documento tiene **dos partes**:

1. ✅ **Lo que YA quedó implementado en la app** (restricciones de invitado).
2. 💡 **La IDEA para los roles `admin` / `usuario`** (esto NO está implementado todavía;
   es una guía para que lo apliques cuando quieras).

---

## 1. ✅ Lo que YA está implementado (permisos de Invitado)

La app ya distingue entre **invitado** (entró sin loguearse) y **usuario registrado**, usando
el estado `isGuest` que ya existía en `AuthViewModel`. Se aplicaron estas reglas en la interfaz:

| Acción | 👀 Invitado | 🙋 Usuario registrado |
|---|:---:|:---:|
| Ver puntos de agua | ✅ | ✅ |
| **Filtrar / buscar** puntos | ✅ | ✅ |
| Ver detalle de un punto | ✅ | ✅ |
| Ver comentarios y reportes | ✅ | ✅ |
| Ver noticias de la comunidad | ✅ | ✅ |
| **Sugerir** un punto nuevo (botón ＋) | ❌ (oculto) | ✅ |
| **Comentar** | ❌ (campo oculto + aviso) | ✅ |
| **Reportar** un problema | ❌ (botón oculto + aviso) | ✅ |

**Dónde se aplicó (por si quieres revisarlo):**
- `HomeScreen.kt` → el botón flotante ＋ (sugerir punto) se oculta si `isGuest`.
- `WaterPointDetailScreen.kt` → el campo de comentar y el botón de reportar se ocultan si
  `isGuest`; en su lugar aparece un aviso "Inicia sesión para...".
- `AppNavigation.kt` → le pasa `isGuest` a la pantalla de detalle.

> Cuando el invitado intenta una acción bloqueada, ve un mensaje invitándolo a iniciar sesión.

---

## 2. 💡 IDEA para implementar los roles `admin` y `usuario`

Hoy todos los usuarios registrados tienen los mismos permisos. La diferencia que quieres es:

| Acción | 👀 Invitado | 🙋 Usuario | 👑 Admin |
|---|:---:|:---:|:---:|
| Ver / filtrar puntos | ✅ | ✅ | ✅ |
| Comentar | ❌ | ✅ | ✅ |
| Reportar problema | ❌ | ✅ | ✅ |
| **Sugerir** punto (queda pendiente de aprobación) | ❌ | ✅ | ✅ |
| **Crear punto real** (aprobado y visible para todos) | ❌ | ❌ | ✅ |
| **Aprobar / borrar** puntos sugeridos | ❌ | ❌ | ✅ |

La clave es: **el usuario "sugiere"** (su punto queda en revisión) y **el admin "crea de verdad"**
(o aprueba las sugerencias). Aquí está la idea para lograrlo.

---

### Paso A — Guardar el ROL de cada usuario (tabla `perfiles`)

Lo más limpio en Supabase es una tabla `perfiles` ligada a `auth.users`. Pega esto en el
**SQL Editor**:

```sql
-- 1) Tabla de perfiles con el rol de cada usuario
create table if not exists public.perfiles (
    id        uuid primary key references auth.users(id) on delete cascade,
    rol       text not null default 'usuario',   -- 'usuario' | 'admin'
    creado_en timestamptz default now()
);

-- 2) Cuando alguien se registra, se le crea su perfil automáticamente como 'usuario'
create or replace function public.crear_perfil_nuevo()
returns trigger
language plpgsql
security definer
as $$
begin
    insert into public.perfiles (id, rol) values (new.id, 'usuario');
    return new;
end;
$$;

create trigger al_registrarse_crear_perfil
    after insert on auth.users
    for each row execute function public.crear_perfil_nuevo();

-- 3) Seguridad: cada quien puede leer su propio perfil
alter table public.perfiles enable row level security;

create policy "perfil_leer_propio"
    on public.perfiles for select
    to authenticated
    using (id = auth.uid());
```

> Para hacer **admin** a alguien, ejecutas una vez (con su id de Authentication → Users):
> ```sql
> update public.perfiles set rol = 'admin' where id = 'EL-UUID-DEL-USUARIO';
> ```

---

### Paso B — Marcar los puntos como "aprobados" o "sugeridos"

Agrega una columna a `puntos_agua` para distinguir un punto real (aprobado) de una sugerencia:

```sql
alter table public.puntos_agua
    add column if not exists aprobado boolean default false;
```

- **Admin** crea puntos con `aprobado = true` (se ven en el mapa de todos).
- **Usuario** crea sugerencias con `aprobado = false` (esperan revisión del admin).

#### Políticas RLS según el rol (idea)

```sql
-- Helper: ¿el usuario actual es admin?
create or replace function public.es_admin()
returns boolean language sql security definer as $$
    select exists (
        select 1 from public.perfiles
        where id = auth.uid() and rol = 'admin'
    );
$$;

-- TODOS ven solo los puntos APROBADOS...
drop policy if exists "puntos_lectura_publica" on public.puntos_agua;
create policy "puntos_ver_aprobados"
    on public.puntos_agua for select
    using (aprobado = true or public.es_admin());

-- El USUARIO solo puede insertar SUGERENCIAS (aprobado = false)
create policy "usuario_sugiere"
    on public.puntos_agua for insert
    to authenticated
    with check (aprobado = false);

-- El ADMIN puede insertar puntos aprobados y actualizar/aprobar/borrar
create policy "admin_crea_aprobado"
    on public.puntos_agua for insert
    to authenticated
    with check (public.es_admin());

create policy "admin_actualiza"
    on public.puntos_agua for update
    to authenticated
    using (public.es_admin());

create policy "admin_borra"
    on public.puntos_agua for delete
    to authenticated
    using (public.es_admin());
```

> Con esto, aunque la app enviara `aprobado = true`, Supabase **rechaza** la inserción si el
> usuario no es admin (la seguridad real vive en el backend, no solo en la app).

---

### Paso C — Que la app lea el rol (cambios sugeridos en el código)

Cuando lo quieras implementar, estos serían los cambios (pequeños) en la app:

1. **`UsuarioSesion`** → añadir el campo del rol:
   ```kotlin
   data class UsuarioSesion(
       val nombre: String = "",
       // ...
       val rol: String = "usuario"   // 'usuario' | 'admin'
   )
   ```

2. **`SupabaseApiService`** → endpoint para leer el perfil tras el login:
   ```kotlin
   @GET("rest/v1/perfiles")
   suspend fun getMiPerfil(
       @Header("apikey") apiKey: String,
       @Header("Authorization") bearer: String,
       @Query("id") id: String,        // "eq.<uuid-del-usuario>"
       @Query("select") select: String = "rol"
   ): Response<List<PerfilDto>>
   ```

3. **`AuthViewModel`** → después del login, pedir el perfil y guardar el `rol` en la sesión
   (y en `SessionManager`, junto al token).

4. **La UI** → exponer un `isAdmin` (igual que hoy usamos `isGuest`) y mostrar acciones extra:
   - Botón "Aprobar" en los puntos sugeridos (solo admin).
   - El texto del botón ＋ cambia: "Crear punto" (admin) vs "Sugerir punto" (usuario).

> Es exactamente el mismo patrón que ya usamos con `isGuest`, pero con un tercer nivel
> (`isAdmin`). Por eso el cambio es chico y de bajo riesgo.

---

### Resumen de la idea

```
INVITADO  →  solo mira y filtra            (ya implementado en la app ✅)
USUARIO   →  comenta, reporta y SUGIERE    (sugerencias con aprobado=false)
ADMIN     →  crea puntos reales y APRUEBA  (rol='admin' en tabla perfiles)
```

1. Tabla `perfiles` con columna `rol` + trigger al registrarse.  → **Paso A**
2. Columna `aprobado` en `puntos_agua` + políticas RLS por rol.  → **Paso B**
3. La app lee el rol y muestra/oculta acciones de admin.          → **Paso C**

Cuando quieras, dime *"implementa los roles"* y aplico el Paso C en el código.
