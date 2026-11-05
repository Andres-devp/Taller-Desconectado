-- Esquema del sistema central. Debe coincidir con app/infrastructure/orm_models.py

CREATE TABLE IF NOT EXISTS reportes (
    id              UUID PRIMARY KEY,                -- generado en el dispositivo (idempotencia)
    latitud         DOUBLE PRECISION NOT NULL,
    longitud        DOUBLE PRECISION NOT NULL,
    nivel           SMALLINT         NOT NULL,
    evento          VARCHAR(120),
    descripcion     VARCHAR(500),
    precision_m     DOUBLE PRECISION,
    dispositivo_id  VARCHAR(64),
    capturado_en    TIMESTAMPTZ      NOT NULL,       -- momento de la captura en campo
    recibido_en     TIMESTAMPTZ      NOT NULL DEFAULT now(),  -- momento de la sincronización
    CONSTRAINT ck_reportes_latitud  CHECK (latitud  BETWEEN -90  AND 90),
    CONSTRAINT ck_reportes_longitud CHECK (longitud BETWEEN -180 AND 180),
    CONSTRAINT ck_reportes_nivel    CHECK (nivel    BETWEEN 1    AND 5)
);

CREATE INDEX IF NOT EXISTS ix_reportes_evento       ON reportes (evento);
CREATE INDEX IF NOT EXISTS ix_reportes_capturado_en ON reportes (capturado_en);
