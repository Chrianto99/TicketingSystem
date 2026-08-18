-- Widen ticket_source_check (from V5) to allow the new CALLBACK source.
ALTER TABLE public.ticket DROP CONSTRAINT IF EXISTS ticket_source_check;
ALTER TABLE public.ticket ADD CONSTRAINT ticket_source_check
    CHECK (((source)::text = ANY ((ARRAY['MANUAL'::character varying, 'INCIDENT'::character varying, 'CALLBACK'::character varying])::text[])));
