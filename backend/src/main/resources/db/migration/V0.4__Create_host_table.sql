CREATE TABLE host (
    id int8 NOT NULL,
    alive bool NOT NULL,
    heartbeat_duration int8 NOT NULL,
    last_heartbeat timestamp NOT NULL,
    terraria_instance_directory varchar(255) NOT NULL,
    url varchar(255) NULL,
    uuid uuid NOT NULL,
    CONSTRAINT host_pkey PRIMARY KEY (id),
    CONSTRAINT host_uuid_ukey UNIQUE (uuid)
);
