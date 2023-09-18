# --- !Ups

CREATE TABLE users
(
    username            VARCHAR(255)        NOT NULL PRIMARY KEY,
    displayName         VARCHAR(255)        NOT NULL,
    password            VARCHAR(255)        NOT NULL,
    email               VARCHAR(255) UNIQUE NOT NULL,
    hasProfilePicture   BOOLEAN             NOT NULL
);

CREATE TABLE friendships
(
    username1 VARCHAR(255) NOT NULL,
    username2 VARCHAR(255) NOT NULL,
    CONSTRAINT user1_fk FOREIGN KEY (username1) REFERENCES users (username),
    CONSTRAINT user2_fk FOREIGN KEY (username2) REFERENCES users (username),
    PRIMARY KEY (username1, username2)
);

CREATE TABLE friend_requests
(
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender   VARCHAR(255) NOT NULL,
    receiver VARCHAR(255) NOT NULL,
    status   VARCHAR(255) NOT NULL,
    created  TIMESTAMP    NOT NULL,
    updated  TIMESTAMP
);

CREATE TABLE posts (
    id       BIGINT AUTO_INCREMENT  PRIMARY KEY,
    poster   VARCHAR(255)           NOT NULL,
    content  TEXT                   NOT NULL,
    posted   TIMESTAMP              NOT NULL,
    likes    BIGINT                 DEFAULT 0,
    CONSTRAINT poster_fk FOREIGN KEY (poster) REFERENCES users (username)
);

CREATE TABLE likes (
    username VARCHAR(255)           NOT NULL,
    post BIGINT                     NOT NULL,
    PRIMARY KEY (username, post),
    CONSTRAINT user_fk              FOREIGN KEY (username) REFERENCES users (username),
    CONSTRAINT post_fk              FOREIGN KEY (post) REFERENCES posts (id) ON DELETE CASCADE
);

# --- !Downs

DROP TABLE IF EXISTS likes;

DROP TABLE IF EXISTS posts;

DROP TABLE IF EXISTS friendships;

DROP TABLE IF EXISTS friend_requests;

DROP TABLE IF EXISTS users;