"""Agregación espacial: agrupa reportes en celdas de una cuadrícula para
identificar las regiones más afectadas."""

from __future__ import annotations

import math
from collections import defaultdict
from collections.abc import Iterable
from dataclasses import dataclass

from app.domain.entities import Reporte


@dataclass(frozen=True)
class Zona:
    latitud: float          # centro de la celda
    longitud: float
    celda: float            # tamaño de la celda en grados
    reportes: int
    nivel_promedio: float
    nivel_maximo: int
    afectacion: int         # suma de niveles: combina cantidad e intensidad


def agrupar_por_zona(reportes: Iterable[Reporte], celda: float) -> list[Zona]:
    if celda <= 0:
        raise ValueError("el tamaño de celda debe ser positivo")

    grupos: dict[tuple[int, int], list[int]] = defaultdict(list)
    for r in reportes:
        clave = (math.floor(r.latitud / celda), math.floor(r.longitud / celda))
        grupos[clave].append(r.nivel)

    zonas = [
        Zona(
            latitud=round((i + 0.5) * celda, 6),
            longitud=round((j + 0.5) * celda, 6),
            celda=celda,
            reportes=len(niveles),
            nivel_promedio=round(sum(niveles) / len(niveles), 2),
            nivel_maximo=max(niveles),
            afectacion=sum(niveles),
        )
        for (i, j), niveles in grupos.items()
    ]
    zonas.sort(key=lambda z: (z.afectacion, z.nivel_maximo, z.nivel_promedio), reverse=True)
    return zonas
