-- Backs the "red dot on My Tickets" notification: flips true whenever a ticket
-- is assigned/reassigned to a user, flips back false once they view My Tickets.
-- IF NOT EXISTS guard follows V3's convention for environments previously
-- managed by Hibernate ddl-auto=update.
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS unseen_assigned_tickets boolean NOT NULL DEFAULT false;
