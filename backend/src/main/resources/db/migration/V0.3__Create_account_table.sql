CREATE TABLE public.account (
	id int8 NOT NULL,
	"password" bytea NOT NULL,
	"session" bytea NOT NULL,
	session_expiration timestamp NOT NULL,
	username varchar(255) NOT NULL,
	role_id int8 NULL,
	CONSTRAINT account_pkey PRIMARY KEY (id),
	CONSTRAINT account_username_ukey UNIQUE (username)
);

-- public.account foreign keys
ALTER TABLE public.account ADD CONSTRAINT account_role_fkey FOREIGN KEY (role_id) REFERENCES account_role(id);
