"""Casos de uso de consulta que alimentan el visor de mapas de calor."""

from __future__ import annotations

from app.application.ports import Filtros, ReporteRepository
from app.domain.entities import Reporte, intensidad
from app.domain.zonas import Zona, agrupar_por_zona


class ConsultarReportes:
    def __init__(self, repo: ReporteRepository) -> None:
        self._repo = repo

    def reportes(self, filtros: Filtros) -> list[Reporte]:
        return self._repo.listar(filtros)

    def heatmap(self, filtros: Filtros) -> list[tuple[float, float, float]]:
        return [(r.latitud, r.longitud, intensidad(r.nivel)) for r in self._repo.listar(filtros)]

    def zonas(self, filtros: Filtros, celda: float, top: int) -> list[Zona]:
        return agrupar_por_zona(self._repo.listar(filtros), celda)[:top]

    def eventos(self) -> list[tuple[str, int]]:
        return self._repo.eventos()
