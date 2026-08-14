-- Ticket history rows now carry a free-text description: the diff for INFO_CHANGED,
-- the old/new text for COMMENT_EDITED, the reason for CANCELLED/REOPENED/REASSIGNED,
-- and (redundantly, for convenience) the comment text for COMMENT_ADDED.
-- IF NOT EXISTS/IF EXISTS guards because this database was previously managed by
-- Hibernate ddl-auto=update, which may already have created the column by the time
-- this migration runs on some environments.
ALTER TABLE public.ticket_history ADD COLUMN IF NOT EXISTS description character varying(2000);

ALTER TABLE public.ticket_history DROP CONSTRAINT IF EXISTS ticket_history_action_check;

ALTER TABLE public.ticket_history ADD CONSTRAINT ticket_history_action_check
    CHECK (((action)::text = ANY (ARRAY[
        'CREATED'::text, 'ASSIGNED'::text, 'REASSIGNED'::text, 'RESOLVED'::text,
        'CANCELLED'::text, 'REOPENED'::text, 'PRIORITY_CHANGED'::text, 'INFO_CHANGED'::text,
        'COMMENT_ADDED'::text, 'COMMENT_EDITED'::text, 'COMMENT_REMOVED'::text,
        'ATTACHMENT_ADDED'::text, 'ATTACHMENT_REMOVED'::text
    ])));

-- Backfill: existing CANCELLED/REOPENED/REASSIGNED rows still hold their reason on the
-- linked comment row only; copy it over so old history entries read the same as new ones.
UPDATE public.ticket_history h
SET description = c.text
FROM public.comment c
WHERE h.comment_id = c.id
  AND h.action IN ('CANCELLED', 'REOPENED', 'REASSIGNED')
  AND h.description IS NULL;

-- Backfill: existing COMMENT_ADDED rows likewise only have the text on the comment row.
UPDATE public.ticket_history h
SET description = c.text
FROM public.comment c
WHERE h.comment_id = c.id
  AND h.action = 'COMMENT_ADDED'
  AND h.description IS NULL;
