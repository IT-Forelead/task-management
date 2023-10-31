CREATE TABLE IF NOT EXISTS roles
(
    name    VARCHAR NOT NULL PRIMARY KEY
);

INSERT INTO roles
VALUES ('super_manager'),
       ('tech_admin'),
       ('admin'),
       ('trainer');

CREATE TABLE IF NOT EXISTS delivery_statuses
(
    name    VARCHAR NOT NULL PRIMARY KEY
);

INSERT INTO delivery_statuses
VALUES ('sent'),
       ('delivered'),
       ('failed'),
       ('undefined');

CREATE TABLE IF NOT EXISTS users
(
    id          UUID PRIMARY KEY,
    created_at  TIMESTAMP NOT NULL,
    firstname   VARCHAR NOT NULL,
    lastname    VARCHAR NOT NULL,
    phone       VARCHAR NOT NULL UNIQUE,
    role        VARCHAR NOT NULL
        CONSTRAINT fk_role REFERENCES roles (name) ON UPDATE CASCADE ON DELETE CASCADE NOT NULL,
    password    VARCHAR NOT NULL
);

INSERT INTO "users" ("id", "created_at", "firstname", "lastname", "phone", "role", "password")
VALUES ('72a911c8-ad24-4e2d-8930-9c3ba51741df', '2022-11-07T06:43:01.089Z', 'Admin', 'Super Manager', '+998901234567',
        'super_manager', '$s0$e0801$5JK3Ogs35C2h5htbXQoeEQ==$N7HgNieSnOajn1FuEB7l4PhC6puBSq+e1E8WUaSJcGY=');