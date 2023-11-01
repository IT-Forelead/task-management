CREATE TYPE ROLE AS ENUM ('admin', 'creator');
CREATE TYPE DELIVERY_STATUS AS ENUM ('sent', 'delivered', 'not_delivered', 'failed', 'transmitted', 'undefined');
CREATE TYPE STATUS AS ENUM ('new', 'in_progress', 'complete', 'on_hold');

CREATE TABLE IF NOT EXISTS users
(
    id          UUID PRIMARY KEY,
    created_at  TIMESTAMP NOT NULL,
    firstname   VARCHAR NOT NULL,
    lastname    VARCHAR NOT NULL,
    phone       VARCHAR NOT NULL UNIQUE,
    role        ROLE NOT NULL,
    password    VARCHAR NOT NULL
);

INSERT INTO "users" ("id", "created_at", "firstname", "lastname", "phone", "role", "password")
VALUES ('72a911c8-ad24-4e2d-8930-9c3ba51741df', '2022-11-07T06:43:01.089Z', 'Admin', 'Super Manager', '+998901234567',
        'admin', '$s0$e0801$5JK3Ogs35C2h5htbXQoeEQ==$N7HgNieSnOajn1FuEB7l4PhC6puBSq+e1E8WUaSJcGY=');

CREATE TABLE IF NOT EXISTS tasks (
  id UUID PRIMARY KEY,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  title VARCHAR NOT NULL,
  filename VARCHAR NOT NULL,
  due_date TIMESTAMP WITH TIME ZONE NOT NULL,
  user_id UUID NULL CONSTRAINT fk_users REFERENCES users (id) ON UPDATE CASCADE ON DELETE CASCADE,
  status STATUS NOT NULL,
  description VARCHAR NOT NULL
);

CREATE TABLE IF NOT EXISTS task_comments (
  task_id UUID NOT NULL CONSTRAINT fk_tasks REFERENCES tasks (id) ON UPDATE CASCADE ON DELETE CASCADE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  note VARCHAR NOT NULL,
  user_id UUID NULL CONSTRAINT fk_users REFERENCES users (id) ON UPDATE CASCADE ON DELETE CASCADE
);
