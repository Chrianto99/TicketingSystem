-- Superseded by the report-oriented close/reopen flow: closing/reopening an
-- incident now posts a Comment (visible in the Αναφορές thread) instead of
-- writing to a single resolution column that got overwritten on every
-- subsequent close, losing earlier cycles' text.
ALTER TABLE public.incident DROP COLUMN IF EXISTS resolution;
