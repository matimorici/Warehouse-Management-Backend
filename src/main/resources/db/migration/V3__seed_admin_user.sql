-- ============================================================================
-- WMS — Migración V3: usuario administrador por defecto (seed)
-- Flyway — PostgreSQL
-- ============================================================================
-- Inserta un usuario administrador inicial para que cualquier clon del
-- proyecto tenga un login funcional sin insertarlo a mano por la API.
--
-- Credenciales por defecto (documentadas en README.md):
--   CUIL:     20-00000000-1
--   Password: Admin1234
--   Rol:      ADMINISTRADOR
-- La contraseña está hasheada con BCrypt (BCryptPasswordEncoder, strength 10);
-- el hash corresponde a "Admin1234".
--
-- El guard WHERE NOT EXISTS evita duplicados si el CUIL ya existe (la tabla
-- usuario no tiene UNIQUE sobre cuil a nivel de BD). Como Flyway aplica cada
-- migración una sola vez, en la práctica corre una única vez.
-- ============================================================================

INSERT INTO usuario (nombre, apellido, cuil, rol, contrasena)
SELECT 'Administrador', 'Sistema', '20-00000000-1', 'ADMINISTRADOR',
       '$2a$10$c0AJ/n5exC8DOpGCN3kdse.NfC08cFrUL0CmVw0MKSPhvAKcGWI.C'
WHERE NOT EXISTS (
    SELECT 1 FROM usuario WHERE cuil = '20-00000000-1'
);

-- ============================================================================
-- Fin de la migración V3
-- ============================================================================
