"""Sistema central de sincronización de reportes de desastre.

Recibe los lotes que envía la app móvil cuando recupera conexión y expone las
consultas que usa el visor de mapas de calor.
"""

from __future__ import annotations

from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.api.routers import health, reportes
from app.core.config import Settings, get_settings
from app.core.logging import configure_logging, get_logger
from app.domain.exceptions import LoteDemasiadoGrande
from app.infrastructure.database import build_engine, build_session_factory, check_connection
from app.infrastructure.orm_models import Base

_logger = get_logger("main")


def create_app(settings: Settings | None = None) -> FastAPI:
    settings = settings or get_settings()
    configure_logging(settings.log_level, settings.service_name)

    @asynccontextmanager
    async def lifespan(app: FastAPI):
        engine = build_engine(settings)
        app.state.engine = engine
        app.state.session_factory = build_session_factory(engine)
        # En Docker el esquema lo crea `database/init/01-schema.sql`; esto cubre
        # ejecuciones locales y pruebas.
        if check_connection(engine):
            Base.metadata.create_all(engine)
            _logger.info("servicio iniciado")
        else:
            _logger.error("arranque sin base de datos; /health/ready reportará not-ready")
        yield
        engine.dispose()

    app = FastAPI(
        title="Taller Desconectado — Sistema central",
        description="Sincronización de reportes de sitios de desastre y datos para mapas de calor",
        version="1.0.0",
        lifespan=lifespan,
    )
    app.add_middleware(
        CORSMiddleware,
        allow_origins=settings.cors_origin_list,
        allow_methods=["GET", "POST"],
        allow_headers=["*"],
    )

    @app.exception_handler(LoteDemasiadoGrande)
    async def _lote_grande(_: Request, exc: LoteDemasiadoGrande) -> JSONResponse:
        return JSONResponse(status_code=413, content={"detail": str(exc)})

    app.dependency_overrides[get_settings] = lambda: settings
    app.include_router(health.router)
    app.include_router(reportes.router)
    return app


app = create_app()
