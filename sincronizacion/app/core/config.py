"""Configuración del sistema central, leída de variables de entorno."""

from __future__ import annotations

from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    database_url: str = "postgresql+psycopg://desastres:desastres@localhost:5432/desastres"
    log_level: str = "INFO"
    # Orígenes permitidos para el visor web cuando no pasa por el proxy de nginx.
    cors_origins: str = "*"
    # Tamaño máximo de un lote de sincronización enviado por la app móvil.
    max_lote: int = 500
    service_name: str = "sincronizacion"

    @property
    def cors_origin_list(self) -> list[str]:
        return [o.strip() for o in self.cors_origins.split(",") if o.strip()]


@lru_cache
def get_settings() -> Settings:
    return Settings()
