-- V2 — character sex + appearance (design-draft-v2.md §5, Slice 2a).
--
-- Adds the required, cosmetic `sex` (binary MALE/FEMALE) and the optional
-- `appearance` tints JSON to both the persisted sheet (`characters`) and the
-- draft table (`character_drafts`). Columns are nullable so existing rows
-- survive; the app validates that new characters carry a sex on compile.
-- Forward-only: never edit V1 (design-draft-v2.md §3).

alter table if exists characters add column sex varchar(255);
alter table if exists characters add column appearance oid;
alter table if exists character_drafts add column sex varchar(255);
alter table if exists character_drafts add column appearance oid;

alter table if exists characters add constraint chk_characters_sex check (sex in ('MALE','FEMALE'));
alter table if exists character_drafts add constraint chk_character_drafts_sex check (sex in ('MALE','FEMALE'));
