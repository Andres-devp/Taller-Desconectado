"""Caso de uso: recibir un lote de reportes capturados sin conexión."""

from __future__ import annotations

from collections.abc import Sequence
from dataclasses import dataclass, field
from uuid import UUID

from app.application.ports import ReporteRepository
from app.core.logging import get_logger
from app.domain.entities import Reporte
from app.domain.exceptions import LoteDemasiadoGrande

_logger = get_logger("sincronizar_lote")


@dataclass(frozen=True)
class Rechazo:
    id: str | None
    motivo: str


@dataclass
class ResultadoLote:
    aceptados: list[UUID] = field(default_factory=list)
    nuevos: int = 0
    duplicados: int = 0
    rechazados: list[Rechazo] = field(default_factory=list)


class SincronizarLote:
    """Persiste los reportes válidos de un lote.

    Un reporte ya existente (mismo id) se considera *aceptado*: el dispositivo
    puede haber perdido la respuesta de un envío anterior y reintentar. Así el
    cliente solo necesita marcar como sincronizado lo que vuelva en `aceptados`.
    """

    def __init__(self, repo: ReporteRepository, max_lote: int) -> None:
        self._repo = repo
        self._max_lote = max_lote

    def ejecutar(self, validos: Sequence[Reporte], rechazados: Sequence[Rechazo]) -> ResultadoLote:
        total = len(validos) + len(rechazados)
        if total > self._max_lote:
            raise LoteDemasiadoGrande(f"el lote tiene {total} reportes; máximo {self._max_lote}")

        # Deduplicar dentro del mismo lote conservando el primero.
        unicos: dict[UUID, Reporte] = {}
        for r in validos:
            unicos.setdefault(r.id, r)

        insertados = self._repo.guardar_si_no_existe(list(unicos.values())) if unicos else set()

        resultado = ResultadoLote(
            aceptados=list(unicos.keys()),
            nuevos=len(insertados),
            duplicados=len(unicos) - len(insertados),
            rechazados=list(rechazados),
        )
        _logger.info(
            "lote sincronizado",
            extra={"evento_log": "sync.lote", "recibidos": total, "nuevos": resultado.nuevos,
                   "duplicados": resultado.duplicados, "rechazados": len(rechazados)},
        )
        return resultado
