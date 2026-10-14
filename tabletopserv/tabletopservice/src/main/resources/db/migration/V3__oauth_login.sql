-- V3 — social OAuth login (R19, design-draft-v2.md §11).
--
-- Links a users row to a social provider by its stable (provider, subject) pair. Both columns
-- are nullable so existing password accounts survive untouched; the unique constraint lets a
-- provider identity back at most one account (Postgres/H2 both treat NULLs as distinct, so
-- password-only rows are unaffected). Date of birth becomes nullable because providers do not
-- return it — the age gate still applies to password registration, which requires it.
-- Forward-only: never edit V1/V2 (design-draft-v2.md §3).

alter table if exists users add column oauth_provider varchar(32);
alter table if exists users add column oauth_subject varchar(255);
alter table if exists users alter column date_of_birth drop not null;

alter table if exists users add constraint uq_users_oauth_identity unique (oauth_provider, oauth_subject);
