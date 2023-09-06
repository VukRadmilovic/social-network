# --- !Ups

CREATE TABLE users (
  username VARCHAR(255) NOT NULL PRIMARY KEY,
  displayName VARCHAR(255) NOT NULL,
  password VARCHAR(255) NOT NULL,
  email VARCHAR(255) UNIQUE NOT NULL
);

INSERT INTO users (username, displayName, password, email) VALUES
  ('jova', 'Jova', '10000:nPgQghJANf+lmHoINwqgEvl/7whHWlCZHht9xUdVns8=:PdRPv/yXYtn/C0N0xJoikQ==', 'jova@mail.com'),
  ('nika', 'Nika', '10000:nPgQghJANf+lmHoINwqgEvl/7whHWlCZHht9xUdVns8=:PdRPv/yXYtn/C0N0xJoikQ==', 'nika@mail.com');

CREATE TABLE friendships (
  username1 VARCHAR(255) NOT NULL,
  username2 VARCHAR(255) NOT NULL,
  FOREIGN KEY (username1) REFERENCES users (username),
  FOREIGN KEY (username2) REFERENCES users (username)
);

# --- !Downs

DROP TABLE IF EXISTS friendships;

DROP TABLE IF EXISTS users;
