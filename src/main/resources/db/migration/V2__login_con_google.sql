-- Las cuentas creadas con Google no tienen contrasena: el hash pasa a ser opcional
-- y el usuario queda identificado por el "sub" que entrega Google.
ALTER TABLE usuarios ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE usuarios ADD COLUMN google_sub VARCHAR(64);
CREATE UNIQUE INDEX IF NOT EXISTS uq_usuarios_google_sub ON usuarios (google_sub);
