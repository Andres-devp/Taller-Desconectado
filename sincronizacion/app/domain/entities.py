"""Entidades y reglas del dominio: el reporte de un sitio de desastre."""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime, timedelta, timezone
from uuid import UUID

from app.domain.exceptions import ReporteInvalido

NIVEL_MIN = 1
NIVEL_MAX = 5
# Tolerancia ante relojes de dispositivos mal configurados.
MAX_DESFASE_FUTURO = timedelta(hours=24)


@dataclass(frozen=True)
class Reporte:
    """Un sitio de desastre capturado en campo.

    El `id` lo genera el dispositivo al momento de la captura (UUID v4) y es la
    clave de idempotencia de la sincronización: reenviar el mismo reporte nunca
    crea un duplicado.
    """

    id: UUID
    latitud: float
    longitud: float
    nivel: int
    capturado_en: datetime
    evento: str | None = None
    descripcion: str | None = None
    precision_m: float | None = None
    dispositivo_id: str | None = None
    recibido_en: datetime | None = field(default=None, compare=False)

    def __post_init__(self) -> None:
        validar(self)


def validar(r: Reporte) -> None:
    if not -90.0 <= r.latitud <= 90.0:
        raise ReporteInvalido(f"latitud fuera de rango: {r.latitud}")
    if not -180.0 <= r.longitud <= 180.0:
        raise ReporteInvalido(f"longitud fuera de rango: {r.longitud}")
    if not NIVEL_MIN <= r.nivel <= NIVEL_MAX:
        raise ReporteInvalido(f"nivel de afectación debe estar entre {NIVEL_MIN} y {NIVEL_MAX}")
    if r.precision_m is not None and r.precision_m < 0:
        raise ReporteInvalido("la precisión no puede ser negativa")
    capturado = r.capturado_en
    if capturado.tzinfo is None:
        capturado = capturado.replace(tzinfo=timezone.utc)
    if capturado > datetime.now(timezone.utc) + MAX_DESFASE_FUTURO:
        raise ReporteInvalido("la fecha de captura está en el futuro")


def intensidad(nivel: int) -> float:
    """Peso del punto en el mapa de calor (0, 1]."""
    return nivel / NIVEL_MAX
