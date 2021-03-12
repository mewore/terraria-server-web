BEGIN;
ALTER TABLE account RENAME COLUMN type_id TO role_id;
ALTER TABLE account_type RENAME COLUMN allowed_to_manage_accounts TO manage_accounts;
ALTER TABLE account_type RENAME TO account_role;
COMMIT;

--DELETE FROM flyway_schema_history WHERE script = 'V0.6__Account_role_to_account_type.sql';
