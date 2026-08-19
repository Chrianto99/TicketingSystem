-- Renames the IncidentReport feature's table/columns to match the Java-side
-- IncidentReport -> Incident rename. Constraint/sequence names are left as-is
-- (fk_*_incident_report, incident_report_id_seq, etc.) — Hibernate's
-- ddl-auto=validate only checks table/column names, and Postgres keeps FKs
-- valid across a RENAME (it tracks the target by OID, not by name).
ALTER TABLE public.incident_report RENAME TO incident;

ALTER TABLE public.attachment RENAME COLUMN incident_report_id TO incident_id;
ALTER TABLE public.comment RENAME COLUMN incident_report_id TO incident_id;
ALTER TABLE public.ticket RENAME COLUMN incident_report_id TO incident_id;
