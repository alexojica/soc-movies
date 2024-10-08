\c postgres;

DROP DATABASE IF EXISTS soc_movies_test_db;
CREATE DATABASE soc_movies_test_db;
GRANT ALL PRIVILEGES ON DATABASE soc_movies_test_db TO justin;

DROP DATABASE IF EXISTS soc_movies_db;
CREATE DATABASE soc_movies_db;
GRANT ALL PRIVILEGES ON DATABASE soc_movies_db TO justin;

\c soc_movies_db;


CREATE TABLE users
(
    id         serial PRIMARY KEY,
    username   TEXT UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);


CREATE TABLE movies
(
    id            serial PRIMARY KEY,
    name          TEXT UNIQUE,
    slug          TEXT UNIQUE,
    description   TEXT,
    avg_rating    NUMERIC(5, 2),
    tags          TEXT,
    released_year NUMERIC(4),
    lang          TEXT,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);


CREATE TABLE ratings
(
    id         serial PRIMARY KEY,
    user_id    integer REFERENCES users (id)  NOT NULL,
    movie_id   integer REFERENCES movies (id) NOT NULL,
    rating     NUMERIC(5, 2),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT one_review_per_user_movie UNIQUE (user_id, movie_id)
);


\c soc_movies_test_db;

CREATE TABLE users
(
    id         serial PRIMARY KEY,
    username   TEXT UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);


CREATE TABLE movies
(
    id            serial PRIMARY KEY,
    name          TEXT UNIQUE,
    slug          TEXT UNIQUE,
    description   TEXT,
    avg_rating    NUMERIC(5, 2),
    tags          TEXT,
    released_year NUMERIC(4),
    lang          TEXT,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);


CREATE TABLE ratings
(
    id         serial PRIMARY KEY,
    user_id    integer REFERENCES users (id)  NOT NULL,
    movie_id   integer REFERENCES movies (id) NOT NULL,
    rating     NUMERIC(5, 2),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT one_review_per_user_movie UNIQUE (user_id, movie_id)
);
