"""Pruebas del endpoint de sincronización por lotes."""

from __future__ import annotations

from datetime import datetime, timedelta, timezone


def _lote(client, reportes):
    return client.post("/api/v1/reportes/lote", json={"reportes": reportes})


def test_lote_valido_se_persiste(client, nuevo_reporte):
    reportes = [nuevo_reporte(nivel=n) for n in range(1, 6)]
    r = _lote(client, reportes)
    assert r.status_code == 200
    body = r.json()
    assert body["nuevos"] == 5
    assert body["duplicados"] == 0
    assert sorted(body["aceptados"]) == sorted(x["id"] for x in reportes)
    assert len(client.get("/api/v1/reportes").json()) == 5


def test_reenviar_mismo_lote_es_idempotente(client, nuevo_reporte):
    reportes = [nuevo_reporte(), nuevo_reporte()]
    assert _lote(client, reportes).json()["nuevos"] == 2

    segundo = _lote(client, reportes).json()
    # El dispositivo pudo perder la respuesta: se acepta otra vez, sin duplicar.
    assert segundo["nuevos"] == 0
    assert segundo["duplicados"] == 2
    assert sorted(segundo["aceptados"]) == sorted(x["id"] for x in reportes)
    assert len(client.get("/api/v1/reportes").json()) == 2


def test_ids_repetidos_dentro_del_lote(client, nuevo_reporte):
    rep = nuevo_reporte()
    body = _lote(client, [rep, dict(rep)]).json()
    assert body["nuevos"] == 1
    assert body["aceptados"] == [rep["id"]]


def test_lote_mixto_rechaza_solo_los_invalidos(client, nuevo_reporte):
    bueno = nuevo_reporte()
    nivel_malo = nuevo_reporte(nivel=6)
    nivel_cero = nuevo_reporte(nivel=0)
    lat_mala = nuevo_reporte(latitud=95)
    sin_id = nuevo_reporte()
    del sin_id["id"]
    futuro = nuevo_reporte(capturado_en=(datetime.now(timezone.utc) + timedelta(days=3)).isoformat())

    body = _lote(client, [bueno, nivel_malo, nivel_cero, lat_mala, sin_id, futuro]).json()
    assert body["aceptados"] == [bueno["id"]]
    rechazados = {r["id"]: r["motivo"] for r in body["rechazados"]}
    assert len(body["rechazados"]) == 5
    assert "nivel" in rechazados[nivel_malo["id"]]
    assert "nivel" in rechazados[nivel_cero["id"]]
    assert "latitud" in rechazados[lat_mala["id"]]
    assert "futuro" in rechazados[futuro["id"]]
    assert None in rechazados


def test_lote_demasiado_grande(client, nuevo_reporte):
    r = _lote(client, [nuevo_reporte() for _ in range(51)])
    assert r.status_code == 413


def test_lote_vacio(client):
    body = _lote(client, []).json()
    assert body == {"aceptados": [], "nuevos": 0, "duplicados": 0, "rechazados": []}


def test_health(client):
    assert client.get("/health").json() == {"status": "ok"}
    assert client.get("/health/ready").json()["database"] == "up"
