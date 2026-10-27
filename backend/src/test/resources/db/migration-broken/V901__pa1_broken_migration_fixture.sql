CREATE SCHEMA pa1_broken_marker;

CREATE TABLE pa1_broken_marker.first_valid_ddl (
    id integer NOT NULL,
    note text NOT NULL,
    CONSTRAINT pk_pa1_broken_marker PRIMARY KEY (id)
);

INSERT INTO pa1_broken_marker.first_valid_ddl (id, note) VALUES (1, 'executed-before-failure');

DO $$
BEGIN
    RAISE EXCEPTION 'PA1_S1_BROKEN_MIGRATION_SENTINEL_2f4c9a7e';
END;
$$;
