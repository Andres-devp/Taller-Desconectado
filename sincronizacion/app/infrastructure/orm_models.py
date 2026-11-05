"""Modelo ORM. Debe coincidir con `database/init/01-schema.sql`."""

from __future__ import annotations

import uuid
from datetime import datetime

from sqlalchemy import CheckConstraint, DateTime, Float, Index, SmallInteger, String, Uuid, func
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column


class Base(DeclarativeBase):
    pass


class ReporteORM(Base):
    __tablename__ = "reportes"
    __table_args__ = (
        CheckConstraint("latitud BETWEEN -90 AND 90", name="ck_reportes_latitud"),
        CheckConstraint("longitud BETWEEN -180 AND 180", name="ck_reportes_longitud"),
        CheckConstraint("nivel BETWEEN 1 AND 5", name="ck_reportes_nivel"),
        Index("ix_reportes_evento", "evento"),
        Index("ix_reportes_capturado_en", "capturado_en"),
    )

    id: Mapped[uuid.UUID] = mapped_column(Uuid, primary_key=True)
    latitud: Mapped[float] = mapped_column(Float, nullable=False)
    longitud: Mapped[float] = mapped_column(Float, nullable=False)
    nivel: Mapped[int] = mapped_column(SmallInteger, nullable=False)
    evento: Mapped[str | None] = mapped_column(String(120))
    descripcion: Mapped[str | None] = mapped_column(String(500))
    precision_m: Mapped[float | None] = mapped_column(Float)
    dispositivo_id: Mapped[str | None] = mapped_column(String(64))
    capturado_en: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    recibido_en: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
