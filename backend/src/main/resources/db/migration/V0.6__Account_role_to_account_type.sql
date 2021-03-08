BEGIN;
ALTER TABLE account_role RENAME TO account_type;
ALTER TABLE account_type RENAME COLUMN manage_accounts TO allowed_to_manage_accounts;
ALTER TABLE account RENAME COLUMN role_id TO type_id;
COMMIT;
