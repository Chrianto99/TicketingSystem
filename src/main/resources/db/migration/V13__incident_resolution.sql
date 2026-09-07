-- Incidents gain a resolution field, mirroring Ticket.resolution — closing an
-- incident now requires explaining what was done, matching the ticket resolve flow.
ALTER TABLE public.incident ADD COLUMN IF NOT EXISTS resolution character varying(2000);
