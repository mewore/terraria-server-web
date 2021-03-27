BEGIN;
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
COMMIT;
