# --- !Ups

CREATE TABLE users (
  username VARCHAR(255) NOT NULL PRIMARY KEY,
  displayName VARCHAR(255) NOT NULL,
  password VARCHAR(255) NOT NULL,
  email VARCHAR(255) UNIQUE NOT NULL
);

CREATE TABLE friendships (
  username1 VARCHAR(255) NOT NULL,
  username2 VARCHAR(255) NOT NULL,
  FOREIGN KEY (username1) REFERENCES users (username),
  FOREIGN KEY (username2) REFERENCES users (username),
  PRIMARY KEY (username1, username2)
);

# --- !Downs

DROP TABLE IF EXISTS friendships;

DROP TABLE IF EXISTS users;
