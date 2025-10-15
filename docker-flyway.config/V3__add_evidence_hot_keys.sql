ALTER TABLE Evidence ADD COLUMN IF NOT EXISTS thumbnail_key text;
ALTER TABLE Evidence ADD COLUMN IF NOT EXISTS redacted_key  text;