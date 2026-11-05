"""Inyección de dependencias: arma los casos de uso con sus adaptadores."""

from __future__ import annotations

from fastapi import Depends, Request

from app.application.consultas import ConsultarReportes
from app.application.sincronizar_lote import SincronizarLote
from app.core.config import Settings, get_settings
from app.infrastructure.reporte_repository import SqlReporteRepository


def get_repo(request: Request) -> SqlReporteRepository:
    return SqlReporteRepository(request.app.state.session_factory)


def get_sincronizar(
    repo: SqlReporteRepository = Depends(get_repo),
    settings: Settings = Depends(get_settings),
) -> SincronizarLote:
    return SincronizarLote(repo, settings.max_lote)


def get_consultas(repo: SqlReporteRepository = Depends(get_repo)) -> ConsultarReportes:
    return ConsultarReportes(repo)
