CREATE TABLE terraria_instance (
	id int8 NOT NULL,
	"location" varchar(1023) NOT NULL,
	mod_loader_url varchar(1023) NOT NULL,
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
