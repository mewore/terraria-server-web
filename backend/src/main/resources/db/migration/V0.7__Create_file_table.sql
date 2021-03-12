CREATE TABLE file (
    id int8 NOT NULL,
    "data" oid NOT NULL,
    os varchar(255) NOT NULL,
    source_url varchar(255) NOT NULL,
    "type" varchar(255) NOT NULL,
    "version" varchar(255) NOT NULL,
    CONSTRAINT file_pkey PRIMARY KEY (id),
    CONSTRAINT file_source_url_ukey UNIQUE (source_url)
);
