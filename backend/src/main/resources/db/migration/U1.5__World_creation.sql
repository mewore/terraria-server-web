ALTER TABLE terraria_world DROP COLUMN difficulty;
ALTER TABLE terraria_world DROP COLUMN "size";

ALTER TABLE terraria_world DROP COLUMN display_name;

ALTER TABLE terraria_world DROP CONSTRAINT terraria_world_file_name_host_id_ukey;
ALTER TABLE terraria_world RENAME COLUMN file_name TO name;
ALTER TABLE terraria_world ADD CONSTRAINT terraria_world_name_host_id_ukey UNIQUE (name, host_id);

DELETE FROM flyway_schema_history WHERE script = 'V1.5__World_creation.sql';
