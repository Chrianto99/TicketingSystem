-- Incident reports gain a priority column, reusing Ticket's TicketPriority enum
-- (LOW/MEDIUM/HIGH — verified against the live enum, not a stale snapshot, after
-- V5's copy-paste mismatch on incident_report_status_check).
ALTER TABLE public.incident_report ADD COLUMN IF NOT EXISTS priority character varying(255);
ALTER TABLE public.incident_report DROP CONSTRAINT IF EXISTS incident_report_priority_check;
ALTER TABLE public.incident_report ADD CONSTRAINT incident_report_priority_check
    CHECK (((priority)::text = ANY ((ARRAY['LOW'::character varying, 'MEDIUM'::character varying, 'HIGH'::character varying])::text[])));
