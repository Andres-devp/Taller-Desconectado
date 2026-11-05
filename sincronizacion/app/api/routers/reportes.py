"""Endpoints de sincronización (app móvil) y de consulta (visor de mapas)."""

from __future__ import annotations

from datetime import datetime

from fastapi import APIRouter, Depends, Query
from pydantic import ValidationError

from app.api.dependencies import get_consultas, get_sincronizar
from app.api.schemas import (
    EventoSalida,
    LoteEntrada,
    LoteSalida,
    RechazoSalida,
    ReporteEntrada,
    ReporteSalida,
    ZonaSalida,
)
from app.application.consultas import ConsultarReportes
from app.application.ports import Filtros
from app.application.sincronizar_lote import Rechazo, SincronizarLote
from app.domain.entities import Reporte
from app.domain.exceptions import ReporteInvalido

router = APIRouter(prefix="/api/v1", tags=["reportes"])


def _filtros(
    evento: str | None = Query(default=None, description="Nombre del evento de desastre"),
    nivel_min: int | None = Query(default=None, ge=1, le=5),
    desde: datetime | None = Query(default=None),
    hasta: datetime | None = Query(default=None),
    limite: int = Query(default=10_000, ge=1, le=50_000),
) -> Filtros:
    return Filtros(evento=evento or None, nivel_min=nivel_min, desde=desde, hasta=hasta,
                   limite=limite)


@router.post("/reportes/lote", response_model=LoteSalida,
             summary="Sincroniza un lote de reportes capturados sin conexión (idempotente)")
def sincronizar_lote(
    lote: LoteEntrada,
    caso_uso: SincronizarLote = Depends(get_sincronizar),
) -> LoteSalida:
    validos: list[Reporte] = []
    rechazados: list[Rechazo] = []
    for crudo in lote.reportes:
        id_crudo = str(crudo.get("id")) if crudo.get("id") is not None else None
        try:
            entrada = ReporteEntrada.model_validate(crudo)
            validos.append(Reporte(**entrada.model_dump()))
        except ValidationError as exc:
            campos = ", ".join(".".join(map(str, e["loc"])) for e in exc.errors())
            rechazados.append(Rechazo(id=id_crudo, motivo=f"campos inválidos: {campos}"))
        except ReporteInvalido as exc:
            rechazados.append(Rechazo(id=id_crudo, motivo=str(exc)))

    resultado = caso_uso.ejecutar(validos, rechazados)
    return LoteSalida(
        aceptados=resultado.aceptados,
        nuevos=resultado.nuevos,
        duplicados=resultado.duplicados,
        rechazados=[RechazoSalida(id=r.id, motivo=r.motivo) for r in resultado.rechazados],
    )


@router.get("/reportes", response_model=list[ReporteSalida])
def listar_reportes(
    filtros: Filtros = Depends(_filtros),
    consultas: ConsultarReportes = Depends(get_consultas),
) -> list[ReporteSalida]:
    return [ReporteSalida(**r.__dict__) for r in consultas.reportes(filtros)]


@router.get("/heatmap", response_model=list[tuple[float, float, float]],
            summary="Puntos [latitud, longitud, intensidad] para Leaflet.heat")
def heatmap(
    filtros: Filtros = Depends(_filtros),
    consultas: ConsultarReportes = Depends(get_consultas),
) -> list[tuple[float, float, float]]:
    return consultas.heatmap(filtros)


@router.get("/zonas", response_model=list[ZonaSalida],
            summary="Regiones más afectadas, agregadas en una cuadrícula")
def zonas(
    celda: float = Query(default=0.05, gt=0, le=10, description="Tamaño de celda en grados"),
    top: int = Query(default=10, ge=1, le=200),
    filtros: Filtros = Depends(_filtros),
    consultas: ConsultarReportes = Depends(get_consultas),
) -> list[ZonaSalida]:
    return [ZonaSalida(**z.__dict__) for z in consultas.zonas(filtros, celda, top)]


@router.get("/eventos", response_model=list[EventoSalida])
def eventos(consultas: ConsultarReportes = Depends(get_consultas)) -> list[EventoSalida]:
    return [EventoSalida(evento=e, reportes=n) for e, n in consultas.eventos()]
