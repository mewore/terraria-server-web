CREATE TABLE public.account_role (
	id int8 NOT NULL,
	manage_accounts bool NOT NULL,
	CONSTRAINT account_role_pkey PRIMARY KEY (id)
);
