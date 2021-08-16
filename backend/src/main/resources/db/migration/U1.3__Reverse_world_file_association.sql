ALTER TABLE terraria_world_file DROP CONSTRAINT terraria_world_file_world_fkey;
ALTER TABLE terraria_world_file RENAME COLUMN world_id TO id;
ALTER TABLE terraria_world
    ADD CONSTRAINT terraria_world_file_fkey FOREIGN KEY (id) REFERENCES terraria_world_file(id);

--DELETE FROM flyway_schema_history WHERE script = 'V1.3__Reverse_world_file_association.sql';
