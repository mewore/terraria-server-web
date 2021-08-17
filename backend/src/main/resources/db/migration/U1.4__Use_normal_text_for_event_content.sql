UPDATE terraria_instance_event SET "content" = lo_from_bytea(0, "content" :: text :: bytea) :: varchar;
ALTER TABLE terraria_instance_event ALTER COLUMN "content" TYPE text USING "content"::text;
ALTER TABLE terraria_instance_event RENAME COLUMN "content" TO "text";

--DELETE FROM flyway_schema_history WHERE script = 'V1.4__Use_normal_text_for_event_content.sql';
