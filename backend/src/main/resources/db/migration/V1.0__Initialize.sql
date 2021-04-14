BEGIN;

-- [hibernate_sequence]

CREATE SEQUENCE IF NOT EXISTS hibernate_sequence
    INCREMENT BY 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    START 1
    CACHE 1
    NO CYCLE;

-- [account_type]

CREATE TABLE account_type (
    id int8 NOT NULL,
    able_to_manage_accounts bool NOT NULL,
    able_to_manage_hosts bool NOT NULL,
    able_to_manage_terraria bool NOT NULL,
    CONSTRAINT account_type_pkey PRIMARY KEY (id)
);

-- [account]

CREATE TABLE account (
    id int8 NOT NULL,
    "password" bytea NOT NULL,
    "session" bytea NOT NULL,
    session_expiration timestamp NOT NULL,
    username varchar(255) NOT NULL,
    type_id int8 NULL,
    CONSTRAINT account_pkey PRIMARY KEY (id),
    CONSTRAINT account_username_ukey UNIQUE (username)
);

ALTER TABLE account ADD CONSTRAINT account_type_fkey FOREIGN KEY (role_id) REFERENCES account_type(id);

-- [host]

CREATE TABLE host (
    id int8 NOT NULL,
    name varchar(255),
    alive bool NOT NULL,
    heartbeat_duration int8 NOT NULL,
    last_heartbeat timestamp NOT NULL,
    terraria_instance_directory varchar(255) NOT NULL,
    url varchar(255) NULL,
    uuid uuid NOT NULL,
    CONSTRAINT host_pkey PRIMARY KEY (id),
    CONSTRAINT host_uuid_ukey UNIQUE (uuid)
);

-- [terraria_world]

CREATE TABLE terraria_world (
	id int8 NOT NULL,
	"data" oid NULL,
	last_modified timestamp NULL,
	"name" varchar(255) NOT NULL,
	host_id int8 NOT NULL,
	CONSTRAINT terraria_world_pkey PRIMARY KEY (id),
	CONSTRAINT terraria_world_name_host_id_ukey UNIQUE (name, host_id)
);

ALTER TABLE terraria_world ADD CONSTRAINT terraria_world_host_fkey FOREIGN KEY (host_id) REFERENCES host(id);

-- [terraria_instance]

CREATE TABLE terraria_instance (
	id int8 NOT NULL,
	"location" varchar(1023) NOT NULL,
	mod_loader_release_url varchar(1023) NOT NULL,
	mod_loader_archive_url varchar(1023) NOT NULL,
	mod_loader_version varchar(255) NOT NULL,
	name varchar(255) NOT NULL,
	state varchar(255) NOT NULL,
	terraria_server_url varchar(1023) NOT NULL,
	terraria_version varchar(255) NOT NULL,
	uuid uuid NOT NULL,
	host_id int8 NOT NULL,
	CONSTRAINT terraria_instance_pkey PRIMARY KEY (id),
	CONSTRAINT terraria_instance_ukey UNIQUE (host_id, uuid)
);

ALTER TABLE terraria_instance ADD CONSTRAINT terraria_instance_host_fkey FOREIGN KEY (host_id) REFERENCES host(id);

COMMIT;
