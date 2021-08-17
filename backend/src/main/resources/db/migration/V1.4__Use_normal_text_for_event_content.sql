ALTER TABLE terraria_instance_event RENAME COLUMN "text" TO "content";
ALTER TABLE terraria_instance_event ALTER COLUMN "content" TYPE varchar USING "content"::varchar;
UPDATE terraria_instance_event SET "content" = convert_from(loread(lo_open("content" :: oid, 262144), 1048576), 'UTF8');
