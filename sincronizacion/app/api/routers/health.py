"""Sondas de salud: la app móvil usa /health para saber si el servidor es alcanzable."""

from __future__ import annotations

from fastapi import APIRouter, Request, Response, status

from app.infrastructure.database import check_connection

router = APIRouter(tags=["salud"])


@router.get("/health")
def health() -> dict:
    return {"status": "ok"}


@router.get("/health/ready")
def ready(request: Request, response: Response) -> dict:
    if check_connection(request.app.state.engine):
        return {"status": "ready", "database": "up"}
    response.status_code = status.HTTP_503_SERVICE_UNAVAILABLE
    return {"status": "not-ready", "database": "down"}
