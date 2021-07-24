-- TerrariaInstanceEntity
UPDATE terraria_instance SET state = 'IDLE' WHERE state = 'READY';
ALTER TABLE terraria_instance ADD COLUMN current_action varchar(255) NULL;
ALTER TABLE terraria_instance ADD COLUMN next_output_byte_position int8 DEFAULT 0 NOT NULL;
ALTER TABLE terraria_instance ADD COLUMN max_players int4 DEFAULT 8 NOT NULL;
ALTER TABLE terraria_instance ADD COLUMN port int4 DEFAULT 7777 NOT NULL;
ALTER TABLE terraria_instance ADD COLUMN automatically_forward_port boolean DEFAULT false NOT NULL;
ALTER TABLE terraria_instance ADD COLUMN password varchar(255) DEFAULT '' NOT NULL;
ALTER TABLE terraria_instance ADD COLUMN pending_options varchar(255) DEFAULT '{}' NOT NULL;
ALTER TABLE terraria_instance ADD COLUMN options varchar(255) DEFAULT '{}' NOT NULL;
ALTER TABLE terraria_instance ADD COLUMN mods_to_enable varchar(255) DEFAULT '[]' NOT NULL;
ALTER TABLE terraria_instance ADD COLUMN loaded_mods varchar(255) DEFAULT '[]' NOT NULL;
ALTER TABLE terraria_instance ADD COLUMN world_id int8 NULL;
ALTER TABLE terraria_instance
    ADD CONSTRAINT terraria_instance_world_fkey FOREIGN KEY (world_id) REFERENCES terraria_world(id);

-- FileDataEntity
CREATE TABLE file_data (
    id int8 NOT NULL,
    "name" varchar(255) NOT NULL,
    content oid NULL,
    CONSTRAINT file_data_pkey PRIMARY KEY (id)
);

-- TerrariaWorldEntity
DELETE FROM terraria_world;
ALTER TABLE terraria_world DROP COLUMN IF EXISTS "data";
ALTER TABLE terraria_world ADD COLUMN data_id int8 NULL;
ALTER TABLE terraria_world ADD CONSTRAINT terraria_world_data_fkey FOREIGN KEY (data_id) REFERENCES file_data(id);
ALTER TABLE terraria_world ADD COLUMN mods varchar(255);

-- TerrariaInstanceEventEntity
CREATE TABLE terraria_instance_event (
    id int8 NOT NULL,
    type varchar(255) NOT NULL,
    "timestamp" timestamp NOT NULL,
    "text" text NOT NULL,
    instance_id int8 NOT NULL,
    CONSTRAINT terraria_instance_event_pkey PRIMARY KEY (id),
    CONSTRAINT terraria_instance_event_instance_fkey FOREIGN KEY (instance_id) REFERENCES terraria_instance(id)
);
