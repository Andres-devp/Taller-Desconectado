"""Excepciones del dominio."""


class ErrorDominio(Exception):
    """Base de los errores de negocio."""


class ReporteInvalido(ErrorDominio):
    """El reporte no cumple las reglas del dominio."""


class LoteDemasiadoGrande(ErrorDominio):
    """El lote supera el máximo permitido por sincronización."""
