-- Splits ticket_history into a shared table for both Ticket and Incident timelines:
-- the single `action` column becomes `ticket_action` + `incident_action` (exactly
-- one set per row, enforced in the entity, not the DB), and the `assigned_to_id` /
-- comment_id` FK columns are dropped in favor of folding that information into the
-- free-text `description` at write time (TicketHistoryService.logHistory /
-- logIncidentHistory compose the full sentence there now).

-- Backfill description BEFORE dropping assigned_to_id, using the same sentence
-- format TicketHistoryService.logHistory() writes going forward, so old and new
-- entries render identically. Falls back to the same "deleted user" labels the
-- frontend already uses when performed_by_id/assigned_to_id were nulled out by
-- UserService.deleteUser.
UPDATE public.ticket_history
SET description =
    COALESCE((SELECT username FROM public.users WHERE id = performed_by_id), 'Διαγραμμένος χρήστης')
    || ' ανέθεσε το ticket σε '
    || COALESCE((SELECT username FROM public.users WHERE id = assigned_to_id), 'διαγραμμένο χρήστη')
    || '.'
WHERE action = 'ASSIGNED';

UPDATE public.ticket_history
SET description =
    COALESCE((SELECT username FROM public.users WHERE id = performed_by_id), 'Διαγραμμένος χρήστης')
    || ' επανέθεσε το ticket σε '
    || COALESCE((SELECT username FROM public.users WHERE id = assigned_to_id), 'διαγραμμένο χρήστη')
    || '.'
    || CASE WHEN description IS NOT NULL AND description <> '' THEN ' Λόγος: ' || description ELSE '' END
WHERE action = 'REASSIGNED';

ALTER TABLE public.ticket_history RENAME COLUMN action TO ticket_action;

ALTER TABLE public.ticket_history ADD COLUMN IF NOT EXISTS incident_action character varying(255);
ALTER TABLE public.ticket_history ADD COLUMN IF NOT EXISTS incident_id bigint;
ALTER TABLE public.ticket_history
    ADD CONSTRAINT fk_ticket_history_incident FOREIGN KEY (incident_id) REFERENCES public.incident(id);

ALTER TABLE public.ticket_history DROP COLUMN IF EXISTS assigned_to_id;
ALTER TABLE public.ticket_history DROP COLUMN IF EXISTS comment_id;

ALTER TABLE public.ticket_history DROP CONSTRAINT IF EXISTS ticket_history_incident_action_check;
ALTER TABLE public.ticket_history ADD CONSTRAINT ticket_history_incident_action_check
    CHECK ((incident_action IS NULL) OR ((incident_action)::text = ANY (ARRAY[
        'REPORTED'::text, 'CLOSED'::text, 'REOPENED'::text, 'INFO_CHANGED'::text,
        'COMMENT_ADDED'::text, 'COMMENT_EDITED'::text, 'COMMENT_REMOVED'::text,
        'ATTACHMENT_ADDED'::text, 'ATTACHMENT_REMOVED'::text,
        'TICKET_ASSIGNED'::text, 'TICKET_RESOLVED'::text, 'TICKET_CANCELLED'::text, 'TICKET_REOPENED'::text
    ])));
