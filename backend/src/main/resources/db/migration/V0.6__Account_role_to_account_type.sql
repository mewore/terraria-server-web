BEGIN;
ALTER TABLE account_role RENAME TO account_type;
ALTER TABLE account_type RENAME COLUMN manage_users TO allowed_to_manage_accounts;
COMMIT;
