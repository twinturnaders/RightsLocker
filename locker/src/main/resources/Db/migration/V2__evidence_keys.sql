ALTER TABLE evidence ADD COLUMN IF NOT EXISTS thumbnail_key text;
ALTER TABLE evidence ADD COLUMN IF NOT EXISTS redacted_key  text;