BEGIN;
ALTER TABLE account_type RENAME COLUMN allowed_to_manage_accounts TO manage_users;
ALTER TABLE account_type RENAME TO account_role;
COMMIT;
