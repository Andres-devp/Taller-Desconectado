"""Genera reportes de ejemplo y los envía a la API como lo haría la app móvil.

Uso:  python scripts/seed.py [URL_BASE] [CANTIDAD]
      python scripts/seed.py http://localhost:8000 400
Solo usa la librería estándar, así corre en el host o dentro del contenedor.
"""

from __future__ import annotations

import json
import random
import sys
import urllib.request
import uuid
from datetime import datetime, timedelta, timezone

# (evento, latitud, longitud, dispersión en grados, sesgo de nivel)
FOCOS = [
    ("Inundación Mocoa", 1.1466, -76.6476, 0.04, 4),
    ("Inundación Mocoa", 1.1700, -76.6200, 0.03, 3),
    ("Deslizamiento Rosas", 2.2620, -76.7400, 0.03, 4),
    ("Huracán Providencia", 13.3486, -81.3739, 0.03, 5),
    ("Huracán Providencia", 12.5847, -81.7006, 0.04, 3),
    ("Sismo Eje Cafetero", 4.5339, -75.6811, 0.06, 3),
    ("Sismo Eje Cafetero", 4.8133, -75.6961, 0.05, 2),
]


def generar(n: int) -> list[dict]:
    ahora = datetime.now(timezone.utc)
    reportes = []
    for _ in range(n):
        evento, lat, lon, disp, sesgo = random.choice(FOCOS)
        nivel = max(1, min(5, round(random.gauss(sesgo, 1))))
        reportes.append({
            "id": str(uuid.uuid4()),
            "latitud": round(random.gauss(lat, disp), 6),
            "longitud": round(random.gauss(lon, disp), 6),
            "nivel": nivel,
            "evento": evento,
            "descripcion": "Dato de ejemplo generado por seed.py",
            "precision_m": round(random.uniform(3, 25), 1),
            "dispositivo_id": f"seed-{random.randint(1, 5)}",
            "capturado_en": (ahora - timedelta(hours=random.uniform(0, 72))).isoformat(),
        })
    return reportes


def enviar(base: str, reportes: list[dict]) -> dict:
    req = urllib.request.Request(
        f"{base.rstrip('/')}/api/v1/reportes/lote",
        data=json.dumps({"reportes": reportes}).encode(),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=15) as resp:
        return json.loads(resp.read())


def main() -> None:
    base = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8000"
    cantidad = int(sys.argv[2]) if len(sys.argv) > 2 else 400
    reportes = generar(cantidad)
    nuevos = 0
    for i in range(0, len(reportes), 100):
        nuevos += enviar(base, reportes[i:i + 100])["nuevos"]
    print(f"{nuevos} reportes de ejemplo creados en {base}")


if __name__ == "__main__":
    main()
