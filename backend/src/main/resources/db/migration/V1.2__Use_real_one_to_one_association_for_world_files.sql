DELETE FROM file_data WHERE NOT EXISTS (SELECT 1 FROM terraria_world WHERE terraria_world.data_id = file_data.id);
ALTER TABLE terraria_world DROP CONSTRAINT terraria_world_data_fkey;

UPDATE file_data SET id = terraria_world.id FROM terraria_world WHERE terraria_world.data_id = file_data.id;
ALTER TABLE terraria_world DROP COLUMN data_id;
ALTER TABLE file_data RENAME TO terraria_world_file;

-- It's safe to assume that all worlds have matching file data
ALTER TABLE terraria_world
    ADD CONSTRAINT terraria_world_file_fkey FOREIGN KEY (id) REFERENCES terraria_world_file(id);
