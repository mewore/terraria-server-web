BEGIN;
ALTER TABLE account_type RENAME COLUMN allowed_to_manage_accounts TO able_to_manage_accounts;
ALTER TABLE account_type ADD COLUMN able_to_manage_hosts bool NOT NULL DEFAULT false;
ALTER TABLE account_type ADD COLUMN able_to_manage_terraria bool NOT NULL DEFAULT false;
COMMIT;
