"""Fixtures: la API real sobre SQLite en un archivo temporal.

El repositorio es el mismo código que corre contra PostgreSQL (SQLAlchemy con
`ON CONFLICT DO NOTHING` en ambos dialectos), así se prueba la idempotencia de
verdad sin levantar un contenedor.
"""

from __future__ import annotations

import uuid
from datetime import datetime, timedelta, timezone

import pytest
from fastapi.testclient import TestClient

from app.core.config import Settings
from app.main import create_app


@pytest.fixture
def settings(tmp_path) -> Settings:
    return Settings(database_url=f"sqlite:///{tmp_path}/test.db", max_lote=50)


@pytest.fixture
def client(settings: Settings) -> TestClient:
    with TestClient(create_app(settings)) as c:
        yield c


@pytest.fixture
def nuevo_reporte():
    def _crear(**cambios) -> dict:
        base = {
            "id": str(uuid.uuid4()),
            "latitud": 4.6097,
            "longitud": -74.0817,
            "nivel": 3,
            "evento": "Prueba",
            "capturado_en": (datetime.now(timezone.utc) - timedelta(minutes=5)).isoformat(),
            "dispositivo_id": "test",
        }
        base.update(cambios)
        return base

    return _crear
