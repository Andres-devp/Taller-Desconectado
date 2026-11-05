# Taller Desconectado — Reporte de sitios de desastre

Arquitectura de Software · Pontificia Universidad Javeriana

App **Android** *offline-first* para registrar la posición (latitud/longitud) y el **nivel de
afectación (1–5)** de sitios de desastre. Funciona sin internet y **sincroniza automáticamente** con
un sistema central cuando vuelve la conexión. Un **visor web de mapas de calor** muestra qué
regiones fueron más afectadas.

```
 App Android ──(sin red: guarda en SQLite)──┐
     │                                       │ WorkManager, al volver la red
     └──────── POST /api/v1/reportes/lote ◄──┘
                         │
              sincronizacion (FastAPI :8000) ──► PostgreSQL
                         ▲
 Navegador ──► visualizacion (nginx :8080, Leaflet + heatmap)
```

El documento de diseño (requisitos, atributos de calidad, diagramas, modelo de datos, API y ADR)
está en **[`docs/diseno.md`](docs/diseno.md)**.

## Contenido

| Carpeta | Qué es | Tecnología |
|---|---|---|
| `app-android/` | App móvil | Kotlin, Jetpack Compose, Room, WorkManager, Retrofit |
| `sincronizacion/` | Sistema central (API) | FastAPI, SQLAlchemy, PostgreSQL |
| `visualizacion/` | Visor de mapas de calor | Leaflet 1.9 + leaflet.heat, nginx |
| `apk/taller-desconectado.apk` | APK lista para instalar | Android 8.0+ (API 26) |
| `docs/diseno.md` | Diseño de la solución | Mermaid |

## 1. Levantar el sistema central y el visor

Requiere Docker.

```bash
make up        # postgres + api (:8000) + visor (:8080)
make seed      # opcional: 400 reportes de ejemplo para ver el mapa de calor
```

- Visor de mapas de calor: <http://localhost:8080>
- API y documentación OpenAPI: <http://localhost:8000/docs>

```bash
make logs      # logs JSON de la API
make down      # detener   ·   make clean para borrar también los datos
```

## 2. Instalar y configurar la app

```bash
adb install apk/taller-desconectado.apk
```

o copie el `.apk` al teléfono y ábralo (permita "instalar apps desconocidas").

En la pestaña **Ajustes** configure la URL del sistema central y pulse *Guardar y probar conexión*:

| Dónde corre la app | URL |
|---|---|
| Emulador de Android Studio | `http://10.0.2.2:8000/` (valor por defecto) |
| Celular físico en la misma Wi-Fi | `http://<IP-del-computador>:8000/` (la IP se obtiene con `make ip`) |

Si el celular no logra conectarse, revise que el firewall del computador permita el puerto 8000.

## 3. Probar el modo desconectado

1. Active el **modo avión** del teléfono o emulador.
2. En **Nuevo**, pulse *Obtener mi ubicación* (el GPS funciona sin internet), elija un nivel y
   pulse *Guardar*. Repita varias veces. En el emulador se puede simular la posición con
   `adb emu geo fix -74.08 4.60`.
3. En **Reportes** aparece el banner *Sin conexión* y cada reporte queda como *Pendiente*.
4. Desactive el modo avión. WorkManager sincroniza solo y los reportes pasan a *Enviado*.
   También puede pulsar *Sincronizar*.
5. Abra <http://localhost:8080>: los reportes aparecen en el mapa de calor y en el ranking de
   *Zonas más afectadas*.

Reenviar no genera duplicados, porque cada reporte lleva un UUID creado en el dispositivo y el
servidor es idempotente.

## 4. Pruebas

Requiere Python 3.11+ y JDK 17.

```bash
make test      # pytest del sistema central (crea sincronizacion/.venv) + pruebas unitarias Android
```

## 5. Compilar la APK

Requiere JDK 17 y el Android SDK (API 35).

```bash
make apk       # ./gradlew assembleDebug y copia a apk/taller-desconectado.apk
```
