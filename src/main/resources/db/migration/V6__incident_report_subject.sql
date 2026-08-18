-- Incident reports gain a subject/title field alongside the existing description.
ALTER TABLE public.incident_report ADD COLUMN IF NOT EXISTS subject character varying(255);
