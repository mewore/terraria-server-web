ALTER TABLE terraria_world DROP CONSTRAINT terraria_world_file_fkey;
ALTER TABLE terraria_world_file RENAME COLUMN id TO world_id;
ALTER TABLE terraria_world_file
    ADD CONSTRAINT terraria_world_file_world_fkey FOREIGN KEY (world_id) REFERENCES terraria_world(id);
