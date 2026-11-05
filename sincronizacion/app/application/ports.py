"""Puertos de la capa de aplicación: lo que el caso de uso necesita del exterior."""

from __future__ import annotations

from collections.abc import Sequence
from dataclasses import dataclass
from datetime import datetime
from typing import Protocol
from uuid import UUID

from app.domain.entities import Reporte


@dataclass(frozen=True)
class Filtros:
    evento: str | None = None
    nivel_min: int | None = None
    desde: datetime | None = None
    hasta: datetime | None = None
    limite: int = 10_000


class ReporteRepository(Protocol):
    def guardar_si_no_existe(self, reportes: Sequence[Reporte]) -> set[UUID]:
        """Inserta los reportes nuevos e ignora los ya existentes (idempotente).

        Devuelve los ids efectivamente insertados.
        """

    def listar(self, filtros: Filtros) -> list[Reporte]: ...

    def eventos(self) -> list[tuple[str, int]]: ...
