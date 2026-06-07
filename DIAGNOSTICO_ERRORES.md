# 🩺 AguaMap — Cómo ver qué endpoint está fallando

Como la app es **offline-first**, cuando Supabase falla la app **no se rompe**: usa los datos
locales y sigue andando. El problema es que así **no ves el error**. Para eso ahora la app
escribe en **Logcat** (la consola de Android Studio) **qué endpoint falló y por qué exactamente**.

---

## 1. Abrir Logcat y filtrar

1. En Android Studio, abajo, abre la pestaña **Logcat** (o `Alt+6`).
2. Conecta tu teléfono / emulador y corre la app.
3. En la barra de búsqueda de Logcat escribe:
   ```
   tag:AGUAMAP_NET
   ```
   (o simplemente `AGUAMAP`). Verás solo los mensajes de red de la app.

---

## 2. Cómo se leen los mensajes

Cada llamada a Supabase deja una línea:

```
✅ [GET rest/v1/puntos_agua] OK 4 puntos
✅ [POST auth/v1/token] OK usuario=jairo@gmail.com
❌ [POST rest/v1/comentarios] HTTP 401 → {"message":"new row violates row-level security policy"}
❌ [GET rest/v1/reportes] HTTP 404 → relation "public.reportes" does not exist
❌ [POST auth/v1/token] Sin respuesta (¿sin internet?): Unable to resolve host
```

- **✅** = la llamada funcionó.
- **❌** = falló. Te dice **el endpoint**, el **código HTTP** y el **mensaje exacto de Supabase**.

Además, OkHttp (solo en debug) imprime la URL completa de cada petición, por ejemplo:
```
--> GET https://xxxxx.supabase.co/rest/v1/comentarios?punto_id=eq.123
<-- 404 Not Found https://xxxxx.supabase.co/rest/v1/comentarios (88ms)
```

---

## 3. Tabla de errores comunes y cómo arreglarlos

| Código | Qué significa | Cómo se arregla |
|:---:|---|---|
| **404** `does not exist` | La **tabla no existe** en Supabase | Crea las tablas (Paso 1 de [SUPABASE_TABLAS.md](SUPABASE_TABLAS.md)) |
| **401 / 403** `row-level security` | Falta permiso (**RLS**) o no estás logueado | Crea las políticas (Paso 2 de [SUPABASE_TABLAS.md](SUPABASE_TABLAS.md)) e inicia sesión |
| **400** `invalid input` / columna | El dato o el nombre de columna no coincide | Revisa que la tabla tenga las columnas del Paso 1 |
| **400** `Email not confirmed` | Falta confirmar el correo | Desactiva "Confirm email" (Paso 4 de [SUPABASE_TABLAS.md](SUPABASE_TABLAS.md)) |
| **400** `Invalid login credentials` | Correo o contraseña incorrectos | Verifica los datos del usuario |
| **404** bucket / **400** storage | El **bucket de fotos** no existe o sin permiso | Crea el bucket `reportes` (Paso 3 de [SUPABASE_TABLAS.md](SUPABASE_TABLAS.md)) |
| `Sin respuesta (¿sin internet?)` | No hubo conexión al servidor | Revisa internet / la URL en `local.properties` |

---

## 4. ¿Por qué la app no muestra el error en pantalla?

A propósito: si mostráramos cada error de red en la pantalla, saldrían **"cosas sin sentido"**
al usuario final (por ejemplo, mientras aún no creas las tablas). Por eso:

- **El usuario final** ve la app funcionando con datos locales (sin errores feos).
- **Tú, como desarrollador**, ves el detalle exacto en **Logcat** para depurar.

> Login y registro **sí** muestran un mensaje en pantalla (Toast), porque ahí el usuario
> necesita saber si entró o no. El resto (puntos, comentarios, etc.) se diagnostica por Logcat.

---

## 5. Resumen

1. Logcat → filtra `AGUAMAP_NET`.
2. Busca la línea con **❌**.
3. Mira el **código** y el **mensaje** → ubícalo en la tabla de arriba.
4. Aplica el arreglo (casi siempre es crear una tabla, un bucket o una política en Supabase).
