"""Adaptador del puerto `ReporteRepository` sobre SQLAlchemy (PostgreSQL / SQLite)."""

from __future__ import annotations

from collections.abc import Sequence
from datetime import timezone
from uuid import UUID

from sqlalchemy import func, select
from sqlalchemy.dialects import postgresql, sqlite
from sqlalchemy.orm import Session, sessionmaker

from app.application.ports import Filtros
from app.domain.entities import Reporte
from app.infrastructure.orm_models import ReporteORM


class SqlReporteRepository:
    def __init__(self, session_factory: sessionmaker[Session]) -> None:
        self._sf = session_factory

    def guardar_si_no_existe(self, reportes: Sequence[Reporte]) -> set[UUID]:
        filas = [
            {
                "id": r.id,
                "latitud": r.latitud,
                "longitud": r.longitud,
                "nivel": r.nivel,
                "evento": r.evento,
                "descripcion": r.descripcion,
                "precision_m": r.precision_m,
                "dispositivo_id": r.dispositivo_id,
                "capturado_en": r.capturado_en,
            }
            for r in reportes
        ]
        with self._sf.begin() as session:
            dialecto = session.get_bind().dialect.name
            insert = postgresql.insert if dialecto == "postgresql" else sqlite.insert
            stmt = (
                insert(ReporteORM)
                .values(filas)
                .on_conflict_do_nothing(index_elements=["id"])
                .returning(ReporteORM.id)
            )
            return set(session.execute(stmt).scalars().all())

    def listar(self, filtros: Filtros) -> list[Reporte]:
        stmt = select(ReporteORM)
        if filtros.evento:
            stmt = stmt.where(ReporteORM.evento == filtros.evento)
        if filtros.nivel_min:
            stmt = stmt.where(ReporteORM.nivel >= filtros.nivel_min)
        if filtros.desde:
            stmt = stmt.where(ReporteORM.capturado_en >= filtros.desde)
        if filtros.hasta:
            stmt = stmt.where(ReporteORM.capturado_en <= filtros.hasta)
        stmt = stmt.order_by(ReporteORM.capturado_en.desc()).limit(filtros.limite)
        with self._sf() as session:
            return [_a_dominio(f) for f in session.execute(stmt).scalars()]

    def eventos(self) -> list[tuple[str, int]]:
        stmt = (
            select(ReporteORM.evento, func.count())
            .where(ReporteORM.evento.is_not(None))
            .group_by(ReporteORM.evento)
            .order_by(func.count().desc())
        )
        with self._sf() as session:
            return [(e, n) for e, n in session.execute(stmt).all()]


def _a_dominio(f: ReporteORM) -> Reporte:
    capturado = f.capturado_en
    if capturado.tzinfo is None:  # SQLite no conserva la zona horaria
        capturado = capturado.replace(tzinfo=timezone.utc)
    return Reporte(
        id=f.id,
        latitud=f.latitud,
        longitud=f.longitud,
        nivel=f.nivel,
        capturado_en=capturado,
        evento=f.evento,
        descripcion=f.descripcion,
        precision_m=f.precision_m,
        dispositivo_id=f.dispositivo_id,
        recibido_en=f.recibido_en,
    )
