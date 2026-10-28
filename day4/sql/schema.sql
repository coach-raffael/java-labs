-- shelfy 2.2: the three tables (Module 14.2). Run in psql as the user shelfy:
--   psql -h localhost -U shelfy -d shelfy -f day4/sql/schema.sql
-- The application runs the same statements at start (PostgresStorage.init), so a missing table
-- is never an error; this file is for reading, for psql and for your own experiments.

CREATE TABLE IF NOT EXISTS media (
    id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    kind text NOT NULL CHECK (kind IN ('BOOK', 'AUDIOBOOK')),
    title text NOT NULL CHECK (length(title) BETWEEN 1 AND 200),
    author text NOT NULL,
    isbn text NOT NULL UNIQUE,
    duration_minutes integer NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS members (
    id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name text NOT NULL
);

CREATE TABLE IF NOT EXISTS loans (
    id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    medium_id integer NOT NULL REFERENCES media(id),
    member_id integer NOT NULL REFERENCES members(id),
    borrowed_on date NOT NULL,
    due date NOT NULL,
    returned_on date
);

-- A medium is available when it has no loan with returned_on IS NULL. Try it:
--   INSERT INTO media (kind, title, author, isbn) VALUES ('BOOK', 'Clean Code', 'Robert C. Martin', '9780132350884');
--   INSERT INTO members (name) VALUES ('Ada Lovelace');
--   SELECT m.id, m.title, (SELECT count(*) FROM loans l WHERE l.medium_id = m.id AND l.returned_on IS NULL) = 0 AS available FROM media m;
