ALTER TABLE terraria_world DROP CONSTRAINT terraria_world_file_fkey;

ALTER TABLE terraria_world_file RENAME TO file_data;
ALTER TABLE terraria_world ADD COLUMN data_id int8;

UPDATE terraria_world SET data_id = terraria_world.id;
ALTER TABLE terraria_world ALTER COLUMN data_id SET NOT NULL;

ALTER TABLE terraria_world
    ADD CONSTRAINT terraria_world_data_fkey FOREIGN KEY (data_id) REFERENCES file_data(id);

--DELETE FROM flyway_schema_history WHERE script = 'V1.2__Use_real_one_to_one_association_for_world_files.sql';
