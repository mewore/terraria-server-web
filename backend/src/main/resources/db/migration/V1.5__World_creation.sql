ALTER TABLE terraria_world DROP CONSTRAINT terraria_world_name_host_id_ukey;
ALTER TABLE terraria_world RENAME COLUMN name TO file_name;
ALTER TABLE terraria_world ADD CONSTRAINT terraria_world_file_name_host_id_ukey UNIQUE (file_name, host_id);

ALTER TABLE terraria_world ADD COLUMN display_name varchar(255) NULL;
UPDATE terraria_world SET display_name = REPLACE(file_name, '_', ' ');
ALTER TABLE terraria_world ALTER COLUMN display_name SET NOT NULL;

ALTER TABLE terraria_world ADD COLUMN "size" varchar(255) NULL;
ALTER TABLE terraria_world ADD COLUMN difficulty varchar(255) NULL;
