-- TerrariaInstanceEventEntity
DROP TABLE IF EXISTS terraria_instance_event;

-- TerrariaWorldEntity
UPDATE terraria_instance SET world_id = NULL;
DELETE FROM terraria_world;
ALTER TABLE terraria_world DROP COLUMN IF EXISTS mods;
ALTER TABLE terraria_world DROP CONSTRAINT IF EXISTS terraria_world_data_fkey;
ALTER TABLE terraria_world DROP COLUMN IF EXISTS data_id;
ALTER TABLE terraria_world ADD COLUMN content oid NOT NULL;

-- FileDataEntity
DROP TABLE IF EXISTS file_data;

-- TerrariaInstanceEntity
ALTER TABLE terraria_instance DROP CONSTRAINT IF EXISTS terraria_instance_world_fkey;
ALTER TABLE terraria_instance DROP COLUMN IF EXISTS world_id;
ALTER TABLE terraria_instance DROP COLUMN IF EXISTS loaded_mods;
ALTER TABLE terraria_instance DROP COLUMN IF EXISTS mods_to_enable;
ALTER TABLE terraria_instance DROP COLUMN IF EXISTS options;
ALTER TABLE terraria_instance DROP COLUMN IF EXISTS pending_options;
ALTER TABLE terraria_instance DROP COLUMN IF EXISTS password;
ALTER TABLE terraria_instance DROP COLUMN IF EXISTS automatically_forward_port;
ALTER TABLE terraria_instance DROP COLUMN IF EXISTS port;
ALTER TABLE terraria_instance DROP COLUMN IF EXISTS max_players;
ALTER TABLE terraria_instance DROP COLUMN IF EXISTS next_output_byte_position;
ALTER TABLE terraria_instance DROP COLUMN IF EXISTS current_action;
UPDATE terraria_instance SET state = 'READY' WHERE state = 'IDLE';

--DELETE FROM flyway_schema_history WHERE script = 'V1.1__Make_instances_bootable.sql';
