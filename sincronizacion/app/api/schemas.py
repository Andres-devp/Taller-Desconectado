"""Contratos HTTP (DTO) de la API."""

from __future__ import annotations

from datetime import datetime
from typing import Any
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field


class ReporteEntrada(BaseModel):
    """Un reporte tal como lo envía la app móvil."""

    model_config = ConfigDict(extra="ignore")

    id: UUID
    latitud: float
    longitud: float
    nivel: int
    capturado_en: datetime
    evento: str | None = Field(default=None, max_length=120)
    descripcion: str | None = Field(default=None, max_length=500)
    precision_m: float | None = None
    dispositivo_id: str | None = Field(default=None, max_length=64)


class LoteEntrada(BaseModel):
    # Se reciben como objetos genéricos para validar cada reporte por separado:
    # un reporte malformado no debe invalidar todo el lote.
    reportes: list[dict[str, Any]]


class RechazoSalida(BaseModel):
    id: str | None
    motivo: str


class LoteSalida(BaseModel):
    aceptados: list[UUID]
    nuevos: int
    duplicados: int
    rechazados: list[RechazoSalida]


class ReporteSalida(BaseModel):
    id: UUID
    latitud: float
    longitud: float
    nivel: int
    capturado_en: datetime
    recibido_en: datetime | None
    evento: str | None
    descripcion: str | None
    precision_m: float | None
    dispositivo_id: str | None


class ZonaSalida(BaseModel):
    latitud: float
    longitud: float
    celda: float
    reportes: int
    nivel_promedio: float
    nivel_maximo: int
    afectacion: int


class EventoSalida(BaseModel):
    evento: str
    reportes: int
