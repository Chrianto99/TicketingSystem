-- V5's incident_report_status_check was copied verbatim from V1's stale
-- snapshot text (OPEN/RESOLVED/CANCELLED, matching a TicketStatus-shaped
-- status), but the actual IncidentStatus enum is OPEN/CLOSED — so closing an
-- incident violated the CHECK constraint. Correcting it to match the enum.
ALTER TABLE public.incident_report DROP CONSTRAINT IF EXISTS incident_report_status_check;
ALTER TABLE public.incident_report ADD CONSTRAINT incident_report_status_check
    CHECK (((status)::text = ANY ((ARRAY['OPEN'::character varying, 'CLOSED'::character varying])::text[])));
