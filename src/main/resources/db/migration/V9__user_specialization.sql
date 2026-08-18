-- Optional field, same nullability as email/phone_number — shown next to the
-- username in ticket-assignee dropdowns.
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS specialization varchar(255);
