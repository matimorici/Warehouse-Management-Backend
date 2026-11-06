-- Estado habilitado/deshabilitado del usuario.
-- Los usuarios existentes quedan activos.
ALTER TABLE usuario
    ADD COLUMN activo BOOLEAN NOT NULL DEFAULT TRUE;