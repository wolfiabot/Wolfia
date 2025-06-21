ALTER TABLE oauth2
	ADD COLUMN created_at timestamptz NOT NULL DEFAULT now();
