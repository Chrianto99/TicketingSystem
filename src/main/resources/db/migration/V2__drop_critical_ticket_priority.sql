-- The CRITICAL priority level was removed from the app (collapsed into HIGH);
-- existing CRITICAL rows were already migrated to HIGH before this constraint
-- was tightened. This runs on every database, baselined or fresh.
ALTER TABLE public.ticket DROP CONSTRAINT ticket_priority_check;

ALTER TABLE public.ticket ADD CONSTRAINT ticket_priority_check
    CHECK (((priority)::text = ANY ((ARRAY['LOW'::character varying, 'MEDIUM'::character varying, 'HIGH'::character varying])::text[])));
