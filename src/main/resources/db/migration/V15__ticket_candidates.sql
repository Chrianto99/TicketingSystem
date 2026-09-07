-- Multi-candidate ticket assignment: a ticket can be offered to several users at
-- once (assigned_user_id left null while offered), and the first one to claim it
-- becomes its sole assignee. TicketService enforces "assigned XOR has candidates";
-- this table only holds the currently-open offers.
CREATE TABLE public.ticket_candidate (
    ticket_id bigint NOT NULL REFERENCES public.ticket(id) ON DELETE CASCADE,
    user_id bigint NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    PRIMARY KEY (ticket_id, user_id)
);

-- Widen ticket_action to add OFFERED (a ticket was put up for grabs) and CLAIMED
-- (a candidate accepted it) — same pattern as V12's split, just appending values.
ALTER TABLE public.ticket_history DROP CONSTRAINT IF EXISTS ticket_history_action_check;
ALTER TABLE public.ticket_history ADD CONSTRAINT ticket_history_action_check
    CHECK (((ticket_action)::text = ANY (ARRAY[
        'CREATED'::text, 'ASSIGNED'::text, 'REASSIGNED'::text, 'RESOLVED'::text,
        'CANCELLED'::text, 'REOPENED'::text, 'PRIORITY_CHANGED'::text, 'INFO_CHANGED'::text,
        'COMMENT_ADDED'::text, 'COMMENT_EDITED'::text, 'COMMENT_REMOVED'::text,
        'ATTACHMENT_ADDED'::text, 'ATTACHMENT_REMOVED'::text,
        'OFFERED'::text, 'CLAIMED'::text
    ])));
