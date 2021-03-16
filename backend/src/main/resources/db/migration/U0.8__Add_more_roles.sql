BEGIN;
ALTER TABLE account_type RENAME COLUMN able_to_manage_accounts TO allowed_to_manage_accounts;
ALTER TABLE account_type DROP COLUMN able_to_manage_hosts;
ALTER TABLE account_type DROP COLUMN able_to_manage_terraria;
COMMIT;

--DELETE FROM flyway_schema_history WHERE script = 'V0.8__Add_more_roles.sql';
