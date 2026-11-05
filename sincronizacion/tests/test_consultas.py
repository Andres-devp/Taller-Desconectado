"""Pruebas de las consultas del visor y de la agregación por zonas."""

from __future__ import annotations

import uuid
from datetime import datetime, timezone

import pytest

from app.domain.entities import Reporte
from app.domain.exceptions import ReporteInvalido
from app.domain.zonas import agrupar_por_zona


def _cargar(client, reportes):
    assert client.post("/api/v1/reportes/lote", json={"reportes": reportes}).status_code == 200


def test_heatmap_pondera_por_nivel(client, nuevo_reporte):
    _cargar(client, [nuevo_reporte(nivel=5, latitud=1.0, longitud=2.0),
                     nuevo_reporte(nivel=1, latitud=3.0, longitud=4.0)])
    puntos = sorted(client.get("/api/v1/heatmap").json())
    assert puntos == [[1.0, 2.0, 1.0], [3.0, 4.0, 0.2]]


def test_filtros_evento_y_nivel(client, nuevo_reporte):
    _cargar(client, [nuevo_reporte(evento="A", nivel=2), nuevo_reporte(evento="A", nivel=5),
                     nuevo_reporte(evento="B", nivel=4)])
    assert len(client.get("/api/v1/reportes", params={"evento": "A"}).json()) == 2
    assert len(client.get("/api/v1/heatmap", params={"nivel_min": 4}).json()) == 2
    eventos = client.get("/api/v1/eventos").json()
    assert eventos[0] == {"evento": "A", "reportes": 2}


def test_zonas_ordena_por_afectacion(client, nuevo_reporte):
    # Zona norte: 3 reportes graves. Zona sur: 1 reporte leve.
    _cargar(client, [nuevo_reporte(latitud=10.01, longitud=-75.01, nivel=5),
                     nuevo_reporte(latitud=10.02, longitud=-75.02, nivel=4),
                     nuevo_reporte(latitud=10.03, longitud=-75.03, nivel=5),
                     nuevo_reporte(latitud=2.0, longitud=-76.0, nivel=1)])
    zonas = client.get("/api/v1/zonas", params={"celda": 0.1}).json()
    assert len(zonas) == 2
    assert zonas[0]["reportes"] == 3
    assert zonas[0]["afectacion"] == 14
    assert zonas[0]["nivel_maximo"] == 5
    assert zonas[1]["nivel_promedio"] == 1.0


def _rep(lat, lon, nivel=3):
    return Reporte(id=uuid.uuid4(), latitud=lat, longitud=lon, nivel=nivel,
                   capturado_en=datetime.now(timezone.utc))


def test_agrupar_celdas_con_coordenadas_negativas():
    zonas = agrupar_por_zona([_rep(-0.01, -0.01), _rep(0.01, 0.01)], celda=0.1)
    assert len(zonas) == 2  # floor separa correctamente a ambos lados del 0


def test_agrupar_celda_invalida():
    with pytest.raises(ValueError):
        agrupar_por_zona([], celda=0)


@pytest.mark.parametrize("nivel", [0, 6, -1])
def test_dominio_rechaza_nivel_fuera_de_rango(nivel):
    with pytest.raises(ReporteInvalido):
        _rep(0, 0, nivel)
